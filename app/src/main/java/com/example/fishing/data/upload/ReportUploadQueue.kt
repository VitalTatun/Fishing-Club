package com.example.fishing.data.upload

import android.content.Context
import com.example.fishing.data.local.dao.PendingReportUploadDao
import com.example.fishing.data.local.entity.PendingReportUploadEntity
import com.example.fishing.model.FishingReport
import com.example.fishing.model.PendingReportUpload
import com.example.fishing.model.ReportUploadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.example.fishing.data.IoDispatcher
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent queue of reports waiting for upload. A row is written before the
 * worker is scheduled, so killing the process cannot lose an accepted report.
 */
interface ReportUploadQueue {
    fun observePending(): Flow<List<PendingReportUpload>>
    fun observeResults(): SharedFlow<ReportUploadResult>

    suspend fun enqueue(
        report: FishingReport,
        localPhotoPaths: List<String>,
        temporaryPhotoPaths: List<String>
    ): Result<Unit>

    /** Cancels the upload, removes the queue row and its temporary photo copies. */
    suspend fun cancel(reportId: UUID)

    /** Re-queues a permanently failed upload after an explicit user request. */
    suspend fun retry(reportId: UUID)

    /** Re-schedules unfinished rows and fails rows that exhausted their retries. */
    suspend fun resumePending()

    /** Deletes rows (and their files) older than the retention period. */
    suspend fun cleanupStale(nowMillis: Long = System.currentTimeMillis())

    /** Cancels every upload and deletes all queued data (used on logout). */
    suspend fun clearAll()

    /** Local photo paths still referenced by the queue; safe to keep on startup. */
    suspend fun referencedLocalPaths(): Set<String>
}

@Singleton
class RoomReportUploadQueue @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: PendingReportUploadDao,
    private val scheduler: ReportUploadScheduler,
    private val codec: PendingReportPayloadCodec,
    private val notifier: ReportUploadNotifier,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ReportUploadQueue {

    override fun observePending(): Flow<List<PendingReportUpload>> =
        dao.observeAll().map { rows -> rows.map(PendingReportUploadEntity::toDomain) }

    override fun observeResults(): SharedFlow<ReportUploadResult> = notifier.results

    override suspend fun enqueue(
        report: FishingReport,
        localPhotoPaths: List<String>,
        temporaryPhotoPaths: List<String>
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val entity = PendingReportUploadEntity(
                id = report.id,
                reportJson = codec.encode(report),
                localPhotoPathsJson = codec.encodePaths(localPhotoPaths),
                temporaryPhotoPathsJson = codec.encodePaths(temporaryPhotoPaths),
                isPublic = report.isPublic,
                createdAt = System.currentTimeMillis(),
                attempts = 0,
                state = ReportUploadState.QUEUED.name,
                uploadedPhotos = 0,
                totalPhotos = report.photos.size,
                lastError = null
            )
            dao.upsert(entity)
            scheduler.enqueue(report.id)
        }
    }

    override suspend fun cancel(reportId: UUID) = withContext(ioDispatcher) {
        scheduler.cancel(reportId)
        val row = dao.getById(reportId)
        if (row != null) {
            dao.deleteById(reportId)
            // Only processed copies are deleted here: camera originals are still
            // referenced by the open form and must survive a cancel.
            LocalPhotoFiles.delete(context, codec.decodePaths(row.temporaryPhotoPathsJson))
        }
    }

    override suspend fun retry(reportId: UUID) = withContext(ioDispatcher) {
        val row = dao.getById(reportId) ?: return@withContext
        if (row.state == ReportUploadState.FAILED.name) {
            dao.resetForRetry(reportId)
        }
        scheduler.enqueue(reportId)
    }

    override suspend fun resumePending() = withContext(ioDispatcher) {
        dao.getAll().forEach { row ->
            when (row.state) {
                ReportUploadState.FAILED.name -> Unit
                else -> {
                    if (row.attempts >= MAX_UPLOAD_ATTEMPTS) {
                        dao.markFailed(row.id, row.lastError)
                    } else {
                        // A process killed mid-upload leaves UPLOADING behind.
                        dao.markWaiting(
                            id = row.id,
                            attempts = row.attempts,
                            error = row.lastError
                        )
                        scheduler.enqueue(row.id)
                    }
                }
            }
        }
    }

    override suspend fun cleanupStale(nowMillis: Long) = withContext(ioDispatcher) {
        val cutoff = nowMillis - STALE_AFTER_MILLIS
        dao.getOlderThan(cutoff).forEach { row ->
            scheduler.cancel(row.id)
            dao.deleteById(row.id)
            LocalPhotoFiles.delete(context, codec.decodePaths(row.localPhotoPathsJson))
        }
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        scheduler.cancelAll()
        val rows = dao.getAll()
        dao.deleteAll()
        rows.forEach { row ->
            LocalPhotoFiles.delete(context, codec.decodePaths(row.localPhotoPathsJson))
        }
    }

    override suspend fun referencedLocalPaths(): Set<String> = withContext(ioDispatcher) {
        dao.getAll().flatMap { row ->
            codec.decodePaths(row.localPhotoPathsJson)
        }.toSet()
    }

    companion object {
        val STALE_AFTER_MILLIS: Long = TimeUnit.DAYS.toMillis(7)
    }
}
