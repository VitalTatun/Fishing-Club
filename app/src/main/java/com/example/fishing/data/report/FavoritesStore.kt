package com.example.fishing.data.report

import androidx.room.withTransaction
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.FavoriteReportDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.entity.FavoriteReportEntity
import com.example.fishing.data.supabase.FavoriteDto
import com.example.fishing.model.FishingReport
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

/**
 * Bookmarks are cached in Room so the UI can render the correct state
 * immediately and survive a failed network call. The server snapshot is
 * versioned so a refresh that started before a local toggle cannot drop it.
 */
interface FavoritesStore {
    fun getFavoriteReportIds(userId: UUID): Flow<Set<UUID>>

    /** Loads bookmarked reports and refreshes the cached id set. */
    suspend fun loadFavoriteReports(userId: UUID): List<FishingReport>

    /** Pulls bookmarked ids from the backend into the local cache. */
    suspend fun refreshFavoriteIds(userId: UUID)

    suspend fun addFavorite(reportId: UUID): Result<Unit>

    suspend fun removeFavorite(reportId: UUID): Result<Unit>

    /** Version to capture before fetching server links. */
    fun currentMutationVersion(): Long

    /**
     * Writes a server snapshot of bookmarked ids into Room, keeping ids toggled
     * locally after [snapshotVersion] was taken.
     */
    suspend fun persistServerLinks(userId: UUID, reportIds: List<UUID>, snapshotVersion: Long)
}

@Singleton
class SupabaseFavoritesStore @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val database: AppDatabase,
    private val reportDetailsDao: ReportDetailsDao,
    private val favoriteReportDao: FavoriteReportDao,
    private val reportRemoteQueries: ReportRemoteQueries
) : FavoritesStore {

    // A refresh that snapshotted the server before a local toggle must not drop
    // that toggle from the local cache.
    private val mutationSeq = AtomicLong(0)
    private val mutationVersions = ConcurrentHashMap<UUID, Long>()

    override fun getFavoriteReportIds(userId: UUID): Flow<Set<UUID>> =
        favoriteReportDao.getFavoriteIds(userId)
            .map { it.toSet() }
            .distinctUntilChanged()

    override suspend fun loadFavoriteReports(userId: UUID): List<FishingReport> {
        val snapshotSeq = currentMutationVersion()
        val ids = reportRemoteQueries.fetchFavoriteReportIds(userId)
        persistServerLinks(userId, ids, snapshotSeq)
        if (ids.isEmpty()) return emptyList()
        return reportRemoteQueries.fetchReportsByIds(ids).map { remote ->
            remote.toReportDetailsEntity(authRepository::resolveImageUrl).toDomain()
        }
    }

    override suspend fun refreshFavoriteIds(userId: UUID) {
        try {
            val snapshotSeq = currentMutationVersion()
            val ids = reportRemoteQueries.fetchFavoriteReportIds(userId)
            persistServerLinks(userId, ids, snapshotSeq)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun addFavorite(reportId: UUID): Result<Unit> {
        val currentUser = authRepository.currentUser()
            ?: return Result.failure(IllegalStateException("Нет активной сессии"))
        return try {
            supabase.postgrest["favorites"].insert(
                FavoriteDto(userId = currentUser.id, fishingId = reportId)
            )
            mutationVersions[reportId] = mutationSeq.incrementAndGet()
            favoriteReportDao.insertAll(
                listOf(FavoriteReportEntity(currentUser.id, reportId))
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(reportId: UUID): Result<Unit> {
        val currentUser = authRepository.currentUser()
            ?: return Result.failure(IllegalStateException("Нет активной сессии"))
        return try {
            supabase.postgrest["favorites"].delete {
                filter { eq("user_id", currentUser.id) }
                filter { eq("fishing_id", reportId) }
            }
            val cachedReport = reportDetailsDao.getByIdOneShot(reportId)
            if (cachedReport != null && cachedReport.userId != currentUser.id) {
                reportDetailsDao.deleteById(reportId)
            }
            mutationVersions[reportId] = mutationSeq.incrementAndGet()
            favoriteReportDao.delete(currentUser.id, reportId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun currentMutationVersion(): Long = mutationSeq.get()

    override suspend fun persistServerLinks(
        userId: UUID,
        reportIds: List<UUID>,
        snapshotVersion: Long
    ) {
        val links = reportIds.map { FavoriteReportEntity(userId = userId, reportId = it) }
        val protectedIds = buildSet {
            links.forEach { add(it.reportId) }
            mutationVersions.forEach { (id, version) ->
                if (version > snapshotVersion) add(id)
            }
        }
        database.withTransaction {
            favoriteReportDao.insertAll(links)
            if (protectedIds.isEmpty()) {
                favoriteReportDao.deleteAllForUser(userId)
            } else {
                favoriteReportDao.deleteNotIn(userId, protectedIds.toList())
            }
        }
    }
}
