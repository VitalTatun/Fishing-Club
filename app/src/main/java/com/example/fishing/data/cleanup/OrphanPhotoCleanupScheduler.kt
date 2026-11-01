package com.example.fishing.data.cleanup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Daily orphan-photo cleanup; separated so startup does not depend on WorkManager. */
interface OrphanPhotoCleanupScheduler {
    fun schedule()
}

@Singleton
class WorkManagerOrphanPhotoCleanupScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : OrphanPhotoCleanupScheduler {

    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<OrphanPhotoCleanupWorker>(
            1,
            TimeUnit.DAYS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            // Let the app finish start-up work before listing Storage.
            .setInitialDelay(INITIAL_DELAY_HOURS, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val UNIQUE_NAME = "orphan-photo-cleanup"
        private const val INITIAL_DELAY_HOURS = 1L
    }
}
