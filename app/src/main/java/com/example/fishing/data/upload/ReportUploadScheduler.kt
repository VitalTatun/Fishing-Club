package com.example.fishing.data.upload

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules the actual upload work. Kept behind an interface so the queue and
 * ViewModel can be tested without a real WorkManager. */
interface ReportUploadScheduler {
    fun enqueue(reportId: UUID)
    fun cancel(reportId: UUID)
    fun cancelAll()
}

@Singleton
class WorkManagerReportUploadScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ReportUploadScheduler {

    override fun enqueue(reportId: UUID) {
        val request = OneTimeWorkRequestBuilder<ReportUploadWorker>()
            .setInputData(
                workDataOf(ReportUploadWorker.KEY_PENDING_REPORT_ID to reportId.toString())
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(reportId),
            // Keep an in-flight worker; a repeated save must not create a second
            // upload for the same report.
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun cancel(reportId: UUID) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(reportId))
    }

    override fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }

    private fun uniqueName(reportId: UUID) = "report-upload-$reportId"

    companion object {
        const val TAG = "report-upload"
        private const val BACKOFF_SECONDS = 30L
    }
}
