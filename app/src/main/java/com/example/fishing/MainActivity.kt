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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fishing.ui.screens.CatchEditScreen
import com.example.fishing.ui.screens.CommentEditScreen
import com.example.fishing.ui.screens.FishingMethodAndBaitScreen
import com.example.fishing.ui.screens.FullScreenPhotoScreen
import com.example.fishing.ui.screens.CreateReportScreen
import com.example.fishing.ui.screens.MainScreen
import com.example.fishing.ui.screens.MapScreen
import com.example.fishing.ui.screens.ReportDetailScreen
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.viewmodel.MainViewModel
import org.osmdroid.config.Configuration
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Запрос разрешений
        requestLocationPermissions()

        // Инициализация конфигурации OSM
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        enableEdgeToEdge()
        setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                val viewModel: MainViewModel = viewModel()
                val reports by viewModel.reports.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val selectedTab by viewModel.selectedTab.collectAsState()
                
                val navController = rememberNavController()

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
                                selectedTab = selectedTab,
                                viewModel = viewModel,
                                onTabSelected = { index -> viewModel.selectTab(index) },
                                onCreateReportClick = {
                                    navController.navigate("create_report")
                                },
                                onReportClick = { report ->
                                    navController.navigate("detail/${report.id}")
                                }
                            )
                        }

                        composable("create_report") {
                            val resultMethod = navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.get<FishingMethod>("method") ?: FishingMethod.NONE
                            val resultBaits = navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.get<List<Bait>>("baits") ?: emptyList()
                            val resultFish = navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.get<List<Fish>>("fish") ?: emptyList()
                            val resultComment = navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.get<String>("comment") ?: ""

                            CreateReportScreen(
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { navController.popBackStack() },
                                initialMethod = resultMethod,
                                initialBaits = resultBaits,
                                initialFish = resultFish,
                                initialComment = resultComment,
                                onNavigateToCatchEdit = {
                                    navController.navigate("catch_edit")
                                },
                                onNavigateToMethodAndBaitEdit = {
                                    navController.navigate("method_bait_edit")
                                },
                                onNavigateToCommentEdit = {
                                    navController.navigate("comment_edit")
                                }
                            )
                        }

                        composable("comment_edit") {
                            val currentComment = navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<String>("comment") ?: ""

                            CommentEditScreen(
                                initialComment = currentComment,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { comment ->
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("comment", comment)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("method_bait_edit") {
                            val currentMethod = navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<FishingMethod>("method") ?: FishingMethod.NONE
                            val currentBaits = navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<List<Bait>>("baits") ?: emptyList()

                            FishingMethodAndBaitScreen(
                                initialMethod = currentMethod,
                                initialBaits = currentBaits,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { method, baits ->
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("method", method)
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("baits", ArrayList(baits))
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("catch_edit") {
                            val currentFish = navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<List<Fish>>("fish") ?: emptyList()
                            
                            CatchEditScreen(
                                fishList = currentFish,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { fish ->
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("fish", ArrayList(fish))
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = "detail/{reportId}",
                            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val reportId = backStackEntry.arguments
                                ?.getString("reportId")
                                ?.let(UUID::fromString)
                            reports.firstOrNull { it.id == reportId }?.let { report ->
                                ReportDetailScreen(
                                    report = report,
                                    onBackClick = { navController.popBackStack() },
                                    onMapClick = { point ->
                                        viewModel.requestMapLocation(point)
                                        navController.navigate("full_map")
                                    }
                                )
                            }
                        }

                        composable("full_map") {
                            MapScreen(
                                reports = reports,
                                viewModel = viewModel,
                                onBackClick = { 
                                    navController.popBackStack() 
                                },
                                onReportClick = { report ->
                                    navController.navigate("detail/${report.id}") {
                                        popUpTo("full_map") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "full_screen_photo/{reportId}/{index}",
                            arguments = listOf(
                                navArgument("reportId") { type = NavType.StringType },
                                navArgument("index") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val reportId = backStackEntry.arguments
                                ?.getString("reportId")
                                ?.let(UUID::fromString)
                            val index = backStackEntry.arguments?.getInt("index") ?: 0
                            reports.firstOrNull { it.id == reportId }?.let { report ->
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

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsPermission) {
            ActivityCompat.requestPermissions(this, permissions, 0)
        }
    }
}
