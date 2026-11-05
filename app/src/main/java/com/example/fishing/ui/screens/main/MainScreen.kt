package com.example.fishing.ui.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.fishing.R
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.PendingReportUpload
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.model.ReportUploadNotice
import com.example.fishing.model.User
import com.example.fishing.model.Water
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.HomeUiState
import com.example.fishing.viewmodel.MainViewModel
import java.util.Calendar
import java.util.UUID

sealed class BottomNavItem(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home)
    object Map : BottomNavItem(R.string.tab_map, Icons.Filled.Map, Icons.Outlined.Map)
    object Profile : BottomNavItem(R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainScreen(
    homeUiState: HomeUiState,
    mapMarkers: List<MarkerDomain> = emptyList(),
    isRefreshing: Boolean = false,
    isLoadingMore: Boolean = false,
    hasMoreReports: Boolean = false,
    loadMoreError: String? = null,
    mapIsLoading: Boolean = false,
    mapIsRefreshing: Boolean = false,
    mapErrorMessage: String? = null,
    selectedTab: Int = 0,
    viewModel: MainViewModel? = null,
    repository: FishingRepository? = null,
    onTabSelected: (Int) -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onReportClick: (FishingReport) -> Unit,
    onSearchClick: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRetryLoadMore: () -> Unit = onLoadMore,
    userEmail: String? = null,
    userName: String? = null,
    userImage: String? = null,
    currentUserId: UUID? = null,
    onLogout: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onChangeHistoryClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    errorText: String? = null,
    onErrorDismiss: () -> Unit = {},
    deleteErrorText: String? = null,
    onDeleteErrorDismiss: () -> Unit = {},
    favoriteErrorText: String? = null,
    onFavoriteErrorDismiss: () -> Unit = {},
    likeErrorText: String? = null,
    onLikeErrorDismiss: () -> Unit = {},
    pendingUploads: List<PendingReportUpload> = emptyList(),
    uploadNotice: ReportUploadNotice? = null,
    onRetryPendingUpload: (UUID) -> Unit = {},
    onCancelPendingUpload: (UUID) -> Unit = {},
    onUploadNoticeConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val highlightedPolygon by viewModel?.highlightedPolygon?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentSortOrder by viewModel?.reportSortOrder?.collectAsState() ?: remember { mutableStateOf(ReportSortOrder.BY_FISHING_TIME) }

    MainShell(
        homeUiState = homeUiState,
        mapMarkers = mapMarkers,
        isRefreshing = isRefreshing,
        isLoadingMore = isLoadingMore,
        hasMoreReports = hasMoreReports,
        loadMoreError = loadMoreError,
        mapIsLoading = mapIsLoading,
        mapIsRefreshing = mapIsRefreshing,
        mapErrorMessage = mapErrorMessage,
        selectedTab = selectedTab,
        highlightedPolygon = highlightedPolygon,
        currentSortOrder = currentSortOrder,
        viewModel = viewModel,
        onTabSelected = onTabSelected,
        onSetSortOrder = { order -> viewModel?.setSortOrder(order) },
        onClearHighlightedPolygon = { viewModel?.setHighlightedPolygon(null) },
        onCreateReportClick = onCreateReportClick,
        onReportClick = onReportClick,
        onSearchClick = onSearchClick,
        onLoadMore = onLoadMore,
        onRetryLoadMore = onRetryLoadMore,
        onRefresh = { viewModel?.refresh() },
        userEmail = userEmail,
        userName = userName,
        userImage = userImage,
        currentUserId = currentUserId,
        onLogout = onLogout,
        onEditProfileClick = onEditProfileClick,
        onChangeHistoryClick = onChangeHistoryClick,
        onAppearanceClick = onAppearanceClick,
        errorText = errorText,
        onErrorDismiss = onErrorDismiss,
        pendingUploads = pendingUploads,
        uploadNotice = uploadNotice,
        onRetryPendingUpload = onRetryPendingUpload,
        onCancelPendingUpload = onCancelPendingUpload,
        onUploadNoticeConsumed = onUploadNoticeConsumed,
        modifier = modifier
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 800)
@Composable
fun MainScreenPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val myUserId = UUID.randomUUID()
        val sampleUser = User(id = myUserId, name = "Виталий", image = "", email = "vital@example.com")
        val calendar = Calendar.getInstance()

        val myReport = FishingReport(
            id = UUID.randomUUID(),
            userId = myUserId,
            type = FishingType.FISHING_LOG,
            name = "Смеркалось...",
            water = Water(waterName = "Водохранилище Крылово", latitude = 0.0, longitude = 0.0),
            photos = emptyList(),
            fishingStartAt = calendar.apply { set(2023, Calendar.AUGUST, 22) }.time.toInstant(),
            fishingEndAt = calendar.apply { set(2023, Calendar.AUGUST, 22) }.time.toInstant().plusSeconds(3600 * 3),
            weight = 1.2,
            fish = listOf(Fish(id = UUID.randomUUID(), name = "Окунь", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Ловил на джиг, глубина 5 метров.",
            user = sampleUser,
            fishingFromTheShore = false,
            isPublic = false
        )

        MainScreen(
            homeUiState = HomeUiState.Success(listOf(myReport)),
            currentUserId = myUserId,
            userEmail = sampleUser.email,
            userName = sampleUser.name,
            userImage = sampleUser.image,
            onReportClick = {}
        )
    }
}
