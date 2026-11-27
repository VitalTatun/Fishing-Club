package com.example.fishing.data.report

import androidx.room.withTransaction
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.model.FishingReport
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Outcome of the authoritative one-report refresh. Room's asynchronous emission
 * cannot tell "not cached yet" from "removed on the server", so the network
 * answer is explicit and never conflated with an error.
 */
enum class ReportRefreshResult {
    /** The server returned the report and the cache now mirrors it. */
    Found,

    /**
     * The server answered and this viewer can no longer see the report: it was
     * deleted or made private. The local copies were dropped.
     */
    Missing,

    /** There is no session, so the server was not asked and the cache is intact. */
    NoSession
}

/**
 * Single report read path. The screen renders cached data immediately and asks
 * for a network refresh; [ReportRefreshResult] answers "does the report still
 * exist", which Room's asynchronous emission cannot decide on its own.
 */
interface ReportDetailsStore {
    fun getReportDetails(id: UUID): Flow<FishingReport?>

    /**
     * Refreshes one report into Room. When the server authoritatively reports it
     * missing, the cached report row is deleted so a private or deleted report
     * cannot be resurrected by reopening the screen.
     */
    suspend fun refreshReportDetails(id: UUID): ReportRefreshResult
}

@Singleton
class RoomReportDetailsStore @Inject constructor(
    private val reportRemoteQueries: ReportRemoteQueries,
    private val likesStore: LikesStore,
    private val reportDetailsDao: ReportDetailsDao,
    private val database: AppDatabase,
    private val authRepository: AuthRepository
) : ReportDetailsStore {

    override fun getReportDetails(id: UUID): Flow<FishingReport?> =
        reportDetailsDao.getById(id).map { entity -> entity?.toDomain() }

    override suspend fun refreshReportDetails(id: UUID): ReportRefreshResult {
        // Without a session the answer is not authoritative, so cached data stays.
        if (authRepository.currentUser() == null) return ReportRefreshResult.NoSession
        val likesSnapshotSeq = likesStore.currentMutationVersion()

        val remote = reportRemoteQueries.fetchReport(id) ?: run {
            // The server was asked and no longer shows the report to this
            // viewer. Drop every local copy: otherwise Room keeps serving the
            // stale row and the map keeps showing a now-private marker.
            database.withTransaction {
                reportDetailsDao.deleteById(id)
            }
            return ReportRefreshResult.Missing
        }
        val entity = remote.toReportDetailsEntity(authRepository::resolveImageUrl)
        database.withTransaction {
            reportDetailsDao.insert(
                likesStore.preserveRecentLikes(listOf(entity), likesSnapshotSeq).single()
            )
        }
        return ReportRefreshResult.Found
    }
}
