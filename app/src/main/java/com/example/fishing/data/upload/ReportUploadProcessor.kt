package com.example.fishing.data.upload

import android.content.Context
import com.example.fishing.data.ErrorMapper
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.IoDispatcher
import com.example.fishing.data.local.dao.PendingReportUploadDao
import com.example.fishing.model.FishingReport
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

/** The server accepted the request, but did not confirm the end state. */
internal const val UNCONFIRMED_UPLOAD_MESSAGE =
    "Не удалось подтвердить сохранение отчёта и фотографий. Попробуйте ещё раз"

sealed interface ReportUploadOutcome {
    data class Success(val isPublic: Boolean) : ReportUploadOutcome
    data class Retry(val message: String) : ReportUploadOutcome
    data class Failed(val message: String) : ReportUploadOutcome
    data object Missing : ReportUploadOutcome
}

/**
 * Does one upload attempt. Kept separate from [ReportUploadWorker] so retry,
 * progress and cleanup logic can be tested without WorkManager.
 *
 * A report can be committed by the server even when the response never reaches
 * the client. Therefore a retry first asks the repository whether the server
 * already holds the report **and all of its photos**: only that complete state
 * may skip the upload, while a report with missing attachments still goes
 * through [FishingRepository.saveReport]. The local queue row is deleted only
 * after the same complete state has been confirmed.
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

        // Recovery path: an earlier attempt may have published the report and
        // lost the acknowledgement. Existence alone is not enough — every photo
        // must be attached, otherwise the missing ones still need uploading.
        if (row.attempts > 0 && isSynced(report, reportId, attempt, row.lastError)) {
            deleteQueueRow(reportId, row.localPhotoPathsJson)
            return@withContext ReportUploadOutcome.Success(isPublic = row.isPublic)
        }

        val result = try {
            repository.saveReport(report) { uploaded, total ->
                dao.updateProgress(reportId, uploaded, total)
            }
        } catch (e: CancellationException) {
            // The worker was stopped (process death / explicit cancel). Keep the
            // row so startup can resume it; explicit cancel deletes it itself.
            markWaitingOnCancel(reportId, attempt, row.lastError)
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

        val error = result.exceptionOrNull()
        if (error == null) {
            // The RPC response is only a transport confirmation. Do not drop the
            // payload until the server exposes the report and every expected
            // attachment; otherwise a lost response would lose photos locally.
            if (!isSynced(report, reportId, attempt, row.lastError)) {
                return@withContext recordFailure(reportId, attempt, UNCONFIRMED_UPLOAD_MESSAGE)
            }
            deleteQueueRow(reportId, row.localPhotoPathsJson)
            ReportUploadOutcome.Success(isPublic = row.isPublic)
        } else {
            val message = ErrorMapper.toUserFriendlyMessage(error)
            recordFailure(reportId, attempt, message)
        }
    }

    /**
     * Confirms the complete server state. A verification error (for example a
     * timeout) is not confirmation: the caller keeps the row and retries.
     */
    private suspend fun isSynced(
        report: FishingReport,
        reportId: UUID,
        attempt: Int,
        lastError: String?
    ): Boolean = try {
        repository.isReportSynced(report)
    } catch (e: CancellationException) {
        markWaitingOnCancel(reportId, attempt, lastError)
        throw e
    } catch (e: Exception) {
        false
    }

    private suspend fun markWaitingOnCancel(reportId: UUID, attempt: Int, lastError: String?) {
        withContext(NonCancellable) {
            runCatching { dao.markWaiting(reportId, attempt, lastError) }
        }
    }

    private suspend fun recordFailure(
        reportId: UUID,
        attempt: Int,
        message: String
    ): ReportUploadOutcome =
        if (attempt >= MAX_UPLOAD_ATTEMPTS) {
            dao.markFailed(reportId, message)
            ReportUploadOutcome.Failed(message)
        } else {
            dao.markWaiting(reportId, attempt, message)
            ReportUploadOutcome.Retry(message)
        }

    private suspend fun deleteQueueRow(reportId: UUID, localPhotoPathsJson: String) {
        dao.deleteById(reportId)
        LocalPhotoFiles.delete(context, codec.decodePaths(localPhotoPathsJson))
    }
}
