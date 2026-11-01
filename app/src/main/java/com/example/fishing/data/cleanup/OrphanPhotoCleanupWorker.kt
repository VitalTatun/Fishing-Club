package com.example.fishing.data.cleanup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Daily sweep of orphaned photo objects for the signed-in user. Work is
 * scheduled from [com.example.fishing.FishingApp] and does nothing when the
 * user is logged out.
 */
@HiltWorker
class OrphanPhotoCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val runner: OrphanPhotoCleanupRunner
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (runner.run()) return Result.success()
        // A transient listing/delete failure gets a few backoff retries; the
        // periodic schedule remains in place even after giving up.
        return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
    }

    companion object {
        const val MAX_RETRIES = 2
    }
}
