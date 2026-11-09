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
 * Single report read path. The screen renders cached data immediately and asks
 * for a network refresh; the boolean answers "does the report still exist",
 * which Room's asynchronous emission cannot decide on its own.
 */
interface ReportDetailsStore {
    fun getReportDetails(id: UUID): Flow<FishingReport?>

    /** Refreshes one report into Room. `false` when there is no session or no report. */
    suspend fun refreshReportDetails(id: UUID): Boolean
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

    override suspend fun refreshReportDetails(id: UUID): Boolean {
        if (authRepository.currentUser() == null) return false
        val likesSnapshotSeq = likesStore.currentMutationVersion()

        val remote = reportRemoteQueries.fetchReport(id) ?: return false
        val entity = remote.toReportDetailsEntity(authRepository::resolveImageUrl)
        database.withTransaction {
            reportDetailsDao.insert(
                likesStore.preserveRecentLikes(listOf(entity), likesSnapshotSeq).single()
            )
        }
        return true
    }
}
