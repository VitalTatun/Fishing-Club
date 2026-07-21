package com.example.fishing

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class FishingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация OSM с уникальным User-Agent
        Configuration.getInstance().userAgentValue = "FishingApp_Android_Client_v1.0_vital"
        Configuration.getInstance().osmdroidBasePath = cacheDir
    }
}
