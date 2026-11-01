package com.example.fishing.data.cleanup

import android.util.Log
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs one cleanup pass and writes the daily summary to the log. Keeping this
 * outside the Worker makes the 24-hour grace period and failure handling
 * testable without WorkManager.
 */
@Singleton
class OrphanPhotoCleanupRunner @Inject constructor(
    private val cleaner: OrphanPhotoCleaner
) {
    /** Returns true when the pass completed, false when it should be retried. */
    suspend fun run(now: Instant = Instant.now()): Boolean = runCatching {
        val report = cleaner.cleanup(now.minus(GRACE))
        Log.i(
            TAG,
            "storage orphan cleanup: scanned=${report.scanned}, deleted=${report.deleted}"
        )
    }.onFailure { error ->
        Log.w(TAG, "storage orphan cleanup failed", error)
    }.isSuccess

    companion object {
        private const val TAG = "OrphanPhotoCleanup"

        /** Photos uploaded for a report that is still being retried are younger. */
        val GRACE: Duration = Duration.ofHours(24)
    }
}
