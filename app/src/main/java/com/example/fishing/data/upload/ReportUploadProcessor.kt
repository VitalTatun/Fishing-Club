package com.example.fishing.data.upload

import android.content.Context
import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.IoDispatcher
import com.example.fishing.data.local.dao.PendingReportUploadDao
import com.example.fishing.model.ReportUploadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/** How many worker attempts a report gets before the user must retry manually. */
const val MAX_UPLOAD_ATTEMPTS = 3

sealed interface ReportUploadOutcome {
    data class Success(val isPublic: Boolean) : ReportUploadOutcome
    data class Retry(val message: String) : ReportUploadOutcome
    data class Failed(val message: String) : ReportUploadOutcome
    data object Missing : ReportUploadOutcome
}

/**
 * Does one upload attempt. Kept separate from [ReportUploadWorker] so retry,
 * progress and cleanup logic can be tested without WorkManager.
 */
class ReportUploadProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: FishingRepository,
    private val dao: PendingReportUploadDao,
    private val codec: PendingReportPayloadCodec,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun process(reportId: UUID): ReportUploadOutcome = withContext(ioDispatcher) {
        val row = dao.getById(reportId) ?: return@withContext ReportUploadOutcome.Missing
        // A failed row is only run again after an explicit retry.
        if (row.state == ReportUploadState.FAILED.name) {
            return@withContext ReportUploadOutcome.Missing
        }

        val attempt = row.attempts + 1
        dao.markUploading(reportId, attempt)

        val report = try {
            codec.decode(row.reportJson)
        } catch (e: Exception) {
            val message = ErrorMapper.toUserFriendlyMessage(e)
            dao.markFailed(reportId, message)
            return@withContext ReportUploadOutcome.Failed(message)
        }

        val result = try {
            repository.saveReport(report) { uploaded, total ->
                dao.updateProgress(reportId, uploaded, total)
            }
        } catch (e: CancellationException) {
            // The worker was stopped (process death / explicit cancel). Keep the
            // row so startup can resume it; explicit cancel deletes it itself.
            withContext(NonCancellable) {
                runCatching { dao.markWaiting(reportId, attempt, row.lastError) }
            }
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

        val error = result.exceptionOrNull()
        if (error == null) {
            dao.deleteById(reportId)
            LocalPhotoFiles.delete(context, codec.decodePaths(row.localPhotoPathsJson))
            ReportUploadOutcome.Success(isPublic = row.isPublic)
        } else {
            val message = ErrorMapper.toUserFriendlyMessage(error)
            if (attempt >= MAX_UPLOAD_ATTEMPTS) {
                dao.markFailed(reportId, message)
                ReportUploadOutcome.Failed(message)
            } else {
                dao.markWaiting(reportId, attempt, message)
                ReportUploadOutcome.Retry(message)
            }
        }
    }
}
