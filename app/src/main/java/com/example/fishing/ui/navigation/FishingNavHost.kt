package com.example.fishing.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.example.fishing.viewmodel.LocationSearchViewModel
import com.example.fishing.ui.screens.search.ReportSearchScreen
import com.example.fishing.ui.screens.user.UserProfileScreen
import com.example.fishing.ui.screens.login.LoginScreen
import com.example.fishing.ui.screens.login.RegistrationScreen
import com.example.fishing.ui.screens.map.MapFiltersScreen
import com.example.fishing.ui.screens.map.MapScreen
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

@Composable
fun FishingNavHost(
    authState: AuthState,
    fishingRepository: FishingRepository,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navigator = remember(backStack) { FishingNavigator(backStack) }

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
    val mapErrorMessage by viewModel.mapRefreshError.collectAsStateWithLifecycle()
    val mapFilters by viewModel.mapFilters.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val reportDisplayMode by viewModel.reportDisplayMode.collectAsStateWithLifecycle()
    val pendingUploads by viewModel.pendingUploads.collectAsStateWithLifecycle()
    val uploadNotice by viewModel.uploadNotice.collectAsStateWithLifecycle()

    val currentUser = (authState as? AuthState.Authenticated)?.user

    // BUG-01: the host is never recreated for an auth change. The effect moves the
    // back stack only when the current destination belongs to the signed out entry
    // points, so a stack restored after process death survives the session restore
    // (the report form reopened after the system camera).
    AuthNavigationEffect(authState = authState, backStack = backStack)

    NavDisplay(
        backStack = backStack,
        onBack = { navigator.popBackStack() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>()
        ),
        transitionSpec = {
            (slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { -it / 7 },
                    animationSpec = tween(300)
                ) + fadeOut(tween(300)))
        },
        popTransitionSpec = {
            (slideInHorizontally(
                initialOffsetX = { -it / 7 },
                animationSpec = tween(300)
            ) + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(tween(300)))
        },
        predictivePopTransitionSpec = {
            (slideInHorizontally(
                initialOffsetX = { -it / 7 },
                animationSpec = tween(300)
            ) + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(tween(300)))
        },
        entryProvider = entryProvider {
            // Deliberately empty: while the session is being restored the activity
            // draws the progress indicator over it, so the host never leaves the
            // composition and a restored back stack is kept.
            entry<SplashRoute> { }

            entry<LoginRoute> {
                val loginViewModel: LoginViewModel = hiltViewModel()
                LoginScreen(
                    viewModel = loginViewModel,
                    onNavigateToRegistration = {
                        navigator.navigate(RegistrationRoute)
                    }
                )
            }

            entry<RegistrationRoute> {
                val registrationViewModel: RegistrationViewModel = hiltViewModel()
                RegistrationScreen(
                    viewModel = registrationViewModel,
                    onBackClick = { navigator.popBackStack() },
                    onConfirmedLogin = { navigator.popBackStack() }
                )
            }

            entry<MainRoute> {
                MainScreen(
                    homeUiState = homeUiState,
                    isRefreshing = isRefreshing,
                    isLoadingMore = isLoadingMore,
                    loadMoreError = loadMoreError,
                    hasMoreReports = hasMoreReports,
                    onLoadMore = { viewModel.loadMoreReports() },
                    onRetryLoadMore = { viewModel.loadMoreReports() },
                    mapIsLoading = mapIsLoading,
                    mapErrorMessage = mapErrorMessage,
                    mapFiltersActiveCount = mapFilters.activeCount,
                    selectedTab = selectedTab,
                    mapMarkers = mapMarkers,
                    viewModel = viewModel,
                    repository = fishingRepository,
                    onTabSelected = { index -> viewModel.selectTab(index) },
                    onCreateReportClick = {
                        navigator.navigate(CreateReportRoute)
                    },
                    onReportClick = { report ->
                        navigator.navigate(ReportDetailRoute(report.id.toString()))
                    },
                    onSearchClick = {
                        if (selectedTab == 0) {
                            navigator.navigate(ReportListSearchRoute)
                        } else {
                            navigator.navigate(LocationSearchRoute())
                        }
                    },
                    onMapFiltersClick = {
                        navigator.navigate(MapFiltersRoute)
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
                        navigator.navigate(EditProfileRoute)
                    },
                    onChangeHistoryClick = {
                        navigator.navigate(ChangeHistoryRoute)
                    },
                    onAppearanceClick = {
                        navigator.navigate(AppearanceRoute)
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

            entry<MapFiltersRoute> {
                MapFiltersScreen(
                    initialFilters = mapFilters,
                    onApply = { filters ->
                        viewModel.applyMapFilters(filters)
                        navigator.popBackStack()
                    },
                    onBack = { navigator.popBackStack() }
                )
            }

            entry<ReportListSearchRoute> {
                val searchState by viewModel.searchState.collectAsStateWithLifecycle()
                // Favourites are per user: the load is a silent no-op without a session.
                SessionReadyEffect(searchState.filters.isFavoritesSelected, authState) {
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
                        navigator.navigate(ReportDetailRoute(report.id.toString()))
                    },
                    onBack = { navigator.popBackStack() },
                    favoriteError = viewModel.favoriteError.collectAsState().value,
                    onFavoriteErrorDismiss = { viewModel.clearFavoriteError() }
                )
            }

            entry<LocationSearchRoute> { route ->
                val locationSearchViewModel: LocationSearchViewModel = hiltViewModel()
                val locationSearchUiState by locationSearchViewModel.uiState.collectAsStateWithLifecycle()

                LocationSearchScreen(
                    state = locationSearchUiState,
                    onQueryChange = locationSearchViewModel::onQueryChanged,
                    onRetry = locationSearchViewModel::retry,
                    onLocationSelected = { point, _, polygon ->
                        if (route.fromWaterEdit) {
                            navigator.replaceCurrentAndPrevious(
                                FishingLocationRoute(
                                    searchLatitude = point.latitude,
                                    searchLongitude = point.longitude
                                )
                            )
                        } else {
                            viewModel.selectTab(1)
                            viewModel.requestMapLocation(point)
                            viewModel.setHighlightedPolygon(polygon)
                            navigator.popBackStack()
                        }
                    },
                    onBack = { navigator.popBackStack() }
                )
            }

            entry<CreateReportRoute> {
                CreateReportScreen(
                    viewModel = createReportViewModel,
                    onBackClick = { navigator.popBackStack() },
                    onSaveComplete = {
                        viewModel.refresh()
                        if (navigator.currentKey == CreateReportRoute) {
                            navigator.popBackStack()
                        }
                    },
                    onNavigateToCatchEdit = {
                        navigator.navigate(CatchEditRoute)
                    },
                    onNavigateToMethodAndBaitEdit = {
                        navigator.navigate(MethodBaitEditRoute)
                    },
                    onNavigateToCommentEdit = {
                        navigator.navigate(CommentEditRoute)
                    },
                    onNavigateToWaterEdit = {
                        navigator.navigate(FishingLocationRoute())
                    },
                    onNavigateToWaterNameEdit = {
                        navigator.navigate(WaterNameEditRoute)
                    },
                    onNavigateToFishingDateTime = {
                        navigator.navigate(FishingDateTimeRoute)
                    }
                )
            }

            entry<FishingDateTimeRoute> {
                FishingDateTimeScreen(
                    startDateText = createReportViewModel.formStartDate.orEmpty(),
                    startTimeText = createReportViewModel.formStartTime.orEmpty(),
                    endDateText = createReportViewModel.formEndDate.orEmpty(),
                    endTimeText = createReportViewModel.formEndTime.orEmpty(),
                    onBackClick = { navigator.popBackStack() },
                    onConfirm = { startDate, startTime, endDate, endTime ->
                        createReportViewModel.formStartDate = startDate
                        createReportViewModel.formStartTime = startTime
                        createReportViewModel.formEndDate = endDate
                        createReportViewModel.formEndTime = endTime
                        navigator.popBackStack()
                    },
                )
            }

            entry<FishingLocationRoute> { route ->
                val searchLocation = remember(route.searchLatitude, route.searchLongitude) {
                    if (route.searchLatitude != null && route.searchLongitude != null) {
                        GeoPoint(route.searchLatitude, route.searchLongitude)
                    } else null
                }
                FishingLocationScreen(
                    initialLocation = createReportViewModel.formLocation,
                    searchLocation = searchLocation,
                    onBackClick = { navigator.popBackStack() },
                    onSaveClick = { point ->
                        createReportViewModel.formLocation = point
                        navigator.popBackStack()
                    },
                    onSearchClick = {
                        navigator.navigate(LocationSearchRoute(fromWaterEdit = true))
                    }
                )
            }

            entry<WaterNameEditRoute> {
                WaterEditScreen(
                    viewModel = createReportViewModel,
                    reportsViewModel = viewModel,
                    onBackClick = { navigator.popBackStack() },
                    onSaveClick = { navigator.popBackStack() }
                )
            }

            entry<CommentEditRoute> {
                CommentEditScreen(
                    initialComment = createReportViewModel.formComment,
                    onBackClick = { navigator.popBackStack() },
                    onSaveClick = { comment ->
                        createReportViewModel.formComment = comment
                        navigator.popBackStack()
                    }
                )
            }

            entry<MethodBaitEditRoute> {
                FishingMethodAndBaitScreen(
                    initialMethod = createReportViewModel.formSelectedMethod,
                    initialBaits = createReportViewModel.formSelectedBaits,
                    onBackClick = { navigator.popBackStack() },
                    onSaveClick = { method, baits ->
                        createReportViewModel.formSelectedMethod = method
                        createReportViewModel.formSelectedBaits = baits
                        navigator.popBackStack()
                    }
                )
            }

            entry<CatchEditRoute> {
                val isTrophy = createReportViewModel.formReportType == FishingType.HAUL
                val catchSearchState by createReportViewModel.catchSearchState.collectAsStateWithLifecycle()
                val fishCatalog by createReportViewModel.fishCatalog.collectAsStateWithLifecycle()

                CatchEditScreen(
                    fishList = createReportViewModel.formSelectedFish,
                    catalog = fishCatalog,
                    searchState = catchSearchState,
                    onOpenSearch = createReportViewModel::openCatchSearch,
                    onCloseSearch = createReportViewModel::closeCatchSearch,
                    onSearchQueryChange = createReportViewModel::onCatchSearchQueryChange,
                    initialWeight = createReportViewModel.formWeight,
                    onBackClick = { navigator.popBackStack() },
                    onSaveClick = { fish, weight ->
                        createReportViewModel.formSelectedFish = fish
                        createReportViewModel.formWeight = weight
                        navigator.popBackStack()
                    },
                    isTrophy = isTrophy
                )
            }

            entry<ReportDetailRoute> { reportDetailRoute ->
                // Consume the back gesture so the system predictive preview is not
                // shown on this destination; the regular pop slide still plays.
                BackHandler { navigator.popBackStack() }

                val reportId = runCatching {
                    UUID.fromString(reportDetailRoute.reportId)
                }.getOrNull()

                // Without a session the store answers NoSession and the screen would stay
                // in Loading (the key no longer changes), so wait for the session.
                SessionReadyEffect(reportId, authState) {
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
                        navigator.popBackStack()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = reportDetailState) {
                        is ReportDetailUiState.Success ->
                            if (state.report.id == reportId) {
                                ReportDetailScreen(
                                    report = state.report,
                                    onBackClick = { navigator.popBackStack() },
                                    isFavorite = state.report.id in favoriteIds,
                                    isFavoriteUpdating = state.report.id in favoriteOpsInFlight,
                                    onToggleFavorite = { viewModel.toggleFavorite(state.report) },
                                    onMapClick = { point ->
                                        viewModel.requestMapLocation(point)
                                        navigator.navigate(FullMapRoute(state.report.id.toString()))
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
                                    onLikeErrorDismiss = { viewModel.clearLikeError() },
                                    // Opened from a user page: the author of this
                                    // report is that same user, so the row must not
                                    // push another profile onto the back stack.
                                    onUserClick = if (reportDetailRoute.allowAuthorNavigation) {
                                        {
                                            navigator.navigate(
                                                UserProfileRoute(
                                                    userId = state.report.userId.toString(),
                                                    userName = state.report.user.name,
                                                    userAvatar = state.report.user.image
                                                )
                                            )
                                        }
                                    } else {
                                        null
                                    }
                                )
                            } else {
                                ReportDetailLoadingScreen(
                                    onBackClick = { navigator.popBackStack() }
                                )
                            }

                        is ReportDetailUiState.Empty ->
                            if (state.reportId == reportId) {
                                ReportDetailUnavailableScreen(
                                    onBackClick = { navigator.popBackStack() }
                                )
                            } else {
                                ReportDetailLoadingScreen(
                                    onBackClick = { navigator.popBackStack() }
                                )
                            }

                        is ReportDetailUiState.Error ->
                            if (state.reportId == reportId) {
                                ReportDetailErrorScreen(
                                    message = state.message,
                                    onBackClick = { navigator.popBackStack() },
                                    onRetry = { reportId?.let(viewModel::loadReportDetails) }
                                )
                            } else {
                                ReportDetailLoadingScreen(
                                    onBackClick = { navigator.popBackStack() }
                                )
                            }

                        ReportDetailUiState.Loading ->
                            ReportDetailLoadingScreen(
                                onBackClick = { navigator.popBackStack() }
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

            entry<FullMapRoute> { fullMapRoute ->
                val reportId = runCatching {
                    UUID.fromString(fullMapRoute.reportId)
                }.getOrNull()

                val fullMapDetailState by viewModel.reportDetailUiState
                    .collectAsStateWithLifecycle()
                SessionReadyEffect(reportId, authState) {
                    reportId?.let(viewModel::loadReportDetails)
                }
                // Full-map does not depend on the viewport cache: the marker is
                // built from the report itself, even when it is outside the
                // currently loaded map area.
                val singleMarker = (fullMapDetailState as? ReportDetailUiState.Success)
                    ?.report
                    ?.takeIf { it.id == reportId }
                    ?.toMarkerDomain()

                MapScreen(
                    markers = singleMarker?.let(::listOf).orEmpty(),
                    viewModel = viewModel,
                    onBackClick = {
                        navigator.popBackStack()
                    },
                    onMarkerClick = { marker ->
                        navigator.replaceCurrent(ReportDetailRoute(marker.id.toString()))
                    },
                    markersInteractive = false,
                    initialReportId = reportId,
                    selectedMarkerDotStyle = true,
                    repository = fishingRepository,
                    viewportMode = false
                )
            }

            entry<UserProfileRoute> { userProfileRoute ->
                val profileUserId = runCatching {
                    UUID.fromString(userProfileRoute.userId)
                }.getOrNull()

                // The profile page is fetched from the server, which needs the session.
                SessionReadyEffect(profileUserId, authState) {
                    profileUserId?.let(viewModel::loadUserProfile)
                }

                val userProfileState by viewModel.userProfileUiState.collectAsStateWithLifecycle()
                val userProfileSortOrder by viewModel.userProfileSortOrder
                    .collectAsStateWithLifecycle()
                val userProfileCanChangeSortOrder by viewModel.userProfileCanChangeSortOrder
                    .collectAsStateWithLifecycle()

                UserProfileScreen(
                    uiState = userProfileState,
                    sortOrder = userProfileSortOrder,
                    canChangeSortOrder = userProfileCanChangeSortOrder,
                    fallbackUserName = userProfileRoute.userName,
                    fallbackUserAvatar = userProfileRoute.userAvatar,
                    onSetSortOrder = viewModel::setUserProfileSortOrder,
                    onBackClick = {
                        viewModel.clearUserProfile()
                        navigator.popBackStack()
                    },
                    onReportClick = { report ->
                        navigator.navigate(
                            ReportDetailRoute(
                                reportId = report.id.toString(),
                                // The author row of these details is this same page,
                                // so it must not push another profile on top.
                                allowAuthorNavigation = false
                            )
                        )
                    },
                    onRetry = { profileUserId?.let(viewModel::loadUserProfile) }
                )
            }

            entry<ChangeHistoryRoute> {
                ChangeHistoryScreen(
                    onBackClick = { navigator.popBackStack() }
                )
            }

            entry<EditProfileRoute> {
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
                    onBackClick = { navigator.popBackStack() },
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
                        navigator.navigate(ChangePasswordRoute)
                    },
                    onDeleteAccount = { editProfileViewModel.deleteAccount() },
                    onClearDeleteAccountError = {
                        editProfileViewModel.clearDeleteAccountError()
                    }
                )
            }

            entry<ChangePasswordRoute> {
                val changePasswordViewModel: ChangePasswordViewModel = hiltViewModel()
                ChangePasswordScreen(
                    viewModel = changePasswordViewModel,
                    onBackClick = { navigator.popBackStack() }
                )
            }

            entry<AppearanceRoute> {
                AppearanceScreen(
                    currentMode = reportDisplayMode,
                    onModeSelected = { mode -> viewModel.setDisplayMode(mode) },
                    onBackClick = { navigator.popBackStack() }
                )
            }
        }
    )
}
