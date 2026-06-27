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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fishing.ui.screens.CatchEditScreen
import com.example.fishing.ui.screens.CommentEditScreen
import com.example.fishing.ui.screens.FishingMethodAndBaitScreen
import com.example.fishing.ui.screens.FishingLocationScreen
import com.example.fishing.ui.screens.FullScreenPhotoScreen
import com.example.fishing.ui.screens.CreateReportScreen
import com.example.fishing.ui.screens.LocationSearchScreen
import com.example.fishing.ui.screens.MainScreen
import com.example.fishing.ui.screens.MapScreen
import com.example.fishing.ui.screens.ReportDetailScreen
import com.example.fishing.ui.screens.ReportDetailLoadingScreen
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.viewmodel.MainViewModel
import com.example.fishing.viewmodel.LoginViewModel
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.FishingRepository
import com.example.fishing.ui.screens.LoginScreen
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var fishingRepository: FishingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Запрос разрешений
        requestLocationPermissions()

        // Инициализация конфигурации OSM
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = cacheDir

        enableEdgeToEdge()
        setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                val viewModel: MainViewModel = hiltViewModel()
                val reports by viewModel.reports.collectAsState()
                val allReports by viewModel.allReports.collectAsState()
                val mapMarkers by viewModel.mapMarkers.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val selectedTab by viewModel.selectedTab.collectAsState()

                val navController = rememberNavController()
                val startDestination = remember {
                    if (authRepository.isLoggedIn()) "main" else "login"
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 7 },
                                animationSpec = tween(300)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 7 },
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        composable("login") {
                            val loginViewModel: LoginViewModel = hiltViewModel()
                            LoginScreen(
                                viewModel = loginViewModel,
                                onAuthenticated = {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("main") {
                            val coroutineScope = rememberCoroutineScope()

                            LaunchedEffect(Unit) {
                                viewModel.loadReportsIfNeeded()
                                viewModel.loadMapMarkers()
                            }

                            MainScreen(
                                reports = reports,
                                isLoading = isLoading,
                                selectedTab = selectedTab,
                                allReports = allReports,
                                mapMarkers = mapMarkers,
                                viewModel = viewModel,
                                repository = fishingRepository,
                                onTabSelected = { index -> viewModel.selectTab(index) },
                                onCreateReportClick = {
                                    navController.navigate("create_report")
                                },
                                onReportClick = { report ->
                                    navController.navigate("detail/${report.id}")
                                },
                                onDeleteReport = { report ->
                                    viewModel.deleteReport(report.id)
                                },
                                onSearchClick = {
                                    navController.navigate("report_search")
                                },
                                userEmail = authRepository.currentUser()?.email,
                                onLogout = {
                                    coroutineScope.launch {
                                        authRepository.logout()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                errorText = viewModel.error.collectAsState().value,
                                onErrorDismiss = { viewModel.refresh() }
                            )
                        }

                        composable("report_search") {
                            val isFromWaterEdit = navController.previousBackStackEntry
                                ?.destination?.route == "water_edit"

                            LocationSearchScreen(
                                onLocationSelected = { point, _ ->
                                    if (isFromWaterEdit) {
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("searchLocation", point)
                                    } else {
                                        viewModel.selectTab(1)
                                        viewModel.requestMapLocation(point)
                                    }
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("create_report") {
                            val currentEntry = navController.currentBackStackEntry!!

                            currentEntry.savedStateHandle.get<FishingMethod>("method")?.let {
                                viewModel.formSelectedMethod = it
                            }
                            currentEntry.savedStateHandle.get<List<Bait>>("baits")?.let {
                                viewModel.formSelectedBaits = it
                            }
                            currentEntry.savedStateHandle.get<List<Fish>>("fish")?.let {
                                viewModel.formSelectedFish = it
                            }
                            currentEntry.savedStateHandle.get<String>("comment")?.let {
                                viewModel.formComment = it
                            }
                            currentEntry.savedStateHandle.get<GeoPoint>("location")?.let {
                                viewModel.formLocation = it
                            }

                            CreateReportScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { title, type, waterName, location, fishingTime, weight, fish, method, baits, comment, shore, isPublic, isPaidWater, photos ->
                                    val internalPhotos = photos.map { copyPhotoToInternalStorage(Uri.parse(it)) }
                                    viewModel.saveNewReport(title, type, waterName, location, fishingTime, weight, fish, method, baits, comment, shore, isPublic, isPaidWater, internalPhotos)
                                    viewModel.resetFormState()
                                    navController.popBackStack()
                                },
                                onNavigateToCatchEdit = {
                                    currentEntry.savedStateHandle["fish"] = ArrayList(viewModel.formSelectedFish)
                                    navController.navigate("catch_edit")
                                },
                                onNavigateToMethodAndBaitEdit = {
                                    currentEntry.savedStateHandle["method"] = viewModel.formSelectedMethod
                                    currentEntry.savedStateHandle["baits"] = ArrayList(viewModel.formSelectedBaits)
                                    navController.navigate("method_bait_edit")
                                },
                                onNavigateToCommentEdit = {
                                    currentEntry.savedStateHandle["comment"] = viewModel.formComment
                                    navController.navigate("comment_edit")
                                },
                                onNavigateToWaterEdit = {
                                    currentEntry.savedStateHandle["location"] = viewModel.formLocation
                                    navController.navigate("water_edit")
                                }
                            )
                        }

                        composable("water_edit") { backStackEntry ->
                            val currentLocation = navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<GeoPoint>("location")

                            val searchLocation =
                                backStackEntry.savedStateHandle.get<GeoPoint>("searchLocation")

                            if (searchLocation != null) {
                                backStackEntry.savedStateHandle.remove<GeoPoint>("searchLocation")
                            }

                            FishingLocationScreen(
                                initialLocation = currentLocation,
                                searchLocation = searchLocation,
                                onBackClick = { navController.popBackStack() },
                                onSaveClick = { point ->
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("location", point)
                                    navController.popBackStack()
                                },
                                onSearchClick = {
                                    navController.navigate("report_search")
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

                            LaunchedEffect(reportId) {
                                reportId?.let { viewModel.loadReportDetails(it) }
                            }

                            val currentReport by viewModel.currentReport.collectAsState()

                            val report = currentReport
                            if (report != null && report.id == reportId) {
                                ReportDetailScreen(
                                    report = report,
                                    onBackClick = { navController.popBackStack() },
                                    onMapClick = { point ->
                                        viewModel.requestMapLocation(point)
                                        navController.navigate("full_map/${report.id}")
                                    }
                                )
                            } else {
                                ReportDetailLoadingScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }

                        composable(
                            route = "full_map/{reportId}",
                            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val reportId = backStackEntry.arguments
                                ?.getString("reportId")
                                ?.let(UUID::fromString)

                            val singleMarker = reportId?.let { id ->
                                mapMarkers.firstOrNull { it.id == id }
                            }

                            MapScreen(
                                markers = if (singleMarker != null) listOf(singleMarker) else mapMarkers,
                                viewModel = viewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onMarkerClick = { marker ->
                                    navController.navigate("detail/${marker.id}") {
                                        popUpTo("full_map/${reportId}") { inclusive = true }
                                    }
                                },
                                markersInteractive = false,
                                initialReportId = reportId,
                                repository = fishingRepository
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

                            LaunchedEffect(reportId) {
                                reportId?.let { viewModel.loadReportDetails(it) }
                            }

                            val currentReport by viewModel.currentReport.collectAsState()

                            val report = currentReport
                            if (report != null && report.id == reportId) {
                                FullScreenPhotoScreen(
                                    photos = report.photo,
                                    initialPage = index,
                                    onBackClick = { navController.popBackStack() }
                                )
                            } else {
                                ReportDetailLoadingScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun copyPhotoToInternalStorage(uri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return uri.toString()
            val fileName = "photo_${UUID.randomUUID()}.jpg"
            val photosDir = File(filesDir, "photos")
            photosDir.mkdirs()
            val outputFile = File(photosDir, fileName)
            FileOutputStream(outputFile).use { output ->
                inputStream.copyTo(output)
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            uri.toString()
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
