package com.example.fishing

import android.app.Application
import com.example.fishing.utils.PhotoUtils
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class FishingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация OSM с уникальным User-Agent
        Configuration.getInstance().userAgentValue = "FishingApp_Android_Client_v1.0_vital"
        Configuration.getInstance().osmdroidBasePath = cacheDir

        // Temporary report photos are not resumable across process restarts.
        Thread {
            runCatching { PhotoUtils.clearSavedPhotos(filesDir) }
        }.apply {
            name = "photo-cleanup"
            isDaemon = true
        }.start()
    }
}
