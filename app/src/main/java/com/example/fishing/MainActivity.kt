package com.example.fishing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fishing.model.*
import com.example.fishing.ui.screens.MainScreen
import com.example.fishing.ui.screens.ReportDetailScreen
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.MainViewModel
import java.util.*

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

                if (selectedReport == null) {
                    MainScreen(
                        reports = reports,
                        isLoading = isLoading,
                        onReportClick = { selectedReport = it }
                    )
                } else {
                    BackHandler { selectedReport = null }
                    ReportDetailScreen(
                        report = selectedReport!!,
                        onBackClick = { selectedReport = null }
                    )
                }
            }
        }
    }
}
