package com.example.fishing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                
                val navController = rememberNavController()

                // Храним выбранный отчет в состоянии (или можно передавать через ViewModel)
                var selectedReport by remember { mutableStateOf(reports.firstOrNull()) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.fillMaxSize(),
                        // Анимация "Слои": новый наезжает сверху, старый чуть сдвигается
                        enterTransition = {

                            slideInHorizontally(
                                initialOffsetX = { it }, // Появляется справа
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 7 }, // Уходит лишь на треть влево
                                animationSpec = tween(300)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 7 }, // Возвращается из-за левого края
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it }, // Уезжает вправо, открывая нижний
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        composable("main") {
                            MainScreen(
                                reports = reports,
                                isLoading = isLoading,
                                onReportClick = { report ->
                                    selectedReport = report
                                    navController.navigate("detail")
                                }
                            )
                        }

                        composable("detail") {
                            selectedReport?.let { report ->
                                ReportDetailScreen(
                                    report = report,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }

                        composable(
                            route = "full_screen_photo/{index}",
                            arguments = listOf(navArgument("index") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val index = backStackEntry.arguments?.getInt("index") ?: 0
                            selectedReport?.let { report ->
                                FullScreenPhotoScreen(
                                    photos = report.photo,
                                    initialPage = index,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
