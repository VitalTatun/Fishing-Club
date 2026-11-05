package com.example.fishing.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishingReport
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.PendingReportUpload
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.model.ReportUploadNotice
import com.example.fishing.model.ReportUploadState
import com.example.fishing.model.Water
import com.example.fishing.model.User
import com.example.fishing.ui.screens.map.MapScreen
import com.example.fishing.ui.screens.profile.ProfileScreen
import com.example.fishing.viewmodel.HomeUiState
import com.example.fishing.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
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
    highlightedPolygon: List<GeoPoint>? = null,
    currentSortOrder: ReportSortOrder = ReportSortOrder.BY_FISHING_TIME,
    viewModel: MainViewModel? = null,
    onTabSelected: (Int) -> Unit = {},
    onSetSortOrder: (ReportSortOrder) -> Unit = {},
    onClearHighlightedPolygon: () -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onReportClick: (FishingReport) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRetryLoadMore: () -> Unit = onLoadMore,
    onRefresh: () -> Unit = {},
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
    pendingUploads: List<PendingReportUpload> = emptyList(),
    uploadNotice: ReportUploadNotice? = null,
    onRetryPendingUpload: (UUID) -> Unit = {},
    onCancelPendingUpload: (UUID) -> Unit = {},
    onUploadNoticeConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Map,
            BottomNavItem.Profile
        )
    }

    val pagerState = rememberPagerState(initialPage = selectedTab) { items.size }
    val coroutineScope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Sync external selectedTab with pagerState
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.scrollToPage(selectedTab)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (pagerState.currentPage) {
                            1 -> stringResource(R.string.tab_map)
                            2 -> stringResource(R.string.tab_profile)
                            else -> stringResource(R.string.fishing_journal)
                        }
                    )
                },
                actions = {
                    if (pagerState.currentPage == 0) {
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
                                        onSetSortOrder(ReportSortOrder.BY_PUBLISH_DATE)
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
                                        onSetSortOrder(ReportSortOrder.BY_FISHING_TIME)
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
                    if (pagerState.currentPage == 1) {
                        if (highlightedPolygon != null) {
                            FilledTonalIconButton(
                                onClick = onClearHighlightedPolygon,
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
                    val isSelected = pagerState.currentPage == index
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.titleRes)
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) },
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.scrollToPage(index)
                            }
                            onTabSelected(index)
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        HomeTabContent(
                            homeUiState = homeUiState,
                            currentUserId = currentUserId,
                            isRefreshing = isRefreshing,
                            isLoadingMore = isLoadingMore,
                            hasMoreReports = hasMoreReports,
                            loadMoreError = loadMoreError,
                            onRetryLoadMore = onRetryLoadMore,
                            onRefresh = onRefresh,
                            onLoadMore = onLoadMore,
                            onReportClick = onReportClick
                        )
                    }
                    1 -> {
                        MapScreen(
                            markers = mapMarkers,
                            viewModel = viewModel,
                            onMarkerClick = { marker ->
                                onReportClick(
                                    FishingReport(
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
                                    )
                                )
                            },
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
