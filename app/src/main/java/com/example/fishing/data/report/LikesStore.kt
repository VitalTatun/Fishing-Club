package com.example.fishing.data.report

import androidx.room.withTransaction
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.LikeDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.entity.LikeEntity
import com.example.fishing.data.local.entity.ReportDetailsEntity
import com.example.fishing.data.supabase.LikeStateDto
import com.example.fishing.model.ReportLikeState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Likes live in two places: the membership table in Room (who liked what) and
 * the counter mirrored on each cached report. Both are refreshed from the
 * server, which races with user taps — this component owns the versioning that
 * stops a stale refresh from undoing a newer local mutation.
 */
interface LikesStore {
    fun getLikeStates(userId: UUID): Flow<Map<UUID, ReportLikeState>>

    /** Pulls the user's liked report ids into Room. */
    suspend fun refreshLikes(userId: UUID)

    suspend fun addLike(reportId: UUID): Result<Unit>

    suspend fun removeLike(reportId: UUID): Result<Unit>

    /**
     * Version to capture before fetching report data. Pass it to
     * [preserveRecentLikes] so mutations that happened during the fetch win.
     */
    fun currentMutationVersion(): Long

    /**
     * Replaces counters of reports mutated after [snapshotVersion] (or with a
     * mutation still in flight) with the authoritative values from Room.
     */
    suspend fun preserveRecentLikes(
        entities: List<ReportDetailsEntity>,
        snapshotVersion: Long
    ): List<ReportDetailsEntity>
}

@Singleton
class SupabaseLikesStore @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val database: AppDatabase,
    private val reportDetailsDao: ReportDetailsDao,
    private val likeDao: LikeDao,
    private val reportRemoteQueries: ReportRemoteQueries
) : LikesStore {

    // Like mutations are versioned so a slow refresh that fetched likesCount
    // before the mutation cannot overwrite the authoritative value afterwards.
    private val mutationSeq = AtomicLong(0)
    private val mutationVersions = ConcurrentHashMap<UUID, Long>()
    private val opsInFlight = ConcurrentHashMap.newKeySet<UUID>()

    override fun getLikeStates(userId: UUID): Flow<Map<UUID, ReportLikeState>> =
        // Single atomic query so isLiked and likesCount always come from the
        // same snapshot. Combining two separate Room flows emitted an
        // intermediate (new isLiked, stale likesCount) that flickered the count.
        reportDetailsDao.getLikeStates(userId)
            .map { rows -> rows.associate { it.id to ReportLikeState(it.id, it.isLiked, it.likesCount) } }
            .distinctUntilChanged()

    override suspend fun refreshLikes(userId: UUID) {
        try {
            val snapshotSeq = mutationSeq.get()
            // Single batched query — never one request per report.
            val likedIds = reportRemoteQueries.fetchLikedReportIds(userId)
            val entities = likedIds.map { LikeEntity(userId = userId, reportId = it) }
            // Never drop a local like that was written by an operation which
            // started after this network snapshot was taken.
            val protectedIds = buildSet {
                entities.forEach { add(it.reportId) }
                addAll(opsInFlight)
                mutationVersions.forEach { (id, version) ->
                    if (version > snapshotSeq) add(id)
                }
            }
            database.withTransaction {
                likeDao.insertAll(entities)
                if (protectedIds.isEmpty()) {
                    likeDao.deleteAllForUser(userId)
                } else {
                    likeDao.deleteNotIn(userId, protectedIds.toList())
                }
            }
            // Counters are not counted here: they arrive as fishing.likes_count
            // inside the existing fishing selects (refreshHomeReports /
            // refreshReportDetails) mapped to ReportDetailsEntity.likesCount.
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun addLike(reportId: UUID): Result<Unit> {
        val currentUser = authRepository.currentUser()
            ?: return Result.failure(IllegalStateException("Нет активной сессии"))
        // Local pre-checks before the RPC (the RPC/RLS enforce authoritatively).
        val cached = try {
            reportDetailsDao.getByIdOneShot(reportId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (cached != null && cached.userId != currentUser.id && !cached.isPublic) {
            return Result.failure(IllegalStateException("Cannot like private report"))
        }
        return mutateLike(reportId, currentUser.id, "like_report")
    }

    override suspend fun removeLike(reportId: UUID): Result<Unit> {
        val currentUser = authRepository.currentUser()
            ?: return Result.failure(IllegalStateException("Нет активной сессии"))
        return mutateLike(reportId, currentUser.id, "unlike_report")
    }

    override fun currentMutationVersion(): Long = mutationSeq.get()

    override suspend fun preserveRecentLikes(
        entities: List<ReportDetailsEntity>,
        snapshotVersion: Long
    ): List<ReportDetailsEntity> {
        if (entities.isEmpty()) return entities
        val changedIds = entities.map { it.id }.filter { id ->
            opsInFlight.contains(id) || likeVersion(id) > snapshotVersion
        }
        if (changedIds.isEmpty()) return entities

        val existingCounts = reportDetailsDao.getLikesCounts(changedIds)
            .associate { it.id to it.likesCount }
        val changed = changedIds.toHashSet()
        return entities.map { entity ->
            if (entity.id in changed) {
                existingCounts[entity.id]?.let { entity.copy(likesCount = it) } ?: entity
            } else {
                entity
            }
        }
    }

    private suspend fun mutateLike(reportId: UUID, userId: UUID, rpcName: String): Result<Unit> {
        opsInFlight.add(reportId)
        return try {
            // The RPC returns the authoritative {liked, likes_count}. Writing that
            // value instead of a blind +1/-1 keeps Room in sync even when the RPC
            // is a no-op or the local membership cache was stale.
            val state = supabase.postgrest.rpc(
                rpcName,
                buildJsonObject { put("fid", reportId.toString()) }
            ).decodeList<LikeStateDto>().firstOrNull()
                ?: return Result.failure(IllegalStateException("Unexpected like response"))

            mutationVersions[reportId] = mutationSeq.incrementAndGet()
            database.withTransaction {
                if (state.liked) {
                    likeDao.insertAll(listOf(LikeEntity(userId, reportId)))
                } else {
                    likeDao.delete(userId, reportId)
                }
                reportDetailsDao.updateLikesCount(reportId, state.likesCount)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            opsInFlight.remove(reportId)
        }
    }

    private fun likeVersion(id: UUID): Long = mutationVersions[id] ?: 0L
}
