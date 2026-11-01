package com.example.fishing

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.fishing.data.cleanup.OrphanPhotoCleanupScheduler
import com.example.fishing.data.upload.ReportUploadQueue
import com.example.fishing.utils.PhotoUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration as OsmConfiguration
import javax.inject.Inject

@HiltAndroidApp
class FishingApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var reportUploadQueue: ReportUploadQueue

    @Inject
    lateinit var orphanPhotoCleanupScheduler: OrphanPhotoCleanupScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Инициализация OSM с уникальным User-Agent
        OsmConfiguration.getInstance().userAgentValue = "FishingApp_Android_Client_v1.0_vital"
        OsmConfiguration.getInstance().osmdroidBasePath = cacheDir

        // Photos of interrupted edits are not resumable, but files referenced by
        // a queued upload must survive the restart. Cleanup finishes before the
        // queue is resumed so a worker cannot race the deletion.
        appScope.launch {
            runCatching {
                reportUploadQueue.cleanupStale()
                val keepPaths = reportUploadQueue.referencedLocalPaths()
                PhotoUtils.clearSavedPhotos(filesDir, keepPaths)
                reportUploadQueue.resumePending()
            }
            runCatching { orphanPhotoCleanupScheduler.schedule() }
        }
    }
}
