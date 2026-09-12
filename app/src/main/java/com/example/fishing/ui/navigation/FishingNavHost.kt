package com.example.fishing.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fishing.model.*
import com.example.fishing.ui.screens.main.MainScreen
import com.example.fishing.ui.screens.profile.ChangeHistoryScreen
import com.example.fishing.ui.screens.profile.EditProfileScreen
import com.example.fishing.ui.screens.report.create.*
import com.example.fishing.ui.screens.report.detail.PhotoViewerOverlay
import com.example.fishing.ui.screens.report.detail.ReportDetailLoadingScreen
import com.example.fishing.ui.screens.report.detail.ReportDetailScreen
import com.example.fishing.ui.screens.report.detail.ReportDetailUnavailableScreen
import com.example.fishing.ui.screens.report.detail.ReportDetailErrorScreen
import com.example.fishing.ui.screens.search.LocationSearchScreen
import com.example.fishing.ui.screens.search.ReportSearchScreen
import com.example.fishing.ui.screens.login.LoginScreen
import com.example.fishing.ui.screens.login.RegistrationScreen
import com.example.fishing.ui.screens.map.MapScreen
import com.example.fishing.ui.theme.FishingTransitions
import com.example.fishing.viewmodel.MainViewModel
import com.example.fishing.viewmodel.ReportDetailUiState
import com.example.fishing.viewmodel.CreateReportViewModel
import com.example.fishing.viewmodel.LoginViewModel
import com.example.fishing.viewmodel.RegistrationViewModel
import com.example.fishing.viewmodel.EditProfileViewModel
import com.example.fishing.data.FishingRepository
import com.example.fishing.data.AuthRepository
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.*
import android.net.Uri
import com.example.fishing.viewmodel.ReportPhoto

@Composable
fun FishingNavHost(
    authState: AuthState,
    fishingRepository: FishingRepository,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: MainViewModel = hiltViewModel()
    val createReportViewModel: CreateReportViewModel = hiltViewModel()

    val reports by viewModel.sortedReports.collectAsState()
    val homeUiState by viewModel.homeUiState.collectAsState()
    val favoriteReports by viewModel.favoriteReports.collectAsState()
    val mapMarkers by viewModel.mapMarkers.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val mapIsLoading by viewModel.mapIsLoading.collectAsState()
    val mapIsRefreshing by viewModel.mapIsRefreshing.collectAsState()
    val mapErrorMessage by viewModel.mapRefreshError.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val isAuthenticated = authState is AuthState.Authenticated
    val currentUser = (authState as? AuthState.Authenticated)?.user

    key(isAuthenticated) {
        val navController = rememberNavController()
        val startDestination = if (isAuthenticated) "main" else "login"

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
                    onNavigateToRegistration = {
                        navController.navigate("registration")
                    }
                )
            }

            composable("registration") {
                val registrationViewModel: RegistrationViewModel = hiltViewModel()
                RegistrationScreen(
                    viewModel = registrationViewModel,
                    onBackClick = { navController.popBackStack() },
                    onConfirmedLogin = { navController.popBackStack() }
                )
            }

            composable("main") {
                MainScreen(
                    homeUiState = homeUiState,
                    isInitialLoading = isInitialLoading,
                    isRefreshing = isRefreshing,
                    mapIsLoading = mapIsLoading,
                    mapIsRefreshing = mapIsRefreshing,
                    mapErrorMessage = mapErrorMessage,
                    selectedTab = selectedTab,
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
                    userEmail = currentUser?.email,
                    userName = currentUser?.name,
                    userImage = currentUser?.image,
                    currentUserId = currentUser?.id,
                    onLogout = {
                        coroutineScope.launch {
                            authRepository.logout()
                        }
                    },
                    onEditProfileClick = {
                        navController.navigate("edit_profile")
                    },
                    onChangeHistoryClick = {
                        navController.navigate("change_history")
                    },
                    errorText = viewModel.error.collectAsState().value,
                    onErrorDismiss = { viewModel.refresh() },
                    deleteErrorText = viewModel.deleteReportError.collectAsState().value,
                    onDeleteErrorDismiss = { viewModel.clearDeleteReportError() },
                    favoriteErrorText = viewModel.favoriteError.collectAsState().value,
                    onFavoriteErrorDismiss = { viewModel.clearFavoriteError() }
                )
            }

            composable("report_list_search") {
                val combinedReports = remember(reports, favoriteReports) {
                    (reports + favoriteReports).distinctBy { it.id }
                }
                ReportSearchScreen(
                    reports = combinedReports,
                    favoriteReports = favoriteReports,
                    query = viewModel.searchQuery,
                    onQueryChange = { viewModel.searchQuery = it },
                    selectedDate = viewModel.searchSelectedDate,
                    onDateChange = { viewModel.searchSelectedDate = it },
                    isFavoritesSelected = viewModel.searchIsFavoritesSelected,
                    onFavoritesChange = { viewModel.searchIsFavoritesSelected = it },
                    isTrophySelected = viewModel.searchIsTrophySelected,
                    onTrophyChange = { viewModel.searchIsTrophySelected = it },
                    isPaidSelected = viewModel.searchIsPaidSelected,
                    onPaidChange = { viewModel.searchIsPaidSelected = it },
                    selectedCatch = viewModel.searchSelectedCatch,
                    onCatchChange = { viewModel.searchSelectedCatch = it },
                    selectedMethod = viewModel.searchSelectedMethod,
                    onMethodChange = { viewModel.searchSelectedMethod = it },
                    onReportClick = { report ->
                        navController.navigate("detail/${report.id}")
                    },
                    onBack = { navController.popBackStack() },
                    onToggleFavorite = viewModel::toggleFavorite,
                    favoriteError = viewModel.favoriteError.collectAsState().value,
                    onFavoriteErrorDismiss = { viewModel.clearFavoriteError() },
                    currentUserId = currentUser?.id
                )
            }

            composable("report_search") {
                val isFromWaterEdit = navController.previousBackStackEntry
                    ?.destination?.route == "water_edit"

                LocationSearchScreen(
                    onLocationSelected = { point, _, polygon ->
                        if (isFromWaterEdit) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("searchLocation", point)
                        } else {
                            viewModel.selectTab(1)
                            viewModel.requestMapLocation(point)
                            viewModel.setHighlightedPolygon(polygon)
                        }
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("create_report") {
                val currentEntry = navController.currentBackStackEntry!!

                currentEntry.savedStateHandle.get<FishingMethod>("method")?.let {
                    createReportViewModel.formSelectedMethod = it
                }
                currentEntry.savedStateHandle.get<List<Bait>>("baits")?.let {
                    createReportViewModel.formSelectedBaits = it
                }
                currentEntry.savedStateHandle.get<List<Fish>>("fish")?.let {
                    createReportViewModel.formSelectedFish = it
                }
                currentEntry.savedStateHandle.get<Float>("weight")?.let {
                    createReportViewModel.formWeight = it
                }
                currentEntry.savedStateHandle.get<String>("comment")?.let {
                    createReportViewModel.formComment = it
                }
                currentEntry.savedStateHandle.get<GeoPoint>("location")?.let {
                    createReportViewModel.formLocation = it
                }
                currentEntry.savedStateHandle.get<List<Uri>>("photos")?.let { uris ->
                    createReportViewModel.formPhotos = uris.map { uri ->
                        createReportViewModel.formPhotos.find { it.uri == uri } ?: ReportPhoto(uri = uri)
                    }
                }

                CreateReportScreen(
                    viewModel = createReportViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaveComplete = {
                        viewModel.refresh()
                        navController.popBackStack()
                    },
                    onNavigateToCatchEdit = {
                        currentEntry.savedStateHandle["fish"] = ArrayList(createReportViewModel.formSelectedFish)
                        currentEntry.savedStateHandle["weight"] = createReportViewModel.formWeight
                        navController.navigate("catch_edit")
                    },
                    onNavigateToMethodAndBaitEdit = {
                        currentEntry.savedStateHandle["method"] = createReportViewModel.formSelectedMethod
                        currentEntry.savedStateHandle["baits"] = ArrayList(createReportViewModel.formSelectedBaits)
                        navController.navigate("method_bait_edit")
                    },
                    onNavigateToCommentEdit = {
                        currentEntry.savedStateHandle["comment"] = createReportViewModel.formComment
                        navController.navigate("comment_edit")
                    },
                    onNavigateToWaterEdit = {
                        currentEntry.savedStateHandle["location"] = createReportViewModel.formLocation
                        navController.navigate("water_edit")
                    },
                    onNavigateToWaterNameEdit = {
                        navController.navigate("water_name_edit")
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

            composable("water_name_edit") {
                WaterEditScreen(
                    viewModel = createReportViewModel,
                    reportsViewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { navController.popBackStack() }
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
                val isTrophy = createReportViewModel.formReportType == FishingType.HAUL

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

                val reportDetailState by viewModel.reportDetailUiState.collectAsState()
                val isDeletingReport by viewModel.isDeletingReport.collectAsState()
                val deleteReportError by viewModel.deleteReportError.collectAsState()
                val deletedReportId by viewModel.deletedReportId.collectAsState()

                var photoViewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }

                LaunchedEffect(deletedReportId, reportId) {
                    if (deletedReportId == reportId) {
                        viewModel.clearDeletedReport()
                        navController.popBackStack()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = reportDetailState) {
                        is ReportDetailUiState.Success ->
                            if (state.report.id == reportId) {
                                ReportDetailScreen(
                                    report = state.report,
                                    onBackClick = { navController.popBackStack() },
                                    isFavorite = favoriteReports.any { it.id == state.report.id },
                                    onToggleFavorite = { viewModel.toggleFavorite(state.report) },
                                    onMapClick = { point ->
                                        viewModel.requestMapLocation(point)
                                        navController.navigate("full_map/${state.report.id}")
                                    },
                                    isOwnReport = state.report.userId == currentUser?.id,
                                    isDeleting = isDeletingReport,
                                    onDeleteReport = { viewModel.deleteReport(state.report.id) },
                                    onPhotoClick = { index ->
                                        photoViewerIndex = index
                                    },
                                    deleteError = deleteReportError,
                                    onDeleteErrorDismiss = { viewModel.clearDeleteReportError() },
                                    favoriteError = viewModel.favoriteError.collectAsState().value,
                                    onFavoriteErrorDismiss = { viewModel.clearFavoriteError() }
                                )
                            } else {
                                ReportDetailLoadingScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                        is ReportDetailUiState.Empty ->
                            if (state.reportId == reportId) {
                                ReportDetailUnavailableScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            } else {
                                ReportDetailLoadingScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                        is ReportDetailUiState.Error ->
                            if (state.reportId == reportId) {
                                ReportDetailErrorScreen(
                                    message = state.message,
                                    onBackClick = { navController.popBackStack() },
                                    onRetry = { reportId?.let(viewModel::loadReportDetails) }
                                )
                            } else {
                                ReportDetailLoadingScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                        ReportDetailUiState.Loading ->
                            ReportDetailLoadingScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                    }

                    val currentReport = (reportDetailState as? ReportDetailUiState.Success)?.report
                    if (photoViewerIndex != null && currentReport != null && currentReport.id == reportId) {
                        PhotoViewerOverlay(
                            photos = currentReport.photos.map { it.url },
                            initialIndex = photoViewerIndex!!,
                            onDismiss = { photoViewerIndex = null }
                        )
                    }
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
                            popUpTo("full_map/$reportId") { inclusive = true }
                        }
                    },
                    markersInteractive = false,
                    initialReportId = reportId,
                    repository = fishingRepository,
                    favoriteReports = favoriteReports
                )
            }

            composable("change_history") {
                ChangeHistoryScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("edit_profile") {
                val editProfileViewModel: EditProfileViewModel = hiltViewModel()
                val user by editProfileViewModel.user.collectAsState()
                val isLoading by editProfileViewModel.isLoading.collectAsState()
                val error by editProfileViewModel.error.collectAsState()
                val saveSuccess by editProfileViewModel.saveSuccess.collectAsState()
                val context = LocalContext.current

                EditProfileScreen(
                    initialName = user?.name ?: "",
                    email = user?.email ?: "",
                    avatarUrl = user?.image,
                    isLoading = isLoading,
                    error = error,
                    saveSuccess = saveSuccess,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { name, imageUri ->
                        editProfileViewModel.updateProfile(
                            context.contentResolver,
                            context.filesDir,
                            name,
                            imageUri
                        )
                    },
                    onResetSaveSuccess = { editProfileViewModel.resetSuccess() },
                    onChangePasswordClick = { /* TODO: Implement */ },
                    onDeleteAccountClick = { /* TODO: Implement */ }
                )
            }
        }
    }
}
