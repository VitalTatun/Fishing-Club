package com.example.fishing.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.components.FishingReportCardItem
import com.example.fishing.ui.components.FishingReportListItem
import com.example.fishing.ui.components.HomeReportListItem
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.MainViewModel
import com.example.fishing.viewmodel.HomeUiState
import com.example.fishing.ui.screens.map.MapScreen
import com.example.fishing.ui.screens.profile.ProfileScreen
import kotlinx.coroutines.delay
import java.util.*

sealed class BottomNavItem(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home)
    object Map : BottomNavItem(R.string.tab_map, Icons.Filled.Map, Icons.Outlined.Map)
    object Profile : BottomNavItem(R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person)
}

private const val PREFETCH_THRESHOLD = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeUiState: HomeUiState,
    favoriteReports: List<FishingReport> = emptyList(),
    mapMarkers: List<MarkerDomain> = emptyList(),
    isInitialLoading: Boolean = false,
    isRefreshing: Boolean = false,
    isLoadingMore: Boolean = false,
    hasMoreReports: Boolean = false,
    mapIsLoading: Boolean = false,
    mapIsRefreshing: Boolean = false,
    mapErrorMessage: String? = null,
    selectedTab: Int = 0,
    viewModel: MainViewModel? = null,
    repository: com.example.fishing.data.FishingRepository,
    onTabSelected: (Int) -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onReportClick: (FishingReport) -> Unit,
    onSearchClick: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    userEmail: String? = null,
    userName: String? = null,
    userImage: String? = null,
    currentUserId: UUID? = null,
    onLogout: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onChangeHistoryClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    reportDisplayMode: ReportDisplayMode = ReportDisplayMode.CARD,
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
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Map,
        BottomNavItem.Profile
    )

    val highlightedPolygon by viewModel?.highlightedPolygon?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentSortOrder by viewModel?.reportSortOrder?.collectAsState() ?: remember { mutableStateOf(ReportSortOrder.BY_FISHING_TIME) }
    
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedTab) {
                            1 -> stringResource(R.string.tab_map)
                            2 -> stringResource(R.string.tab_profile)
                            else -> stringResource(R.string.fishing_journal)
                        },
                    )
                },
                actions = {
                    if (selectedTab == 0) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.Default.SwapVert,
                                    contentDescription = stringResource(R.string.sort)
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_by_publish_date)) },
                                    onClick = {
                                        viewModel?.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (currentSortOrder == ReportSortOrder.BY_PUBLISH_DATE) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_by_fishing_time)) },
                                    onClick = {
                                        viewModel?.setSortOrder(ReportSortOrder.BY_FISHING_TIME)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (currentSortOrder == ReportSortOrder.BY_FISHING_TIME) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        }
                        IconButton(onClick = onCreateReportClick) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_report))
                        }
                    }
                    if (selectedTab == 1) {
                        if (highlightedPolygon != null) {
                            FilledTonalIconButton(
                                onClick = { viewModel?.setHighlightedPolygon(null) },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            val isSelected = selectedTab == index
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.titleRes)
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) },
                        selected = selectedTab == index,
                        onClick = {
                            onTabSelected(index)
                        }
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> {
                    when (homeUiState) {
                        is HomeUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is HomeUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = homeUiState.message,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(onClick = { viewModel?.refresh() }) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                        is HomeUiState.Empty -> {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel?.refresh() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_reports_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is HomeUiState.Success -> {
                            val myReports = remember(homeUiState.reports, currentUserId) {
                                homeUiState.reports.filter { currentUserId == null || it.userId == currentUserId }
                            }
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel?.refresh() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Избранные убраны с главного экрана. Доступ через поиск с фильтром.

                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                myReports.forEachIndexed { index, report ->
                                                    if (hasMoreReports &&
                                                        index >= myReports.size - PREFETCH_THRESHOLD
                                                    ) {
                                                        LaunchedEffect(index) { onLoadMore() }
                                                    }
                                                    HomeReportListItem(
                                                        report = report,
                                                        currentUserId = currentUserId,
                                                        onClick = { onReportClick(report) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (isLoadingMore) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    val mainMapFavoriteReports = viewModel?.filterFavoriteReports?.collectAsState()?.value ?: emptyList()
                    val mainMapIsFavoritesSelected = viewModel?.mapIsFavoritesSelected ?: false
                    LaunchedEffect(Unit) {
                        viewModel?.loadFilterFavoriteReports()
                    }
                    LaunchedEffect(mainMapIsFavoritesSelected) {
                        if (mainMapIsFavoritesSelected) {
                            viewModel?.loadFilterFavoriteReports(force = true)
                        }
                    }
                    MapScreen(
                        markers = mapMarkers,
                        viewModel = viewModel,
                        favoriteReports = mainMapFavoriteReports,

                        onMarkerClick = { marker ->
                            onReportClick(FishingReport(
                                id = marker.id,
                                userId = UUID.randomUUID(),
                                type = marker.type,
                                name = marker.name,
                                water = Water(
                                    waterName = marker.waterName,
                                    latitude = marker.waterLat,
                                    longitude = marker.waterLng
                                ),
                                photos = emptyList(),
                                fishingStartAt = marker.fishingStartAt,
                                weight = 0.0,
                                fish = emptyList(),
                                fishingMethod = marker.fishingMethod,
                                bait = emptyList(),
                                comment = "",
                                user = User(name = "", email = "", image = ""),
                                fishingFromTheShore = true,
                                isPublic = marker.isPublic
                            ))
                        },
                        repository = repository,
                        isLoading = mapIsLoading,
                        isRefreshing = mapIsRefreshing,
                        errorMessage = mapErrorMessage,
                        onRetry = { viewModel?.loadMapMarkers(force = true) }
                    )
                }
                2 -> {
                    ProfileScreen(
                        userEmail = userEmail,
                        userName = userName,
                        avatarUrl = userImage,
                        onEditClick = onEditProfileClick,
                        onLogoutClick = onLogout,
                        onChangeHistoryClick = onChangeHistoryClick,
                        onAppearanceClick = onAppearanceClick
                    )
                }
            }

            if (errorText != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    action = {
                        TextButton(onClick = onErrorDismiss) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                ) {
                    Text(errorText)
                }
            }
            if (deleteErrorText != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp, start = 8.dp, end = 8.dp),
                    action = {
                        TextButton(onClick = onDeleteErrorDismiss) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                ) {
                    Text(deleteErrorText)
                }
            }
            if (favoriteErrorText != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 116.dp, start = 8.dp, end = 8.dp),
                    action = {
                        TextButton(onClick = onFavoriteErrorDismiss) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                ) {
                    Text(favoriteErrorText)
                }
            }
            if (likeErrorText != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 168.dp, start = 8.dp, end = 8.dp),
                    action = {
                        TextButton(onClick = onLikeErrorDismiss) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                ) {
                    Text(likeErrorText)
                }
            }

            pendingUploads.firstOrNull()?.let { upload ->
                ReportUploadBanner(
                    upload = upload,
                    extraCount = (pendingUploads.size - 1).coerceAtLeast(0),
                    onRetry = { onRetryPendingUpload(upload.reportId) },
                    onCancel = { onCancelPendingUpload(upload.reportId) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                )
            }

            if (uploadNotice != null) {
                val noticeText = when (uploadNotice) {
                    is ReportUploadNotice.Published -> stringResource(
                        if (uploadNotice.isPublic) R.string.report_published_notice
                        else R.string.report_draft_saved_notice
                    )
                    is ReportUploadNotice.Failed ->
                        uploadNotice.message.ifBlank { stringResource(R.string.error_save_report) }
                }
                LaunchedEffect(uploadNotice) {
                    delay(4_000)
                    onUploadNoticeConsumed()
                }
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = if (pendingUploads.isNotEmpty()) 92.dp else 8.dp
                        ),
                    action = {
                        TextButton(onClick = onUploadNoticeConsumed) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                ) {
                    Text(noticeText)
                }
            }
        }
    }
}

@Composable
private fun ReportUploadBanner(
    upload: PendingReportUpload,
    extraCount: Int,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = when (upload.state) {
        ReportUploadState.FAILED -> stringResource(R.string.upload_banner_failed)
        ReportUploadState.QUEUED -> stringResource(R.string.upload_banner_queued)
        ReportUploadState.UPLOADING ->
            if (upload.totalPhotos > 0 && upload.uploadedPhotos < upload.totalPhotos) {
                stringResource(
                    R.string.upload_banner_uploading,
                    upload.uploadedPhotos,
                    upload.totalPhotos
                )
            } else {
                stringResource(R.string.upload_banner_publishing)
            }
    }
    val isFailed = upload.state == ReportUploadState.FAILED

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isFailed) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isFailed) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isFailed) {
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.delete))
                    }
                } else {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
            if (extraCount > 0) {
                Text(
                    text = stringResource(R.string.upload_banner_more, extraCount),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 800)
@Composable
fun MainScreenPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val myUserId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val sampleUser = User(id = myUserId, name = "Виталий", image = "", email = "vital@example.com")
        val otherUser = User(id = otherUserId, name = "Никита Белозерцев", image = "", email = "nikita@example.com")
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

        val bookmarkReport = FishingReport(
            id = UUID.randomUUID(),
            userId = otherUserId,
            type = FishingType.FISHING_LOG,
            name = "Поплавок • Карась",
            water = Water(waterName = "Заславское водохранилище", latitude = 0.0, longitude = 0.0),
            photos = emptyList(),
            fishingStartAt = calendar.apply { set(2026, Calendar.AUGUST, 18) }.time.toInstant(),
            fishingEndAt = calendar.apply { set(2026, Calendar.AUGUST, 18) }.time.toInstant().plusSeconds(3600 * 3),
            weight = 2.0,
            fish = listOf(Fish(id = UUID.randomUUID(), name = "Карась", count = 2)),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(Bait.WORM),
            comment = "Отличная рыбалка",
            user = otherUser,
            fishingFromTheShore = true,
            isPublic = true
        )

        MainScreen(
            homeUiState = HomeUiState.Success(listOf(myReport)),

            currentUserId = myUserId,
            userEmail = sampleUser.email,
            userName = sampleUser.name,
            userImage = sampleUser.image,
            repository = com.example.fishing.data.MockFishingRepository(),
            onReportClick = {}
        )
    }
}
