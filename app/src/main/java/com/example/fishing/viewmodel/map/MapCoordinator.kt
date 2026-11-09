package com.example.fishing.viewmodel.map

import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.MarkerDomain
import com.example.fishing.viewmodel.filter.ReportFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * Map scenario: marker loading, refresh state, filter selection, one-shot
 * location requests, the highlighted polygon and the last camera position.
 */
class MapCoordinator(
    private val repository: FishingRepository,
    private val scope: CoroutineScope
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

    private val _mapRequestedLocation = MutableStateFlow<GeoPoint?>(null)
    val mapRequestedLocation: StateFlow<GeoPoint?> = _mapRequestedLocation.asStateFlow()

    private val _highlightedPolygon = MutableStateFlow<List<GeoPoint>?>(null)
    val highlightedPolygon: StateFlow<List<GeoPoint>?> = _highlightedPolygon.asStateFlow()

    var mapLastCenterLat: Double? = null
    var mapLastCenterLon: Double? = null
    var mapLastZoom: Double = 6.0

    private var mapMarkersLoadJob: Job? = null

    fun loadMapMarkers(force: Boolean = false) {
        if (!force && (_mapMarkers.value.isNotEmpty() || (mapMarkersLoadJob?.isActive == true))) {
            return
        }

        mapMarkersLoadJob?.cancel()
        mapMarkersLoadJob = scope.launch {
            if (_mapMarkers.value.isEmpty()) {
                _mapIsLoading.value = true
            }
            _mapIsRefreshing.value = false
            _mapRefreshError.value = null

            // Observe Room cache (never completes — updates UI on every DB change)
            launch {
                try {
                    repository.getMapMarkers().collect { markers ->
                        _mapMarkers.value = markers
                        if (markers.isNotEmpty()) {
                            _mapIsLoading.value = false
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Refresh from network → saves to Room → Flow auto-updates UI.
            // Errors are propagated so the coordinator can distinguish "no data yet",
            // "cache kept + refresh failed" and "nothing loaded at all".
            if (_mapMarkers.value.isNotEmpty()) {
                _mapIsRefreshing.value = true
            }
            try {
                val result = repository.refreshMapMarkers()
                if (result.isSuccess) {
                    val fresh = result.getOrNull()
                    if (!fresh.isNullOrEmpty()) {
                        _mapMarkers.value = fresh
                    }
                    _mapRefreshError.value = null
                } else {
                    _mapRefreshError.value =
                        "Не удалось загрузить отчёты: ${
                            ErrorMapper.toUserFriendlyMessage(
                                result.exceptionOrNull() ?: Exception("неизвестная ошибка")
                            )
                        }"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mapRefreshError.value =
                    "Не удалось загрузить отчёты: ${ErrorMapper.toUserFriendlyMessage(e)}"
            } finally {
                _mapIsRefreshing.value = false
                _mapIsLoading.value = false
            }
        }
    }

    fun updateMapFilters(transform: (ReportFilters) -> ReportFilters) {
        _mapFilters.update(transform)
    }

    fun requestMapLocation(point: GeoPoint?) {
        _mapRequestedLocation.value = point
    }

    fun setHighlightedPolygon(points: List<GeoPoint>?) {
        _highlightedPolygon.value = points
    }

    fun clear() {
        mapMarkersLoadJob?.cancel()
        _mapMarkers.value = emptyList()
        _mapIsLoading.value = true
        _mapIsRefreshing.value = false
        _mapRefreshError.value = null
        _mapFilters.value = ReportFilters()
        mapLastCenterLat = null
        mapLastCenterLon = null
        mapLastZoom = 6.0
    }
}
