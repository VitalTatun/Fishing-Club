package com.example.fishing.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.fishing.model.*
import com.example.fishing.ui.screens.main.MainScreen
import com.example.fishing.ui.screens.profile.ChangeHistoryScreen
import com.example.fishing.ui.screens.profile.EditProfileScreen
import com.example.fishing.ui.screens.profile.ChangePasswordScreen
import com.example.fishing.ui.screens.profile.AppearanceScreen
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
import com.example.fishing.viewmodel.ChangePasswordViewModel
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

    val reports by viewModel.sortedReports.collectAsStateWithLifecycle()
    val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val favoriteOpsInFlight by viewModel.favoriteOpsInFlight.collectAsStateWithLifecycle()
    val filterFavoriteReports by viewModel.filterFavoriteReports.collectAsStateWithLifecycle()
    val likeStates by viewModel.likeStates.collectAsStateWithLifecycle()
    val favoriteError by viewModel.favoriteError.collectAsStateWithLifecycle()
    val likeError by viewModel.likeError.collectAsStateWithLifecycle()
    val mapMarkers by viewModel.mapMarkers.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val loadMoreError by viewModel.loadMoreError.collectAsStateWithLifecycle()
    val hasMoreReports by viewModel.hasMoreReports.collectAsStateWithLifecycle()
    val mapIsLoading by viewModel.mapIsLoading.collectAsStateWithLifecycle()
    val mapIsRefreshing by viewModel.mapIsRefreshing.collectAsStateWithLifecycle()
    val mapErrorMessage by viewModel.mapRefreshError.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val reportDisplayMode by viewModel.reportDisplayMode.collectAsStateWithLifecycle()
    val pendingUploads by viewModel.pendingUploads.collectAsStateWithLifecycle()
    val uploadNotice by viewModel.uploadNotice.collectAsStateWithLifecycle()

    val isAuthenticated = authState is AuthState.Authenticated
    val currentUser = (authState as? AuthState.Authenticated)?.user

    key(isAuthenticated) {
        val navController = rememberNavController()
        val startDestination = if (isAuthenticated) MainRoute else LoginRoute

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            enterTransition = FishingTransitions.defaultEnterTransition,
            exitTransition = FishingTransitions.defaultExitTransition,
            popEnterTransition = FishingTransitions.defaultPopEnterTransition,
            popExitTransition = FishingTransitions.defaultPopExitTransition,
            predictivePopEnterTransition = FishingTransitions.predictivePopEnterTransition,
            predictivePopExitTransition = FishingTransitions.predictivePopExitTransition
        ) {
            composable<LoginRoute> {
                val loginViewModel: LoginViewModel = hiltViewModel()
                LoginScreen(
                    viewModel = loginViewModel,
                    onNavigateToRegistration = {
                        navController.navigate(RegistrationRoute)
                    }
                )
            }

            composable<RegistrationRoute> {
                val registrationViewModel: RegistrationViewModel = hiltViewModel()
                RegistrationScreen(
                    viewModel = registrationViewModel,
                    onBackClick = { navController.popBackStack() },
                    onConfirmedLogin = { navController.popBackStack() }
                )
            }

            composable<MainRoute> {
                MainScreen(
                    homeUiState = homeUiState,
                    isRefreshing = isRefreshing,
                    isLoadingMore = isLoadingMore,
                    loadMoreError = loadMoreError,
                    hasMoreReports = hasMoreReports,
                    onLoadMore = { viewModel.loadMoreReports() },
                    onRetryLoadMore = { viewModel.loadMoreReports() },
                    mapIsLoading = mapIsLoading,
                    mapIsRefreshing = mapIsRefreshing,
                    mapErrorMessage = mapErrorMessage,
                    selectedTab = selectedTab,
                    mapMarkers = mapMarkers,
                    viewModel = viewModel,
                    repository = fishingRepository,
                    onTabSelected = { index -> viewModel.selectTab(index) },
                    onCreateReportClick = {
                        navController.navigate(CreateReportRoute)
                    },
                    onReportClick = { report ->
                        navController.navigate(ReportDetailRoute(report.id.toString()))
                    },
                    onSearchClick = {
                        if (selectedTab == 0) {
                            navController.navigate(ReportListSearchRoute)
                        } else {
                            navController.navigate(LocationSearchRoute)
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
                        navController.navigate(EditProfileRoute)
                    },
                    onChangeHistoryClick = {
                        navController.navigate(ChangeHistoryRoute)
                    },
                    onAppearanceClick = {
                        navController.navigate(AppearanceRoute)
                    },
                    errorText = viewModel.error.collectAsState().value,
                    onErrorDismiss = { viewModel.refresh() },
                    deleteErrorText = viewModel.deleteReportError.collectAsState().value,
                    onDeleteErrorDismiss = { viewModel.clearDeleteReportError() },
                    favoriteErrorText = favoriteError,
                    onFavoriteErrorDismiss = { viewModel.clearFavoriteError() },
                    likeErrorText = likeError,
                    onLikeErrorDismiss = { viewModel.clearLikeError() },
                    pendingUploads = pendingUploads,
                    uploadNotice = uploadNotice,
                    onRetryPendingUpload = { viewModel.retryPendingUpload(it) },
                    onCancelPendingUpload = { viewModel.cancelPendingUpload(it) },
                    onUploadNoticeConsumed = { viewModel.consumeUploadNotice() }
                )
            }

            composable<ReportListSearchRoute> {
                val searchState by viewModel.searchState.collectAsStateWithLifecycle()
                LaunchedEffect(searchState.filters.isFavoritesSelected) {
                    if (searchState.filters.isFavoritesSelected) {
                        viewModel.loadFilterFavoriteReports(force = true)
                    }
                }
                ReportSearchScreen(
                    reports = reports,
                    favoriteReports = filterFavoriteReports,
                    state = searchState,
                    onStateChange = viewModel::updateSearchState,
                    onReportClick = { report ->
                        navController.navigate(ReportDetailRoute(report.id.toString()))
                    },
                    onBack = { navController.popBackStack() },
                    favoriteError = viewModel.favoriteError.collectAsState().value,
                    onFavoriteErrorDismiss = { viewModel.clearFavoriteError() }
                )
            }

            composable<LocationSearchRoute> {
                val isFromWaterEdit = navController.previousBackStackEntry
                    ?.destination
                    ?.hasRoute<FishingLocationRoute>() == true

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

            composable<CreateReportRoute> {
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
                        if (navController.currentBackStackEntry
                                ?.destination
                                ?.hasRoute<CreateReportRoute>() == true
                        ) {
                            navController.popBackStack()
                        }
                    },
                    onNavigateToCatchEdit = {
                        currentEntry.savedStateHandle["fish"] = ArrayList(createReportViewModel.formSelectedFish)
                        currentEntry.savedStateHandle["weight"] = createReportViewModel.formWeight
                        navController.navigate(CatchEditRoute)
                    },
                    onNavigateToMethodAndBaitEdit = {
                        currentEntry.savedStateHandle["method"] = createReportViewModel.formSelectedMethod
                        currentEntry.savedStateHandle["baits"] = ArrayList(createReportViewModel.formSelectedBaits)
                        navController.navigate(MethodBaitEditRoute)
                    },
                    onNavigateToCommentEdit = {
                        currentEntry.savedStateHandle["comment"] = createReportViewModel.formComment
                        navController.navigate(CommentEditRoute)
                    },
                    onNavigateToWaterEdit = {
                        currentEntry.savedStateHandle["location"] = createReportViewModel.formLocation
                        navController.navigate(FishingLocationRoute)
                    },
                    onNavigateToWaterNameEdit = {
                        navController.navigate(WaterNameEditRoute)
                    }
                )
            }

            composable<FishingLocationRoute> { backStackEntry ->
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
                        navController.navigate(LocationSearchRoute)
                    }
                )
            }

            composable<WaterNameEditRoute> {
                WaterEditScreen(
                    viewModel = createReportViewModel,
                    reportsViewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { navController.popBackStack() }
                )
            }

            composable<CommentEditRoute> {
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

            composable<MethodBaitEditRoute> {
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

            composable<CatchEditRoute> {
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

            composable<ReportDetailRoute> { backStackEntry ->
                // Consume the back gesture so the system predictive preview is not
                // shown on this destination; the regular pop slide still plays.
                BackHandler { navController.popBackStack() }

                val reportId = runCatching {
                    UUID.fromString(backStackEntry.toRoute<ReportDetailRoute>().reportId)
                }.getOrNull()

                LaunchedEffect(reportId) {
                    reportId?.let { viewModel.loadReportDetails(it) }
                }

                val reportDetailState by viewModel.reportDetailUiState.collectAsState()
                val isDeletingReport by viewModel.isDeletingReport.collectAsState()
                val deleteReportError by viewModel.deleteReportError.collectAsState()
                val deletedReportId by viewModel.deletedReportId.collectAsState()
                val isUpdatingReportVisibility by
                    viewModel.isUpdatingReportVisibility.collectAsState()
                val reportVisibilityError by
                    viewModel.reportVisibilityError.collectAsState()
                val reportVisibilityResult by
                    viewModel.reportVisibilityResult.collectAsState()

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
                                    isFavorite = state.report.id in favoriteIds,
                                    isFavoriteUpdating = state.report.id in favoriteOpsInFlight,
                                    onToggleFavorite = { viewModel.toggleFavorite(state.report) },
                                    onMapClick = { point ->
                                        viewModel.requestMapLocation(point)
                                        navController.navigate(FullMapRoute(state.report.id.toString()))
                                    },
                                    isOwnReport = state.report.userId == currentUser?.id,
                                    isDeleting = isDeletingReport,
                                    onDeleteReport = { viewModel.deleteReport(state.report.id) },
                                    isUpdatingVisibility = isUpdatingReportVisibility,
                                    onToggleVisibility = { isPublic ->
                                        viewModel.setReportVisibility(state.report.id, isPublic)
                                    },
                                    visibilityError = reportVisibilityError,
                                    onVisibilityErrorDismiss = {
                                        viewModel.clearReportVisibilityError()
                                    },
                                    visibilityResult = reportVisibilityResult,
                                    onVisibilityResultShown = {
                                        viewModel.clearReportVisibilityResult()
                                    },
                                    onPhotoClick = { index ->
                                        photoViewerIndex = index
                                    },
                                    deleteError = deleteReportError,
                                    onDeleteErrorDismiss = { viewModel.clearDeleteReportError() },
                                    favoriteError = favoriteError,
                                    onFavoriteErrorDismiss = { viewModel.clearFavoriteError() },
                                    likeState = likeStates[state.report.id],
                                    onToggleLike = { viewModel.toggleLike(state.report) },
                                    likeError = likeError,
                                    onLikeErrorDismiss = { viewModel.clearLikeError() }
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

            composable<FullMapRoute> { backStackEntry ->
                val fullMapRoute = backStackEntry.toRoute<FullMapRoute>()
                val reportId = runCatching {
                    UUID.fromString(fullMapRoute.reportId)
                }.getOrNull()

                val singleMarker = reportId?.let { id ->
                    mapMarkers.firstOrNull { it.id == id }
                }

                val mapFilters by viewModel.mapFilters.collectAsStateWithLifecycle()
                LaunchedEffect(mapFilters.isFavoritesSelected) {
                    if (mapFilters.isFavoritesSelected) {
                        viewModel.loadFilterFavoriteReports(force = true)
                    }
                }
                MapScreen(
                    markers = if (singleMarker != null) listOf(singleMarker) else mapMarkers,
                    viewModel = viewModel,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onMarkerClick = { marker ->
                        navController.navigate(ReportDetailRoute(marker.id.toString())) {
                            popUpTo(fullMapRoute) { inclusive = true }
                        }
                    },
                    markersInteractive = false,
                    initialReportId = reportId,
                    repository = fishingRepository,
                    favoriteReports = filterFavoriteReports
                )
            }

            composable<ChangeHistoryRoute> {
                ChangeHistoryScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<EditProfileRoute> {
                val editProfileViewModel: EditProfileViewModel = hiltViewModel()
                val user by editProfileViewModel.user.collectAsState()
                val isLoading by editProfileViewModel.isLoading.collectAsState()
                val error by editProfileViewModel.error.collectAsState()
                val saveSuccess by editProfileViewModel.saveSuccess.collectAsState()
                val isDeletingAccount by editProfileViewModel.isDeletingAccount.collectAsState()
                val deleteAccountError by editProfileViewModel.deleteAccountError.collectAsState()
                val context = LocalContext.current

                EditProfileScreen(
                    initialName = user?.name ?: "",
                    email = user?.email ?: "",
                    avatarUrl = user?.image,
                    isLoading = isLoading,
                    error = error,
                    saveSuccess = saveSuccess,
                    isDeletingAccount = isDeletingAccount,
                    deleteAccountError = deleteAccountError,
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
                    onChangePasswordClick = {
                        navController.navigate(ChangePasswordRoute)
                    },
                    onDeleteAccount = { editProfileViewModel.deleteAccount() },
                    onClearDeleteAccountError = {
                        editProfileViewModel.clearDeleteAccountError()
                    }
                )
            }

            composable<ChangePasswordRoute> {
                val changePasswordViewModel: ChangePasswordViewModel = hiltViewModel()
                ChangePasswordScreen(
                    viewModel = changePasswordViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<AppearanceRoute> {
                AppearanceScreen(
                    currentMode = reportDisplayMode,
                    onModeSelected = { mode -> viewModel.setDisplayMode(mode) },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
