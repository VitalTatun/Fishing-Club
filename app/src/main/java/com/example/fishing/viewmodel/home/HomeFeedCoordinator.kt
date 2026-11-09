package com.example.fishing.viewmodel.home

import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.UserPreferencesSource
import com.example.fishing.model.FishingReport
import com.example.fishing.model.ReportDisplayMode
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.viewmodel.HomeUiState
import com.example.fishing.viewmodel.photos.PhotoUrlResolver
import com.example.fishing.viewmodel.session.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Report feed scenario: cache-first list, first page refresh, pagination,
 * sorting, display mode and the derived [HomeUiState].
 */
class HomeFeedCoordinator(
    private val repository: FishingRepository,
    private val userPreferencesRepository: UserPreferencesSource,
    private val session: UserSession,
    private val photoUrls: PhotoUrlResolver,
    private val scope: CoroutineScope
) {
    private val _reports = MutableStateFlow<List<FishingReport>>(emptyList())
    val reports: StateFlow<List<FishingReport>> = _reports.asStateFlow()

    private val _reportSortOrder = MutableStateFlow(userPreferencesRepository.getSortOrder())
    val reportSortOrder: StateFlow<ReportSortOrder> = _reportSortOrder.asStateFlow()

    private val _reportDisplayMode = MutableStateFlow(userPreferencesRepository.getDisplayMode())
    val reportDisplayMode: StateFlow<ReportDisplayMode> = _reportDisplayMode.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    /** Surfaced at the bottom of the home list when the next page fails to load. */
    private val _loadMoreError = MutableStateFlow<String?>(null)
    val loadMoreError: StateFlow<String?> = _loadMoreError.asStateFlow()

    private val _hasMoreReports = MutableStateFlow(false)
    val hasMoreReports: StateFlow<Boolean> = _hasMoreReports.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val sortedReports: StateFlow<List<FishingReport>> = combine(
        reports,
        reportSortOrder
    ) { reports, order ->
        when (order) {
            ReportSortOrder.BY_PUBLISH_DATE ->
                reports.sortedByDescending { it.createdAt ?: it.publishedAt }
            ReportSortOrder.BY_FISHING_TIME ->
                reports.sortedByDescending { it.fishingStartAt ?: it.createdAt?.toInstant() }
        }
    }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), HomeUiState.Loading)

    private var reportsLoadJob: Job? = null
    private var homePhotosVersion = 0

    /** Applies per-user preferences after login or account switch. */
    fun onUserChanged(userId: UUID) {
        _reportSortOrder.value = userPreferencesRepository.getSortOrder(userId)
        _reportDisplayMode.value = userPreferencesRepository.getDisplayMode(userId)
    }

    fun loadReports(force: Boolean) {
        if (!force && (_reports.value.isNotEmpty() || reportsLoadJob?.isActive == true)) {
            return
        }

        val userId = session.bind()
        if (userId == null) {
            // Without a session no data will ever arrive: resolve the loader and
            // surface an error instead of spinning forever.
            _isInitialLoading.value = false
            _error.value = "Нет активной сессии. Войдите снова."
            return
        }

        reportsLoadJob?.cancel()
        reportsLoadJob = scope.launch {
            _error.value = null
            _loadMoreError.value = null
            if (_reports.value.isEmpty()) {
                _isInitialLoading.value = true
            } else {
                _isRefreshing.value = true
            }

            // Observe Room cache (updates UI on every DB change)
            launch {
                try {
                    repository.getHomeReports(userId = userId).collect { reports ->
                        // Show cached rows at once; signed URLs are resolved in a
                        // separate coroutine so the UI is not delayed by network I/O.
                        _reports.value = photoUrls.applyCachedPhotoUrls(reports)
                        // Cached data is enough to leave the full-screen loading
                        // state; the network refresh keeps running quietly below.
                        if (reports.isNotEmpty()) {
                            _isInitialLoading.value = false
                        }
                        val version = ++homePhotosVersion
                        launch {
                            val resolved = photoUrls.resolveReportsPhotoUrls(reports)
                            if (version == homePhotosVersion) {
                                _reports.value = resolved
                            }
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
                val result = repository.refreshHomeReports(userId)
                _hasMoreReports.value = result.hasMore
                // A genuinely empty snapshot will never produce a Room emission,
                // so resolve the loader here; otherwise we wait for Room to deliver
                // the freshly written reports to avoid flashing the Empty state.
                if (result.loadedCount == 0) {
                    _isInitialLoading.value = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = ErrorMapper.toUserFriendlyMessage(e)
                _isInitialLoading.value = false
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMoreReports() {
        if (_isLoadingMore.value || !_hasMoreReports.value) return
        val userId = session.bind() ?: return

        _isLoadingMore.value = true
        _loadMoreError.value = null
        scope.launch {
            try {
                _hasMoreReports.value = repository.loadMoreHomeReports(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                // Keep hasMoreReports untouched so the retry action can re-run
                // the same page instead of silently ending the list.
                _loadMoreError.value =
                    "Не удалось загрузить ещё отчёты: ${ErrorMapper.toUserFriendlyMessage(e)}"
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun setSortOrder(order: ReportSortOrder) {
        _reportSortOrder.value = order
        session.currentUserId?.let { userPreferencesRepository.setSortOrder(it, order) }
    }

    fun setDisplayMode(mode: ReportDisplayMode) {
        _reportDisplayMode.value = mode
        session.currentUserId?.let { userPreferencesRepository.setDisplayMode(it, mode) }
    }

    fun clear() {
        reportsLoadJob?.cancel()
        _reports.value = emptyList()
        _error.value = null
        _isInitialLoading.value = true
        _isRefreshing.value = false
        _isLoadingMore.value = false
        _loadMoreError.value = null
        _hasMoreReports.value = false
        _reportDisplayMode.value = ReportDisplayMode.CARD
        homePhotosVersion++
    }
}
