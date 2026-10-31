package com.example.fishing.data.upload

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.UUID
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReportUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val processor: ReportUploadProcessor,
    private val notifier: ReportUploadNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reportId = inputData.getString(KEY_PENDING_REPORT_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return Result.failure()

        return when (val outcome = processor.process(reportId)) {
            is ReportUploadOutcome.Success -> {
                notifier.notify(
                    ReportUploadResult(
                        reportId = reportId,
                        success = true,
                        isPublic = outcome.isPublic,
                        message = null
                    )
                )
                Result.success()
            }
            is ReportUploadOutcome.Retry -> Result.retry()
            is ReportUploadOutcome.Failed -> {
                notifier.notify(
                    ReportUploadResult(
                        reportId = reportId,
                        success = false,
                        isPublic = false,
                        message = outcome.message
                    )
                )
                Result.failure()
            }
            ReportUploadOutcome.Missing -> Result.success()
        }
    }

    companion object {
        const val KEY_PENDING_REPORT_ID = "pending_report_id"
    }
}
