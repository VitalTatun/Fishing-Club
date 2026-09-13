package com.example.fishing.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.SupabaseFishingRepository
import com.example.fishing.data.UserPreferencesSource
import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import java.util.Date
import com.example.fishing.model.*
import java.util.UUID

sealed interface ReportDetailUiState {
    data object Loading : ReportDetailUiState

    data class Success(val report: FishingReport) : ReportDetailUiState

    // Report is confirmed absent in the backend, not merely not-yet-loaded.
    data class Empty(val reportId: UUID) : ReportDetailUiState

    data class Error(val reportId: UUID, val message: String) : ReportDetailUiState
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val reports: List<FishingReport>) : HomeUiState
    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FishingRepository,
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesSource
) : ViewModel() {

    private val _reports = MutableStateFlow<List<FishingReport>>(emptyList())
    val reports: StateFlow<List<FishingReport>> = _reports.asStateFlow()

    private val _favoriteReports = MutableStateFlow<List<FishingReport>>(emptyList())
    val favoriteReports: StateFlow<List<FishingReport>> = _favoriteReports.asStateFlow()

    private val _reportSortOrder = MutableStateFlow(userPreferencesRepository.getSortOrder())
    val reportSortOrder: StateFlow<ReportSortOrder> = _reportSortOrder.asStateFlow()

    val sortedReports: StateFlow<List<FishingReport>> = combine(
        reports,
        reportSortOrder
    ) { reports, order ->
        when (order) {
            ReportSortOrder.BY_PUBLISH_DATE -> reports.sortedByDescending { it.createdAt ?: it.publishedAt }
            ReportSortOrder.BY_FISHING_TIME -> reports.sortedByDescending { it.fishingStartAt ?: it.createdAt?.toInstant() }
        }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _mapMarkers = MutableStateFlow<List<MarkerDomain>>(emptyList())
    val mapMarkers: StateFlow<List<MarkerDomain>> = _mapMarkers.asStateFlow()

    private val _mapIsLoading = MutableStateFlow(false)
    val mapIsLoading: StateFlow<Boolean> = _mapIsLoading.asStateFlow()

    private val _mapIsRefreshing = MutableStateFlow(false)
    val mapIsRefreshing: StateFlow<Boolean> = _mapIsRefreshing.asStateFlow()

    private val _mapRefreshError = MutableStateFlow<String?>(null)
    val mapRefreshError: StateFlow<String?> = _mapRefreshError.asStateFlow()

    private val _reportDetailUiState = MutableStateFlow<ReportDetailUiState>(ReportDetailUiState.Loading)
    val reportDetailUiState: StateFlow<ReportDetailUiState> = _reportDetailUiState.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _mapRequestedLocation = MutableStateFlow<GeoPoint?>(null)
    val mapRequestedLocation: StateFlow<GeoPoint?> = _mapRequestedLocation.asStateFlow()

    private val _highlightedPolygon = MutableStateFlow<List<GeoPoint>?>(null)
    val highlightedPolygon: StateFlow<List<GeoPoint>?> = _highlightedPolygon.asStateFlow()

    var mapLastCenterLat: Double? = null
    var mapLastCenterLon: Double? = null
    var mapLastZoom: Double = 6.0

    // Search state (persists across navigation)
    var searchQuery by mutableStateOf("")
    var searchSelectedDate by mutableStateOf<Long?>(null)
    var searchIsFavoritesSelected by mutableStateOf(false)
    var searchIsTrophySelected by mutableStateOf(false)
    var searchIsPaidSelected by mutableStateOf(false)
    var searchSelectedCatch by mutableStateOf<String?>(null)
    var searchSelectedMethod by mutableStateOf<FishingMethod?>(null)

    // Map Search state (persists across navigation)
    var mapIsFavoritesSelected by mutableStateOf(false)
    var mapIsTrophySelected by mutableStateOf(false)
    var mapIsPaidSelected by mutableStateOf(false)
    var mapSelectedCatch by mutableStateOf<String?>(null)
    var mapSelectedMethod by mutableStateOf<FishingMethod?>(null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isDeletingReport = MutableStateFlow(false)
    val isDeletingReport: StateFlow<Boolean> = _isDeletingReport.asStateFlow()

    private val _deleteReportError = MutableStateFlow<String?>(null)
    val deleteReportError: StateFlow<String?> = _deleteReportError.asStateFlow()

    private val _favoriteError = MutableStateFlow<String?>(null)
    val favoriteError: StateFlow<String?> = _favoriteError.asStateFlow()

    private val favoriteOpsInFlight = mutableSetOf<UUID>()

    private val _likeStates = MutableStateFlow<Map<UUID, ReportLikeState>>(emptyMap())
    val likeStates: StateFlow<Map<UUID, ReportLikeState>> = _likeStates.asStateFlow()

    private val _likeError = MutableStateFlow<String?>(null)
    val likeError: StateFlow<String?> = _likeError.asStateFlow()

    private val likeOpsInFlight = mutableSetOf<UUID>()
    private val optimisticLikeOverlay = mutableMapOf<UUID, ReportLikeState>()
    private var baseLikeStates: Map<UUID, ReportLikeState> = emptyMap()

    private val _deletedReportId = MutableStateFlow<UUID?>(null)
    val deletedReportId: StateFlow<UUID?> = _deletedReportId.asStateFlow()

    val homeUiState: StateFlow<HomeUiState> = combine(
        sortedReports,
        isInitialLoading,
        error
    ) { reportsList, initialLoading, currentError ->
        when {
            initialLoading && reportsList.isEmpty() && currentError == null -> HomeUiState.Loading
            reportsList.isEmpty() && currentError != null -> HomeUiState.Error(currentError)
            reportsList.isEmpty() -> HomeUiState.Empty
            else -> HomeUiState.Success(reportsList)
        }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState.Loading)

    private var reportsLoadJob: Job? = null
    private var mapMarkersLoadJob: Job? = null
    private var reportDetailsJob: Job? = null
    private var deleteReportJob: Job? = null
    private var likesLoadJob: Job? = null
    private val signedPhotoUrlCache = mutableMapOf<String, String>()

    private var currentUserId: UUID? = null

    init {
        viewModelScope.launch {
            authRepository.authState
                .filterIsInstance<AuthState.Authenticated>()
                .collect { authState ->
                    currentUserId = authState.user.id
                    _reportSortOrder.value = userPreferencesRepository.getSortOrder(authState.user.id)
                    loadReports(force = false)
                    loadMapMarkers(force = false)
                    refreshLikes()
                }
        }
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state == AuthState.Unauthenticated) {
                    clearUserContent()
                }
            }
        }
    }

    private fun clearUserContent() {
        reportsLoadJob?.cancel()
        mapMarkersLoadJob?.cancel()
        reportDetailsJob?.cancel()
        deleteReportJob?.cancel()
        likesLoadJob?.cancel()
        currentUserId = null

        _reports.value = emptyList()
        _favoriteReports.value = emptyList()
        _likeStates.value = emptyMap()
        baseLikeStates = emptyMap()
        optimisticLikeOverlay.clear()
        likeOpsInFlight.clear()
        _likeError.value = null
        _mapMarkers.value = emptyList()
        _reportDetailUiState.value = ReportDetailUiState.Loading
        _error.value = null
        _isDeletingReport.value = false
        _deleteReportError.value = null
        _favoriteError.value = null
        favoriteOpsInFlight.clear()
        _deletedReportId.value = null
        _isInitialLoading.value = true
        _isRefreshing.value = false
        _selectedTab.value = 0
        _mapIsLoading.value = true
        _mapIsRefreshing.value = false
        _mapRefreshError.value = null

        searchQuery = ""
        searchSelectedDate = null
        searchIsFavoritesSelected = false
        searchIsTrophySelected = false
        searchIsPaidSelected = false
        searchSelectedCatch = null
        searchSelectedMethod = null

        mapIsFavoritesSelected = false
        mapIsTrophySelected = false
        mapIsPaidSelected = false
        mapSelectedCatch = null
        mapSelectedMethod = null
        mapLastCenterLat = null
        mapLastCenterLon = null
        mapLastZoom = 6.0

        signedPhotoUrlCache.clear()
    }

    fun refresh() {
        loadReports(force = true)
        loadMapMarkers(force = true)
        refreshLikes()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        if (index == 1) loadMapMarkers(force = false)
    }

    fun setSortOrder(order: ReportSortOrder) {
        _reportSortOrder.value = order
        currentUserId?.let { userPreferencesRepository.setSortOrder(it, order) }
    }

    fun requestMapLocation(point: GeoPoint?) {
        _mapRequestedLocation.value = point
    }

    fun setHighlightedPolygon(points: List<GeoPoint>?) {
        _highlightedPolygon.value = points
    }

    fun loadReportsIfNeeded() {
        loadReports(force = false)
    }

    fun loadMapMarkers(force: Boolean = false) {
        if (!force && (_mapMarkers.value.isNotEmpty() || (mapMarkersLoadJob?.isActive == true))) {
            return
        }

        mapMarkersLoadJob?.cancel()
        mapMarkersLoadJob = viewModelScope.launch {
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
            // Errors are propagated so the ViewModel can distinguish "no data yet",
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
                        "Не удалось загрузить отчёты: ${result.exceptionOrNull()?.message ?: "неизвестная ошибка"}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mapRefreshError.value = "Не удалось загрузить отчёты: ${e.message ?: "неизвестная ошибка"}"
            } finally {
                _mapIsRefreshing.value = false
                _mapIsLoading.value = false
            }
        }
    }

    fun toggleFavorite(report: FishingReport) {
        if (!favoriteOpsInFlight.add(report.id)) return

        _favoriteError.value = null
        val isFavorite = _favoriteReports.value.any { it.id == report.id }

        viewModelScope.launch {
            try {
                val result = if (isFavorite) {
                    repository.removeFavorite(report.id)
                } else {
                    repository.addFavorite(report)
                }
                result.onFailure { e ->
                    _favoriteError.value = favoriteErrorMessage(e)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _favoriteError.value = favoriteErrorMessage(e)
            } finally {
                favoriteOpsInFlight.remove(report.id)
            }
        }
    }

    private fun favoriteErrorMessage(e: Throwable): String {
        return "Не удалось изменить избранное: ${e.message ?: "неизвестная ошибка"}"
    }

    fun clearFavoriteError() {
        _favoriteError.value = null
    }

    fun refreshLikes() {
        val userId = currentUserId ?: authRepository.currentUser()?.id
        if (userId == null) return
        currentUserId = userId

        // Cancel + relaunch (same as loadReports(force = true)): the collect
        // child below never completes, so an isActive guard would block every
        // later refresh, including pull-to-refresh. In-flight toggles survive
        // because they run in viewModelScope, not in this job.
        likesLoadJob?.cancel()
        likesLoadJob = viewModelScope.launch {
            // Observe Room-confirmed states; network refresh runs concurrently
            // and its result lands in Room → the flow below re-emits.
            launch {
                try {
                    repository.getLikeStates(userId).collect { base ->
                        // Stale emission after logout/account switch must not restore state.
                        if (currentUserId != userId) return@collect
                        baseLikeStates = base
                        _likeStates.value = base + activeLikeOverlay()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Reads are cache-first: a failed refresh keeps the existing states.
            try {
                repository.refreshLikes(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleLike(report: FishingReport) {
        val userId = currentUserId ?: authRepository.currentUser()?.id
        if (userId == null) {
            _likeError.value = "Нет активной сессии"
            return
        }
        // Self-like is blocked locally: repository/RPC is never called.
        if (report.userId == userId) return
        // Double-tap guard: no queue, the second tap is ignored while in flight.
        if (!likeOpsInFlight.add(report.id)) return

        _likeError.value = null
        val current = _likeStates.value[report.id]
            ?: baseLikeStates[report.id]
            ?: ReportLikeState(report.id)
        val liking = !current.isLiked
        val optimistic = current.copy(
            isLiked = liking,
            likesCount = (current.likesCount + if (liking) 1 else -1).coerceAtLeast(0)
        )
        // Optimistic state has priority over Room emits for the op duration.
        optimisticLikeOverlay[report.id] = optimistic
        _likeStates.value = baseLikeStates + activeLikeOverlay()

        viewModelScope.launch {
            try {
                val result = if (liking) {
                    repository.addLike(report.id)
                } else {
                    repository.removeLike(report.id)
                }
                // Late results after logout/account switch must not restore state.
                if (currentUserId != userId) return@launch
                result.onSuccess {
                    // Keep the optimistic values as the confirmed state.
                    optimisticLikeOverlay.remove(report.id)
                    _likeStates.value = _likeStates.value + (report.id to optimistic)
                }.onFailure { e ->
                    optimisticLikeOverlay.remove(report.id)
                    _likeStates.value = baseLikeStates + activeLikeOverlay()
                    _likeError.value = likeErrorMessage(e)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (currentUserId != userId) return@launch
                optimisticLikeOverlay.remove(report.id)
                _likeStates.value = baseLikeStates + activeLikeOverlay()
                _likeError.value = likeErrorMessage(e)
            } finally {
                likeOpsInFlight.remove(report.id)
            }
        }
    }

    private fun activeLikeOverlay(): Map<UUID, ReportLikeState> =
        optimisticLikeOverlay.filterKeys { it in likeOpsInFlight }

    private fun likeErrorMessage(e: Throwable): String {
        return "Не удалось изменить лайк: ${e.message ?: "неизвестная ошибка"}"
    }

    fun clearLikeError() {
        _likeError.value = null
    }

    fun loadReportDetails(id: UUID) {
        reportDetailsJob?.cancel()
        _deleteReportError.value = null
        _reportDetailUiState.value = ReportDetailUiState.Loading

        reportDetailsJob = viewModelScope.launch {
            // Observe Room cache and publish Success the moment the report is available.
            launch {
                try {
                    repository.getReportDetails(id).collect { report ->
                        if (report != null) {
                            _reportDetailUiState.value = ReportDetailUiState.Success(
                                report.copy(photos = resolvePhotoUrls(report.photos))
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (_reportDetailUiState.value !is ReportDetailUiState.Success) {
                        _reportDetailUiState.value = reportDetailError(id, e)
                    }
                }
            }
            // Authoritative network refresh: it knows whether the report actually exists,
            // so Empty can be decided without racing Room's async emission.
            try {
                val found = repository is SupabaseFishingRepository &&
                    repository.refreshReportDetails(id)
                if (!found && _reportDetailUiState.value == ReportDetailUiState.Loading) {
                    _reportDetailUiState.value = ReportDetailUiState.Empty(id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_reportDetailUiState.value !is ReportDetailUiState.Success) {
                    _reportDetailUiState.value = reportDetailError(id, e)
                }
            }
        }
    }

    private fun reportDetailError(id: UUID, e: Exception): ReportDetailUiState.Error {
        return ReportDetailUiState.Error(
            reportId = id,
            message = "Ошибка загрузки отчета: ${e.message ?: "неизвестная"}"
        )
    }

    private fun loadReports(force: Boolean) {
        if (!force && (_reports.value.isNotEmpty() || reportsLoadJob?.isActive == true)) {
            return
        }

        val userId = currentUserId ?: authRepository.currentUser()?.id
        if (userId == null) return
        currentUserId = userId

        reportsLoadJob?.cancel()
        reportsLoadJob = viewModelScope.launch {
            _error.value = null
            if (_reports.value.isEmpty()) {
                _isInitialLoading.value = true
            } else {
                _isRefreshing.value = true
            }

            // Observe Room cache (updates UI on every DB change)
            launch {
                try {
                    repository.getHomeReports(userId = userId).collect { reports ->
                        _reports.value = reports.map { report ->
                            report.copy(
                                photos = resolvePhotoUrls(report.photos)
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            launch {
                try {
                    repository.getFavoriteReports(userId).collect { reports ->
                        _favoriteReports.value = reports.map { report ->
                            report.copy(photos = resolvePhotoUrls(report.photos))
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Refresh from network → saves to Room → Flow auto-updates UI
            try {
                repository.refreshHomeReports(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Ошибка загрузки: ${e.message ?: "неизвестная"}"
            } finally {
                _isInitialLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun resolvePhotoUrls(photos: List<FishingPhoto>): List<FishingPhoto> {
        return photos.map { photo ->
            if (repository.isStoragePath(photo.url)) {
                val signedUrl = signedPhotoUrlCache.getOrPut(photo.url) {
                    repository.getPhotoSignedUrl(photo.url) ?: photo.url
                }
                photo.copy(url = signedUrl)
            } else {
                photo
            }
        }
    }

    fun deleteReport(id: UUID) {
        if (_isDeletingReport.value) return

        _isDeletingReport.value = true
        _deleteReportError.value = null
        _deletedReportId.value = null

        deleteReportJob = viewModelScope.launch {
            repository.deleteReport(id)
                .onSuccess {
                    _deletedReportId.value = id
                }
                .onFailure { e ->
                    _deleteReportError.value = "Не удалось удалить отчет: ${e.message ?: "неизвестная ошибка"}"
                }
            _isDeletingReport.value = false
        }
    }

    fun clearDeleteReportError() {
        _deleteReportError.value = null
    }

    fun clearDeletedReport() {
        _deletedReportId.value = null
    }
}
