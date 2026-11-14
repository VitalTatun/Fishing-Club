package com.example.fishing.viewmodel.user

import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.report.UserProfileData
import com.example.fishing.model.FishingReport
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.viewmodel.UserProfileUiState
import com.example.fishing.viewmodel.photos.PhotoUrlResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Public user page opened from a report author. Reports are fetched fresh from
 * the server (they never enter the local cache) and their storage paths are
 * swapped for signed URLs the same way the detail screen does it.
 *
 * The page survives a round trip to a report: the same author is refreshed in
 * the background instead of being reloaded into a spinner.
 */
class UserProfileCoordinator(
    private val repository: FishingRepository,
    private val photoUrls: PhotoUrlResolver,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    /** Same two orders as the home screen, applied to the author's reports. */
    private val _sortOrder = MutableStateFlow(ReportSortOrder.BY_FISHING_TIME)
    val sortOrder: StateFlow<ReportSortOrder> = _sortOrder.asStateFlow()

    /**
     * `false` while photo urls are being signed. Re-sorting republishes the
     * loaded page, and doing that before signing finishes would render raw
     * storage paths — the very state where thumbnails fail to load.
     */
    private val _canChangeSortOrder = MutableStateFlow(true)
    val canChangeSortOrder: StateFlow<Boolean> = _canChangeSortOrder.asStateFlow()

    private var loaded: UserProfileData? = null
    private var loadedUserId: UUID? = null
    private var loadJob: Job? = null

    fun load(userId: UUID) {
        // Navigation back from a report recreates this destination and re-runs
        // its LaunchedEffect. If the same author is already on screen, refresh in
        // the background instead of dropping to Loading: the list, the sort order
        // and the scroll position survive the round trip.
        val isSameUserOnScreen =
            loadedUserId == userId && _uiState.value is UserProfileUiState.Success

        loadJob?.cancel()
        // Sorting is blocked from the moment a load starts until the photo urls
        // of the new page are signed.
        _canChangeSortOrder.value = false
        if (!isSameUserOnScreen) {
            loaded = null
            _uiState.value = UserProfileUiState.Loading
        }

        loadJob = scope.launch {
            try {
                val data = repository.loadUserProfile(userId).getOrElse { e ->
                    _canChangeSortOrder.value = true
                    // A failed background refresh must not replace a page the user
                    // is already reading with an error screen.
                    if (!isSameUserOnScreen) {
                        _uiState.value = errorState(
                            e as? Exception ?: IllegalStateException(e.message, e)
                        )
                    }
                    return@launch
                }
                loadedUserId = userId
                loaded = data
                // Publish before signing: the header and the list render at once,
                // thumbnails appear as soon as the URLs arrive.
                publish(data)

                val resolved = photoUrls.resolveReportsPhotoUrls(data.reports)
                if (loaded === data) {
                    // Keep the signed version as the source for later re-renders:
                    // changing the sort order republishes `loaded`, and raw storage
                    // paths would make every thumbnail fail to load.
                    val resolvedData = data.copy(reports = resolved)
                    loaded = resolvedData
                    _canChangeSortOrder.value = true
                    publish(resolvedData)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _canChangeSortOrder.value = true
                if (!isSameUserOnScreen) {
                    _uiState.value = errorState(e)
                }
            }
        }
    }

    fun setSortOrder(order: ReportSortOrder) {
        if (_sortOrder.value == order) return
        // Ignore taps that land while photo urls are still being signed.
        if (!_canChangeSortOrder.value) return
        _sortOrder.value = order
        loaded?.let(::publish)
    }

    fun clear() {
        loadJob?.cancel()
        loadJob = null
        loaded = null
        loadedUserId = null
        _sortOrder.value = ReportSortOrder.BY_FISHING_TIME
        _canChangeSortOrder.value = true
        _uiState.value = UserProfileUiState.Loading
    }

    private fun publish(data: UserProfileData) {
        _uiState.value = UserProfileUiState.Success(
            user = data.user,
            reports = sortedReports(data.reports),
            caughtCount = data.caughtCount,
            reportsCount = data.reportsCount
        )
    }

    private fun sortedReports(reports: List<FishingReport>): List<FishingReport> =
        when (_sortOrder.value) {
            // Mirrors the home feed so both screens order reports the same way.
            ReportSortOrder.BY_PUBLISH_DATE ->
                reports.sortedByDescending { it.createdAt ?: it.publishedAt }

            ReportSortOrder.BY_FISHING_TIME ->
                reports.sortedByDescending { it.fishingStartAt ?: it.createdAt?.toInstant() }
        }

    private fun errorState(e: Exception) = UserProfileUiState.Error(
        "Не удалось загрузить профиль: ${ErrorMapper.toUserFriendlyMessage(e)}"
    )
}
