package com.example.fishing.ui.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fishing.model.*
import com.example.fishing.ui.screens.main.MainScreen
import com.example.fishing.ui.screens.report.create.*
import com.example.fishing.ui.screens.report.detail.FullScreenPhotoScreen
import com.example.fishing.ui.screens.report.detail.ReportDetailLoadingScreen
import com.example.fishing.ui.screens.report.detail.ReportDetailScreen
import com.example.fishing.ui.screens.search.LocationSearchScreen
import com.example.fishing.ui.screens.search.ReportSearchScreen
import com.example.fishing.ui.screens.login.LoginScreen
import com.example.fishing.ui.screens.map.MapScreen
import com.example.fishing.ui.theme.FishingTransitions
import com.example.fishing.viewmodel.MainViewModel
import com.example.fishing.viewmodel.LoginViewModel
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.AuthRepository
import com.example.fishing.utils.PhotoUtils
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.*

@Composable
fun FishingNavHost(
    navController: NavHostController,
    startDestination: String,
    viewModel: MainViewModel,
    fishingRepository: FishingRepository,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val reports by viewModel.reports.collectAsState()
    val allReports by viewModel.allReports.collectAsState()
    val favoriteReports by viewModel.favoriteReports.collectAsState()
    val mapMarkers by viewModel.mapMarkers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = FishingTransitions.defaultEnterTransition,
        exitTransition = FishingTransitions.defaultExitTransition,
        popEnterTransition = FishingTransitions.defaultPopEnterTransition,
        popExitTransition = FishingTransitions.defaultPopExitTransition
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
            LaunchedEffect(Unit) {
                viewModel.loadReportsIfNeeded()
                viewModel.loadMapMarkers()
            }

            MainScreen(
                reports = reports,
                isLoading = isLoading,
                selectedTab = selectedTab,
                allReports = allReports,
                favoriteReports = favoriteReports,
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
                    if (selectedTab == 0) {
                        navController.navigate("report_list_search")
                    } else {
                        navController.navigate("report_search")
                    }
                },
                userEmail = authRepository.currentUser()?.email,
                currentUserId = authRepository.currentUser()?.id,
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

        composable("report_list_search") {
            ReportSearchScreen(
                reports = reports,
                favoriteReports = favoriteReports,
                onReportClick = { report ->
                    navController.navigate("detail/${report.id}")
                },
                onBack = { navController.popBackStack() },
                currentUserId = authRepository.currentUser()?.id
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
            currentEntry.savedStateHandle.get<Float>("weight")?.let {
                viewModel.formWeight = it
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
                    val internalPhotos = photos.mapNotNull { 
                        PhotoUtils.copyPhotoToInternalStorage(context.contentResolver, context.filesDir, Uri.parse(it)) 
                    }
                    viewModel.saveNewReport(title, type, waterName, location, fishingTime, weight, fish, method, baits, comment, shore, isPublic, isPaidWater, internalPhotos)
                    viewModel.resetFormState()
                    navController.popBackStack()
                },
                onNavigateToCatchEdit = {
                    currentEntry.savedStateHandle["fish"] = ArrayList(viewModel.formSelectedFish)
                    currentEntry.savedStateHandle["weight"] = viewModel.formWeight
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
            val currentWeight = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<Float>("weight") ?: 0f
            val isTrophy = viewModel.formReportType == FishingType.HAUL

            CatchEditScreen(
                fishList = currentFish,
                initialWeight = currentWeight,
                onBackClick = { navController.popBackStack() },
                onSaveClick = { fish, weight ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("fish", ArrayList(fish))
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("weight", weight)
                    navController.popBackStack()
                },
                isTrophy = isTrophy
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
                    isFavorite = favoriteReports.any { it.id == report.id },
                    onToggleFavorite = { viewModel.toggleFavorite(report) },
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
