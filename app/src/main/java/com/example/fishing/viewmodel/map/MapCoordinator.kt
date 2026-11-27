package com.example.fishing.viewmodel.map

import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.MapBounds
import com.example.fishing.model.MarkerDomain
import com.example.fishing.viewmodel.filter.ReportFilters
import com.example.fishing.viewmodel.filter.toMapMarkerFilters
import com.example.fishing.viewmodel.session.UserSession
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * Map scenario: a session-scoped marker cache, viewport loading, filters,
 * one-shot location requests, the highlighted polygon and the last camera
 * position.
 *
 * Public markers are fetched for the visible viewport and merged into
 * [accumulated] so panning does not make previous markers disappear. Own
 * markers come from the offline cache and are always merged on top.
 */
class MapCoordinator(
    private val repository: FishingRepository,
    private val session: UserSession,
    private val scope: CoroutineScope,
    private val debounceMillis: Long = DEBOUNCE_MILLIS,
    private val zoomThreshold: Double = ZOOM_THRESHOLD,
    private val pageLimit: Int = PAGE_LIMIT,
    private val cacheLimit: Int = CACHE_LIMIT
) {
    private val _mapMarkers = MutableStateFlow<List<MarkerDomain>>(emptyList())
    val mapMarkers: StateFlow<List<MarkerDomain>> = _mapMarkers.asStateFlow()

    private val _mapIsLoading = MutableStateFlow(false)
    val mapIsLoading: StateFlow<Boolean> = _mapIsLoading.asStateFlow()

    private val _mapIsRefreshing = MutableStateFlow(false)
    val mapIsRefreshing: StateFlow<Boolean> = _mapIsRefreshing.asStateFlow()

    private val _mapRefreshError = MutableStateFlow<String?>(null)
    val mapRefreshError: StateFlow<String?> = _mapRefreshError.asStateFlow()

    private val _mapFilters = MutableStateFlow(ReportFilters())
    val mapFilters: StateFlow<ReportFilters> = _mapFilters.asStateFlow()

    private val _mapIsZoomedOut = MutableStateFlow(false)
    val mapIsZoomedOut: StateFlow<Boolean> = _mapIsZoomedOut.asStateFlow()

    private val _mapTruncated = MutableStateFlow(false)
    val mapTruncated: StateFlow<Boolean> = _mapTruncated.asStateFlow()

    private val _mapRequestedLocation = MutableStateFlow<GeoPoint?>(null)
    val mapRequestedLocation: StateFlow<GeoPoint?> = _mapRequestedLocation.asStateFlow()

    private val _highlightedPolygon = MutableStateFlow<List<GeoPoint>?>(null)
    val highlightedPolygon: StateFlow<List<GeoPoint>?> = _highlightedPolygon.asStateFlow()

    var mapLastCenterLat: Double? = null
    var mapLastCenterLon: Double? = null
    var mapLastZoom: Double = 6.0

    /** Session cache of public markers, keyed by report id (duplicates update). */
    private val accumulated = LinkedHashMap<UUID, MarkerDomain>()

    private var ownMarkers: List<MarkerDomain> = emptyList()

    /** Expanded area of the last successful, non-truncated load. */
    private var loadedViewport: MapBounds? = null
    private var currentBounds: MapBounds? = null
    private var currentZoom: Double = 6.0
    private var isMapVisible = false
    private var boundUserId: UUID? = null

    private var debounceJob: Job? = null
    private var loadJob: Job? = null
    private var ownMarkersJob: Job? = null

    /**
     * Binds the map to a signed-in user: drops the previous session cache and
     * starts observing the offline own markers. A repeated call for the same
     * user is a no-op, so token refreshes do not reset the map.
     */
    fun onUserChanged(userId: UUID) {
        if (boundUserId == userId) return
        boundUserId = userId
        resetSessionState()
        ownMarkersJob?.cancel()
        ownMarkersJob = scope.launch {
            repository.observeOwnMapMarkers(userId).collect { markers ->
                if (boundUserId != userId) return@collect
                ownMarkers = markers
                publishMarkers()
            }
        }
        publishMarkers()
    }

    /** The map tab became visible. Loads the current viewport when needed. */
    fun onMapShown() {
        session.resolve()?.let { userId ->
            if (boundUserId != userId) onUserChanged(userId)
        }
        isMapVisible = true
        if (accumulated.isEmpty() && loadedViewport == null && currentBounds != null) {
            scheduleLoad(currentBounds!!, immediate = true)
        }
    }

    /** The map tab is gone: cancel requests and drop the session cache. */
    fun onMapHidden() {
        isMapVisible = false
        cancelPendingLoads()
        clearPublicCache()
        publishMarkers()
    }

    /**
     * Camera moved. Below [zoomThreshold] nothing is requested; inside an
     * already loaded area (+margin) nothing is requested either.
     */
    fun onViewportChanged(bounds: MapBounds, zoom: Double) {
        currentBounds = bounds
        currentZoom = zoom
        mapLastZoom = zoom
        _mapIsZoomedOut.value = zoom < zoomThreshold
        if (!isMapVisible) return
        if (zoom < zoomThreshold) {
            cancelPendingLoads()
            _mapIsLoading.value = false
            _mapIsRefreshing.value = false
            return
        }

        val loaded = loadedViewport
        if (loaded != null && loaded.contains(bounds)) return
        scheduleLoad(bounds, immediate = false)
    }

    /** Filters applied from the filters screen: clear and reload the zone. */
    fun onFiltersApplied(filters: ReportFilters) {
        _mapFilters.value = filters
        clearPublicCache()
        publishMarkers()
        if (isMapVisible) {
            currentBounds?.let { bounds ->
                if (currentZoom >= zoomThreshold) {
                    scheduleLoad(bounds, immediate = true)
                }
            }
        }
    }

    /** Explicit pull-to-refresh/retry: drop the cache and reload the zone. */
    fun onRefreshRequested() {
        clearPublicCache()
        publishMarkers()
        if (isMapVisible) {
            currentBounds?.let { bounds ->
                if (currentZoom >= zoomThreshold) {
                    scheduleLoad(bounds, immediate = true)
                }
            }
        }
    }

    /** Kept for the current map chips; the filters screen will call [onFiltersApplied]. */
    fun updateMapFilters(transform: (ReportFilters) -> ReportFilters) {
        onFiltersApplied(transform(_mapFilters.value))
    }

    /** Compatibility entry point used by the current screen and retry button. */
    fun loadMapMarkers(force: Boolean = false) {
        if (force) onRefreshRequested() else onMapShown()
    }

    fun requestMapLocation(point: GeoPoint?) {
        _mapRequestedLocation.value = point
    }

    fun setHighlightedPolygon(points: List<GeoPoint>?) {
        _highlightedPolygon.value = points
    }

    /** Logout: drop everything, including the camera and filters. */
    fun clear() {
        boundUserId = null
        ownMarkersJob?.cancel()
        ownMarkersJob = null
        ownMarkers = emptyList()
        resetSessionState()
        mapLastCenterLat = null
        mapLastCenterLon = null
        mapLastZoom = 6.0
        currentZoom = 6.0
        _mapFilters.value = ReportFilters()
        _mapIsZoomedOut.value = false
    }

    private fun resetSessionState() {
        cancelPendingLoads()
        clearPublicCache()
        ownMarkers = emptyList()
        publishMarkers()
        _mapIsLoading.value = false
        _mapIsRefreshing.value = false
        _mapRefreshError.value = null
        _mapIsZoomedOut.value = false
        currentBounds = null
    }

    private fun scheduleLoad(bounds: MapBounds, immediate: Boolean) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            if (!immediate) delay(debounceMillis)
            if (!isMapVisible || currentZoom < zoomThreshold) return@launch
            loadViewport(bounds.expandedBy(VIEWPORT_MARGIN))
        }
    }

    private suspend fun loadViewport(requestBounds: MapBounds) {
        loadJob?.cancel()
        val hadMarkers = accumulated.isNotEmpty()
        _mapIsLoading.value = !hadMarkers
        _mapIsRefreshing.value = hadMarkers
        _mapRefreshError.value = null

        try {
            val result = repository.loadPublicMapMarkers(
                bounds = requestBounds,
                filters = _mapFilters.value.toMapMarkerFilters(),
                limit = pageLimit
            )
            result.onSuccess { page ->
                page.markers.forEach { marker -> accumulated[marker.id] = marker }
                _mapTruncated.value = page.truncated
                if (!page.truncated) {
                    loadedViewport = requestBounds
                }
                evictIfNeeded()
                publishMarkers()
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _mapRefreshError.value =
                    "Не удалось загрузить отчёты: ${ErrorMapper.toUserFriendlyMessage(error)}"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _mapRefreshError.value =
                "Не удалось загрузить отчёты: ${ErrorMapper.toUserFriendlyMessage(e)}"
        } finally {
            _mapIsLoading.value = false
            _mapIsRefreshing.value = false
        }
    }

    private fun publishMarkers() {
        val filters = _mapFilters.value
        val merged = LinkedHashMap<UUID, MarkerDomain>(accumulated.size + ownMarkers.size)
        accumulated.values.forEach { marker ->
            if (filters.matches(marker, emptySet())) merged[marker.id] = marker
        }
        ownMarkers.forEach { marker ->
            if (filters.matches(marker, emptySet())) merged[marker.id] = marker
        }
        _mapMarkers.value = merged.values.toList()
    }

    private fun clearPublicCache() {
        accumulated.clear()
        loadedViewport = null
        _mapTruncated.value = false
    }

    private fun cancelPendingLoads() {
        debounceJob?.cancel()
        debounceJob = null
        loadJob?.cancel()
        loadJob = null
    }

    /** Keeps the cache bounded by dropping markers farthest from the camera. */
    private fun evictIfNeeded() {
        if (accumulated.size <= cacheLimit) return
        val bounds = currentBounds ?: return
        val centerLat = (bounds.south + bounds.north) / 2.0
        val centerLng = (bounds.west + bounds.east) / 2.0
        val kept = accumulated.values
            .sortedBy { marker ->
                val dLat = marker.waterLat - centerLat
                val dLng = marker.waterLng - centerLng
                dLat * dLat + dLng * dLng
            }
            .take(cacheLimit)
        accumulated.clear()
        kept.forEach { accumulated[it.id] = it }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 350L
        const val ZOOM_THRESHOLD = 10.0
        const val VIEWPORT_MARGIN = 0.25
        const val PAGE_LIMIT = 300
        const val CACHE_LIMIT = 2000
    }
}
