package com.example.fishing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.UserPreferencesSource
import com.example.fishing.data.upload.ReportUploadQueue
import com.example.fishing.model.AuthState
import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.PendingReportUpload
import com.example.fishing.model.ReportDisplayMode
import com.example.fishing.model.ReportLikeState
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.model.ReportUploadNotice
import com.example.fishing.viewmodel.detail.ReportDetailCoordinator
import com.example.fishing.viewmodel.favorites.FavoritesCoordinator
import com.example.fishing.viewmodel.filter.ReportFilters
import com.example.fishing.viewmodel.filter.ReportSearchState
import com.example.fishing.viewmodel.home.HomeFeedCoordinator
import com.example.fishing.viewmodel.likes.LikesCoordinator
import com.example.fishing.viewmodel.map.MapCoordinator
import com.example.fishing.viewmodel.photos.PhotoUrlResolver
import com.example.fishing.viewmodel.search.SearchCoordinator
import com.example.fishing.viewmodel.session.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.UUID
import javax.inject.Inject

/**
 * Binding point between the Compose navigation graph and the per-scenario
 * coordinators. It owns only session orchestration, cross-scenario refresh and
 * upload-queue notices; all scenario logic lives in the coordinators.
 *
 * The public surface is intentionally kept stable so the existing screens and
 * MainViewModelTest continue to work without changes.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: FishingRepository,
    private val authRepository: AuthRepository,
    userPreferencesRepository: UserPreferencesSource,
    private val reportUploadQueue: ReportUploadQueue
) : ViewModel() {

    private val session = UserSession(authRepository)
    private val photoUrls = PhotoUrlResolver(repository)

    private val home = HomeFeedCoordinator(
        repository = repository,
        userPreferencesRepository = userPreferencesRepository,
        session = session,
        photoUrls = photoUrls,
        scope = viewModelScope
    )
    private val favorites = FavoritesCoordinator(
        repository = repository,
        session = session,
        photoUrls = photoUrls,
        scope = viewModelScope
    )
    private val likes = LikesCoordinator(
        repository = repository,
        session = session,
        scope = viewModelScope
    )
    private val map = MapCoordinator(
        repository = repository,
        scope = viewModelScope
    )
    private val search = SearchCoordinator()
    private val detail = ReportDetailCoordinator(
        repository = repository,
        photoUrls = photoUrls,
        scope = viewModelScope
    )

    val reports: StateFlow<List<FishingReport>> = home.reports
    val sortedReports: StateFlow<List<FishingReport>> = home.sortedReports
    val homeUiState: StateFlow<HomeUiState> = home.homeUiState
    val reportSortOrder: StateFlow<ReportSortOrder> = home.reportSortOrder
    val reportDisplayMode: StateFlow<ReportDisplayMode> = home.reportDisplayMode
    val isInitialLoading: StateFlow<Boolean> = home.isInitialLoading
    val isRefreshing: StateFlow<Boolean> = home.isRefreshing
    val isLoadingMore: StateFlow<Boolean> = home.isLoadingMore
    val loadMoreError: StateFlow<String?> = home.loadMoreError
    val hasMoreReports: StateFlow<Boolean> = home.hasMoreReports
    val error: StateFlow<String?> = home.error

    val favoriteIds: StateFlow<Set<UUID>> = favorites.favoriteIds
    val favoriteOpsInFlight: StateFlow<Set<UUID>> = favorites.favoriteOpsInFlight
    val filterFavoriteReports: StateFlow<List<FishingReport>> = favorites.filterFavoriteReports
    val favoriteError: StateFlow<String?> = favorites.favoriteError

    val likeStates: StateFlow<Map<UUID, ReportLikeState>> = likes.likeStates
    val likeError: StateFlow<String?> = likes.likeError

    val mapMarkers: StateFlow<List<MarkerDomain>> = map.mapMarkers
    val mapIsLoading: StateFlow<Boolean> = map.mapIsLoading
    val mapIsRefreshing: StateFlow<Boolean> = map.mapIsRefreshing
    val mapRefreshError: StateFlow<String?> = map.mapRefreshError
    val mapFilters: StateFlow<ReportFilters> = map.mapFilters
    val mapRequestedLocation: StateFlow<GeoPoint?> = map.mapRequestedLocation
    val highlightedPolygon: StateFlow<List<GeoPoint>?> = map.highlightedPolygon

    var mapLastCenterLat: Double?
        get() = map.mapLastCenterLat
        set(value) {
            map.mapLastCenterLat = value
        }
    var mapLastCenterLon: Double?
        get() = map.mapLastCenterLon
        set(value) {
            map.mapLastCenterLon = value
        }
    var mapLastZoom: Double
        get() = map.mapLastZoom
        set(value) {
            map.mapLastZoom = value
        }

    val searchState: StateFlow<ReportSearchState> = search.state

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val reportDetailUiState: StateFlow<ReportDetailUiState> = detail.reportDetailUiState
    val isDeletingReport: StateFlow<Boolean> = detail.isDeletingReport
    val deleteReportError: StateFlow<String?> = detail.deleteReportError
    val deletedReportId: StateFlow<UUID?> = detail.deletedReportId
    val isUpdatingReportVisibility: StateFlow<Boolean> = detail.isUpdatingVisibility
    val reportVisibilityError: StateFlow<String?> = detail.visibilityError
    val reportVisibilityResult: StateFlow<String?> = detail.visibilityResult

    /** Reports waiting for upload; the banner in [MainScreen] is driven by this. */
    val pendingUploads: StateFlow<List<PendingReportUpload>> =
        reportUploadQueue.observePending()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uploadNotice = MutableStateFlow<ReportUploadNotice?>(null)
    val uploadNotice: StateFlow<ReportUploadNotice?> = _uploadNotice.asStateFlow()

    // Remembers which user the home state was already loaded for, so repeated
    // Authenticated emissions (e.g. token refreshes) don't restart every load.
    private var loadedForUserId: UUID? = null

    init {
        viewModelScope.launch {
            authRepository.authState
                .filterIsInstance<AuthState.Authenticated>()
                .collect { authState ->
                    val userId = authState.user.id
                    if (userId == loadedForUserId) return@collect
                    loadedForUserId = userId
                    session.set(userId)
                    home.onUserChanged(userId)
                    home.loadReports(force = false)
                    // Another account may have its own camera/filters state and
                    // own private markers; drop both before loading the map.
                    map.clear()
                    map.loadMapMarkers(force = false)
                    likes.refresh()
                    favorites.refresh()
                }
        }
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state == AuthState.Unauthenticated) {
                    clearUserContent()
                }
            }
        }
        viewModelScope.launch {
            reportUploadQueue.observeResults().collect { result ->
                _uploadNotice.value = if (result.success) {
                    ReportUploadNotice.Published(isPublic = result.isPublic)
                } else {
                    ReportUploadNotice.Failed(result.message.orEmpty())
                }
            }
        }
    }

    fun retryPendingUpload(reportId: UUID) {
        viewModelScope.launch { reportUploadQueue.retry(reportId) }
    }

    fun cancelPendingUpload(reportId: UUID) {
        viewModelScope.launch { reportUploadQueue.cancel(reportId) }
    }

    fun consumeUploadNotice() {
        _uploadNotice.value = null
    }

    private fun clearUserContent() {
        session.clear()
        loadedForUserId = null

        home.clear()
        favorites.clear()
        likes.clear()
        map.clear()
        search.clear()
        detail.clear()

        _selectedTab.value = 0
        _uploadNotice.value = null
        photoUrls.clear()
    }

    fun refresh() {
        home.loadReports(force = true)
        map.loadMapMarkers(force = true)
        likes.refresh()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        if (index == 1) map.loadMapMarkers(force = false)
    }

    fun setSortOrder(order: ReportSortOrder) {
        home.setSortOrder(order)
    }

    fun setDisplayMode(mode: ReportDisplayMode) {
        home.setDisplayMode(mode)
    }

    fun requestMapLocation(point: GeoPoint?) {
        map.requestMapLocation(point)
    }

    fun setHighlightedPolygon(points: List<GeoPoint>?) {
        map.setHighlightedPolygon(points)
    }

    fun updateMapFilters(transform: (ReportFilters) -> ReportFilters) {
        map.updateMapFilters(transform)
    }

    fun updateSearchState(transform: (ReportSearchState) -> ReportSearchState) {
        search.update(transform)
    }

    fun loadReportsIfNeeded() {
        home.loadReports(force = false)
    }

    fun loadMapMarkers(force: Boolean = false) {
        map.loadMapMarkers(force)
    }

    fun loadFilterFavoriteReports(force: Boolean = false) {
        favorites.loadFilterFavoriteReports(force)
    }

    fun toggleFavorite(report: FishingReport) {
        favorites.toggleFavorite(report)
    }

    fun refreshFavorites() {
        favorites.refresh()
    }

    fun clearFavoriteError() {
        favorites.clearFavoriteError()
    }

    fun refreshLikes() {
        likes.refresh()
    }

    fun toggleLike(report: FishingReport) {
        likes.toggleLike(report)
    }

    fun clearLikeError() {
        likes.clearLikeError()
    }

    fun loadReportDetails(id: UUID) {
        detail.loadReportDetails(id)
    }

    fun loadMoreReports() {
        home.loadMoreReports()
    }

    fun deleteReport(id: UUID) {
        detail.deleteReport(id)
    }

    fun clearDeleteReportError() {
        detail.clearDeleteReportError()
    }

    fun clearDeletedReport() {
        detail.clearDeletedReport()
    }

    fun setReportVisibility(id: UUID, isPublic: Boolean) {
        detail.updateVisibility(id, isPublic)
    }

    fun clearReportVisibilityError() {
        detail.clearVisibilityError()
    }

    fun clearReportVisibilityResult() {
        detail.clearVisibilityResult()
    }
}
