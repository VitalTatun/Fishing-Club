package com.example.fishing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                
                val navController = rememberNavController()

                // Храним выбранный отчет в состоянии (или можно передавать через ViewModel)
                var selectedReport by remember { mutableStateOf(reports.firstOrNull()) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.fillMaxSize(),
                        // Анимация "Слои": новый наезжает сверху, старый чуть сдвигается
                        enterTransition = {

                            slideInHorizontally(
                                initialOffsetX = { it }, // Появляется справа
                                animationSpec = tween(400)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 }, // Уходит лишь на треть влево
                                animationSpec = tween(400)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 }, // Возвращается из-за левого края
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it }, // Уезжает вправо, открывая нижний
                                animationSpec = tween(400)
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
                                },
                                onExperimentalClick = {
                                    navController.navigate("experimental")
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

                        composable("experimental") {
                            reports.firstOrNull()?.let { report ->
                                ExperimentalFishingDetailReport(
                                    report = report,
                                    onBackClick = { navController.popBackStack() },
                                    onPhotoClick = { index ->
                                        navController.navigate("full_screen_photo/$index")
                                    }
                                )
                            }
                        }

                        composable(
                            route = "full_screen_photo/{index}",
                            arguments = listOf(navArgument("index") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val index = backStackEntry.arguments?.getInt("index") ?: 0
                            val currentReport = if (navController.previousBackStackEntry?.destination?.route == "experimental") {
                                reports.firstOrNull()
                            } else {
                                selectedReport
                            }

                            currentReport?.let { report ->
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
