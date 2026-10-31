package com.example.fishing.testutil

import com.example.fishing.data.upload.ReportUploadQueue
import com.example.fishing.data.upload.ReportUploadResult
import com.example.fishing.model.FishingReport
import com.example.fishing.model.PendingReportUpload
import com.example.fishing.model.ReportUploadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID

/** In-memory [ReportUploadQueue] with hooks for driving upload progress in tests. */
class FakeReportUploadQueue : ReportUploadQueue {

    val pending = MutableStateFlow<List<PendingReportUpload>>(emptyList())
    private val results = MutableSharedFlow<ReportUploadResult>(extraBufferCapacity = 16)

    val enqueuedReports = mutableListOf<FishingReport>()
    val enqueuedLocalPaths = mutableListOf<List<String>>()
    val cancelledReportIds = mutableListOf<UUID>()
    val retriedReportIds = mutableListOf<UUID>()

    var enqueueResult: Result<Unit> = Result.success(Unit)
    var clearAllCount = 0
    var resumePendingCount = 0
    var referencedPaths: Set<String> = emptySet()

    /** Runs inside [enqueue] so tests can seed the row the ViewModel will observe. */
    var onEnqueue: ((FishingReport) -> Unit)? = null

    override fun observePending(): Flow<List<PendingReportUpload>> = pending

    override fun observeResults(): SharedFlow<ReportUploadResult> = results

    override suspend fun enqueue(
        report: FishingReport,
        localPhotoPaths: List<String>,
        temporaryPhotoPaths: List<String>
    ): Result<Unit> {
        enqueuedReports += report
        enqueuedLocalPaths += localPhotoPaths
        onEnqueue?.invoke(report)
        return enqueueResult
    }

    override suspend fun cancel(reportId: UUID) {
        cancelledReportIds += reportId
    }

    override suspend fun retry(reportId: UUID) {
        retriedReportIds += reportId
    }

    override suspend fun resumePending() {
        resumePendingCount++
    }

    override suspend fun cleanupStale(nowMillis: Long) = Unit

    override suspend fun clearAll() {
        clearAllCount++
    }

    override suspend fun referencedLocalPaths(): Set<String> = referencedPaths

    fun emitResult(result: ReportUploadResult) {
        results.tryEmit(result)
    }

    fun setPendingRow(
        reportId: UUID,
        state: ReportUploadState = ReportUploadState.UPLOADING,
        uploadedPhotos: Int = 0,
        totalPhotos: Int = 1,
        attempts: Int = 1,
        lastError: String? = null,
        isPublic: Boolean = true
    ) {
        pending.value = listOf(
            PendingReportUpload(
                reportId = reportId,
                isPublic = isPublic,
                state = state,
                attempts = attempts,
                uploadedPhotos = uploadedPhotos,
                totalPhotos = totalPhotos,
                lastError = lastError,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}
