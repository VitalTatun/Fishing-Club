package com.example.fishing.viewmodel.favorites

import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.FishingReport
import com.example.fishing.viewmodel.photos.PhotoUrlResolver
import com.example.fishing.viewmodel.session.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Favorites scenario: optimistic bookmark toggles on top of the Room-confirmed
 * id set, plus the separately loaded list used by the search and map filters.
 */
class FavoritesCoordinator(
    private val repository: FishingRepository,
    private val session: UserSession,
    private val photoUrls: PhotoUrlResolver,
    private val scope: CoroutineScope
) {
    private val _favoriteIds = MutableStateFlow<Set<UUID>>(emptySet())
    val favoriteIds: StateFlow<Set<UUID>> = _favoriteIds.asStateFlow()

    private val _favoriteOpsInFlight = MutableStateFlow<Set<UUID>>(emptySet())
    val favoriteOpsInFlight: StateFlow<Set<UUID>> = _favoriteOpsInFlight.asStateFlow()

    private val _filterFavoriteReports = MutableStateFlow<List<FishingReport>>(emptyList())
    val filterFavoriteReports: StateFlow<List<FishingReport>> = _filterFavoriteReports.asStateFlow()

    private val _favoriteError = MutableStateFlow<String?>(null)
    val favoriteError: StateFlow<String?> = _favoriteError.asStateFlow()

    private var filterFavoritesLoadedForUserId: UUID? = null
    private var favoritesLoadJob: Job? = null
    private var favoritePhotosVersion = 0

    // Toggles are applied optimistically on top of the Room-confirmed set and
    // rolled back on failure, mirroring the likes implementation.
    private val optimisticFavoriteOverlay = mutableMapOf<UUID, Boolean>()
    private var baseFavoriteIds: Set<UUID> = emptySet()

    fun loadFilterFavoriteReports(force: Boolean = false) {
        val userId = session.resolve() ?: return
        if (!force && filterFavoritesLoadedForUserId == userId) {
            return
        }
        scope.launch {
            try {
                val reports = repository.loadFavoriteReports(userId)
                val cached = photoUrls.applyCachedPhotoUrls(reports)
                _filterFavoriteReports.value = cached
                val version = ++favoritePhotosVersion
                val resolved = photoUrls.resolveReportsPhotoUrls(reports)
                if (version == favoritePhotosVersion) {
                    _filterFavoriteReports.value = resolved
                }
                filterFavoritesLoadedForUserId = userId
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite(report: FishingReport) {
        val userId = session.resolve()
        if (userId == null) {
            _favoriteError.value = "Нет активной сессии"
            return
        }
        // Double-tap guard: no queue, the second tap is ignored while in flight.
        if (report.id in _favoriteOpsInFlight.value) return
        _favoriteOpsInFlight.value = _favoriteOpsInFlight.value + report.id

        _favoriteError.value = null
        val adding = report.id !in _favoriteIds.value
        optimisticFavoriteOverlay[report.id] = adding
        _favoriteIds.value = applyFavoriteOverlay(baseFavoriteIds)

        scope.launch {
            try {
                val result = if (adding) {
                    repository.addFavorite(report)
                } else {
                    repository.removeFavorite(report.id)
                }
                // Late results after logout/account switch must not restore state.
                if (session.currentUserId != userId) return@launch
                result.onFailure { e ->
                    optimisticFavoriteOverlay.remove(report.id)
                    _favoriteIds.value = applyFavoriteOverlay(baseFavoriteIds)
                    _favoriteError.value = favoriteErrorMessage(e)
                }
                if (result.isSuccess && filterFavoritesLoadedForUserId != null) {
                    loadFilterFavoriteReports(force = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.currentUserId == userId) {
                    optimisticFavoriteOverlay.remove(report.id)
                    _favoriteIds.value = applyFavoriteOverlay(baseFavoriteIds)
                    _favoriteError.value = favoriteErrorMessage(e)
                }
            } finally {
                _favoriteOpsInFlight.value = _favoriteOpsInFlight.value - report.id
                _favoriteIds.value = applyFavoriteOverlay(baseFavoriteIds)
            }
        }
    }

    private fun applyFavoriteOverlay(base: Set<UUID>): Set<UUID> {
        if (optimisticFavoriteOverlay.isEmpty()) return base
        val result = base.toMutableSet()
        val confirmed = mutableListOf<UUID>()
        optimisticFavoriteOverlay.forEach { (id, favorite) ->
            if ((id in base) == favorite) {
                confirmed += id
            } else if (favorite) {
                result += id
            } else {
                result -= id
            }
        }
        confirmed.forEach(optimisticFavoriteOverlay::remove)
        return result
    }

    /**
     * Observes the local favorites cache and refreshes it from the backend.
     * Runs on every authenticated session, so the detail screen knows the
     * bookmark state even when the user never opened the favorites filter.
     */
    fun refresh() {
        val userId = session.bind() ?: return

        // Cancel + relaunch: the collect child below never completes.
        favoritesLoadJob?.cancel()
        favoritesLoadJob = scope.launch {
            launch {
                try {
                    repository.getFavoriteReportIds(userId).collect { base ->
                        if (session.currentUserId != userId) return@collect
                        baseFavoriteIds = base
                        _favoriteIds.value = applyFavoriteOverlay(base)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // A failed refresh keeps the cached ids untouched.
            try {
                repository.refreshFavoriteIds(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearFavoriteError() {
        _favoriteError.value = null
    }

    fun clear() {
        favoritesLoadJob?.cancel()
        _favoriteIds.value = emptySet()
        baseFavoriteIds = emptySet()
        optimisticFavoriteOverlay.clear()
        _favoriteOpsInFlight.value = emptySet()
        _filterFavoriteReports.value = emptyList()
        filterFavoritesLoadedForUserId = null
        _favoriteError.value = null
        favoritePhotosVersion++
    }

    private fun favoriteErrorMessage(e: Throwable): String {
        return "Не удалось изменить избранное: ${ErrorMapper.toUserFriendlyMessage(e)}"
    }
}
