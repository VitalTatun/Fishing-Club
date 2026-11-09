package com.example.fishing.data.report

import androidx.room.withTransaction
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.HomeRefreshResult
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.model.FishingReport
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first home feed: Room is the source of truth for the UI, network
 * refreshes replace or append pages. The paging cursor lives here because both
 * the refresh and the "load more" call share it.
 */
interface HomeFeedStore {
    fun getHomeReports(userId: UUID): Flow<List<FishingReport>>

    /** Loads the first page of home reports, replacing the cache. */
    suspend fun refreshHomeReports(userId: UUID): HomeRefreshResult

    /** Appends the next page of home reports. Returns whether more pages exist. */
    suspend fun loadMoreHomeReports(userId: UUID): Boolean
}

@Singleton
class RoomHomeFeedStore @Inject constructor(
    private val reportRemoteQueries: ReportRemoteQueries,
    private val likesStore: LikesStore,
    private val reportDetailsDao: ReportDetailsDao,
    private val database: AppDatabase,
    private val authRepository: AuthRepository
) : HomeFeedStore {

    // `loadedOwnCount` counts only the current user's own reports already cached
    // (bookmarks are always loaded whole).
    private var loadedOwnCount = 0
    private var hasMore = false

    override fun getHomeReports(userId: UUID): Flow<List<FishingReport>> =
        reportDetailsDao.getHomeReports(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun refreshHomeReports(userId: UUID): HomeRefreshResult {
        try {
            val likesSnapshotSeq = likesStore.currentMutationVersion()
            val page = reportRemoteQueries.fetchOwnReportPage(userId, offset = 0, limit = PAGE_SIZE)

            database.withTransaction {
                val entities = likesStore.preserveRecentLikes(
                    page.reports.map { it.toEntity() },
                    likesSnapshotSeq
                )
                reportDetailsDao.deleteAll()
                reportDetailsDao.insertAll(entities)
            }
            loadedOwnCount = page.reports.size
            hasMore = page.hasMore
            return HomeRefreshResult(
                hasMore = page.hasMore,
                loadedCount = page.reports.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun loadMoreHomeReports(userId: UUID): Boolean {
        if (!hasMore) return false
        val likesSnapshotSeq = likesStore.currentMutationVersion()
        val page = reportRemoteQueries.fetchOwnReportPage(
            userId,
            offset = loadedOwnCount,
            limit = PAGE_SIZE
        )
        database.withTransaction {
            reportDetailsDao.insertAll(
                likesStore.preserveRecentLikes(
                    page.reports.map { it.toEntity() },
                    likesSnapshotSeq
                )
            )
        }
        loadedOwnCount += page.reports.size
        hasMore = page.hasMore
        return page.hasMore
    }

    private fun RemoteReport.toEntity() =
        toReportDetailsEntity(authRepository::resolveImageUrl)

    private companion object {
        const val PAGE_SIZE = 20
    }
}
