package com.example.fishing.viewmodel.likes

import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.FishingReport
import com.example.fishing.model.ReportLikeState
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
 * Likes scenario: Room-confirmed states plus an optimistic overlay that stores
 * intent only, so a newer authoritative count is never masked by a stale value.
 */
class LikesCoordinator(
    private val repository: FishingRepository,
    private val session: UserSession,
    private val scope: CoroutineScope
) {
    private val _likeStates = MutableStateFlow<Map<UUID, ReportLikeState>>(emptyMap())
    val likeStates: StateFlow<Map<UUID, ReportLikeState>> = _likeStates.asStateFlow()

    private val _likeError = MutableStateFlow<String?>(null)
    val likeError: StateFlow<String?> = _likeError.asStateFlow()

    private var likesLoadJob: Job? = null
    private val likeOpsInFlight = mutableSetOf<UUID>()

    private data class LikeOverlay(val liked: Boolean, val fallbackCount: Int)

    private val optimisticLikeOverlay = mutableMapOf<UUID, LikeOverlay>()
    private var baseLikeStates: Map<UUID, ReportLikeState> = emptyMap()

    fun refresh() {
        val userId = session.bind() ?: return

        // Cancel + relaunch (same as loadReports(force = true)): the collect
        // child below never completes, so an isActive guard would block every
        // later refresh, including pull-to-refresh. In-flight toggles survive
        // because they run in the scope, not in this job.
        likesLoadJob?.cancel()
        likesLoadJob = scope.launch {
            // Observe Room-confirmed states; network refresh runs concurrently
            // and its result lands in Room → the flow below re-emits.
            launch {
                try {
                    repository.getLikeStates(userId).collect { base ->
                        // Stale emission after logout/account switch must not restore state.
                        if (session.currentUserId != userId) return@collect
                        baseLikeStates = base
                        _likeStates.value = applyLikeOverlay(base)
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
        val userId = session.resolve()
        if (userId == null) {
            _likeError.value = "Нет активной сессии"
            return
        }
        // Double-tap guard: no queue, the second tap is ignored while in flight.
        if (!likeOpsInFlight.add(report.id)) return

        _likeError.value = null
        val current = _likeStates.value[report.id]
            ?: baseLikeStates[report.id]
            ?: ReportLikeState(report.id)
        val liking = !current.isLiked
        // The overlay stores intent only; the displayed count is derived from
        // the latest Room-confirmed count so a newer authoritative snapshot is
        // never masked by a stale absolute value.
        optimisticLikeOverlay[report.id] = LikeOverlay(liking, current.likesCount)
        _likeStates.value = applyLikeOverlay(baseLikeStates)

        scope.launch {
            try {
                val result = if (liking) {
                    repository.addLike(report.id)
                } else {
                    repository.removeLike(report.id)
                }
                // Late results after logout/account switch must not restore state.
                if (session.currentUserId != userId) return@launch
                result.onFailure { e ->
                    optimisticLikeOverlay.remove(report.id)
                    _likeStates.value = applyLikeOverlay(baseLikeStates)
                    _likeError.value = likeErrorMessage(e)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.currentUserId != userId) return@launch
                optimisticLikeOverlay.remove(report.id)
                _likeStates.value = applyLikeOverlay(baseLikeStates)
                _likeError.value = likeErrorMessage(e)
            } finally {
                likeOpsInFlight.remove(report.id)
                _likeStates.value = applyLikeOverlay(baseLikeStates)
            }
        }
    }

    private fun applyLikeOverlay(
        base: Map<UUID, ReportLikeState>
    ): Map<UUID, ReportLikeState> {
        if (optimisticLikeOverlay.isEmpty()) return base
        val result = base.toMutableMap()
        val confirmed = mutableListOf<UUID>()
        optimisticLikeOverlay.forEach { (id, overlay) ->
            val current = base[id]
            when {
                current == null -> {
                    result[id] = ReportLikeState(
                        reportId = id,
                        isLiked = overlay.liked,
                        likesCount = (overlay.fallbackCount + if (overlay.liked) 1 else -1)
                            .coerceAtLeast(0)
                    )
                }
                current.isLiked == overlay.liked -> confirmed += id
                else -> {
                    result[id] = current.copy(
                        isLiked = overlay.liked,
                        likesCount = (current.likesCount + if (overlay.liked) 1 else -1)
                            .coerceAtLeast(0)
                    )
                }
            }
        }
        confirmed.forEach(optimisticLikeOverlay::remove)
        return result
    }

    fun clearLikeError() {
        _likeError.value = null
    }

    fun clear() {
        likesLoadJob?.cancel()
        _likeStates.value = emptyMap()
        baseLikeStates = emptyMap()
        optimisticLikeOverlay.clear()
        likeOpsInFlight.clear()
        _likeError.value = null
    }

    private fun likeErrorMessage(e: Throwable): String {
        return "Не удалось изменить лайк: ${ErrorMapper.toUserFriendlyMessage(e)}"
    }
}
