package com.example.fishing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fishing.model.*
import com.example.fishing.ui.screens.ExperimentalFishingDetailReport
import com.example.fishing.ui.screens.FullScreenPhotoScreen
import com.example.fishing.ui.screens.MainScreen
import com.example.fishing.ui.screens.ReportDetailScreen
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                val viewModel: MainViewModel = viewModel()
                val reports by viewModel.reports.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()

                var selectedReport by remember { mutableStateOf<FishingReport?>(null) }
                var showExperimental by remember { mutableStateOf(false) }
                var fullScreenPhotoIndex by remember { mutableStateOf<Int?>(null) }

                when {
                    fullScreenPhotoIndex != null && reports.isNotEmpty() -> {
                        val currentReport = if (showExperimental) reports.first() else selectedReport
                        if (currentReport != null) {
                            BackHandler { fullScreenPhotoIndex = null }
                            FullScreenPhotoScreen(
                                photos = currentReport.photo,
                                initialPage = fullScreenPhotoIndex!!,
                                onBackClick = { fullScreenPhotoIndex = null }
                            )
                        }
                    }

                    showExperimental && reports.isNotEmpty() -> {
                        BackHandler { showExperimental = false }
                        ExperimentalFishingDetailReport(
                            report = reports.first(),
                            onBackClick = { showExperimental = false },
                            onPhotoClick = { fullScreenPhotoIndex = it }
                        )
                    }

                    selectedReport != null -> {
                        BackHandler { selectedReport = null }
                        ReportDetailScreen(
                            report = selectedReport!!,
                            onBackClick = { selectedReport = null }
                        )
                    }

                    else -> {
                        MainScreen(
                            reports = reports,
                            isLoading = isLoading,
                            onReportClick = { selectedReport = it },
                            onExperimentalClick = { showExperimental = true }
                        )
                    }
                }
            }
        }
    }
}
