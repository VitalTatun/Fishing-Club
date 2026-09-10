package com.example.fishing.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.components.FishingReportItem
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.MainViewModel
import com.example.fishing.viewmodel.HomeUiState
import com.example.fishing.ui.screens.map.MapScreen
import com.example.fishing.ui.screens.profile.ProfileScreen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeUiState: HomeUiState,
    favoriteReports: List<FishingReport> = emptyList(),
    mapMarkers: List<MarkerDomain> = emptyList(),
    isInitialLoading: Boolean = false,
    isRefreshing: Boolean = false,
    mapIsLoading: Boolean = false,
    mapIsRefreshing: Boolean = false,
    mapErrorMessage: String? = null,
    selectedTab: Int = 0,
    viewModel: MainViewModel? = null,
    repository: com.example.fishing.data.FishingRepository,
    onTabSelected: (Int) -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onReportClick: (FishingReport) -> Unit,
    onDeleteReport: (FishingReport) -> Unit = {},
    onSearchClick: () -> Unit = {},
    userEmail: String? = null,
    userName: String? = null,
    userImage: String? = null,
    currentUserId: UUID? = null,
    onLogout: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onChangeHistoryClick: () -> Unit = {},
    errorText: String? = null,
    onErrorDismiss: () -> Unit = {},
    deleteErrorText: String? = null,
    onDeleteErrorDismiss: () -> Unit = {},
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
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel?.refresh() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(
                                        items = homeUiState.reports,
                                        key = { _, report -> report.id }
                                    ) { index, report ->
                                        FishingReportItem(
                                            report = report,
                                            onClick = { onReportClick(report) },
                                            onDeleteReport = onDeleteReport,
                                            isFavorite = favoriteReports.any { it.id == report.id },
                                            currentUserId = currentUserId
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    MapScreen(
                        markers = mapMarkers,
                        favoriteReports = favoriteReports,
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
                                photo = emptyList(),
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
                        viewModel = viewModel,
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
                        onChangeHistoryClick = onChangeHistoryClick
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val sampleUser = User(name = "Виталий", image = "", email = "vital@example.com")
        val calendar = Calendar.getInstance()

        val sampleReports = listOf(
            FishingReport(
                userId = UUID.randomUUID(),
                type = FishingType.FISHING_LOG,
                name = "Смеркалось...",
                water = Water(waterName = "Водохранилище Крылово", latitude = 0.0, longitude = 0.0),
                photo = emptyList(),
                fishingStartAt = calendar.apply { set(2023, Calendar.AUGUST, 22) }.time.toInstant(),
                fishingEndAt = calendar.apply { set(2023, Calendar.AUGUST, 22) }.time.toInstant().plusSeconds(3600 * 3),
                weight = 1.2,
                fish = listOf(Fish(name = "Окунь", count = 1)),
                fishingMethod = FishingMethod.SPINNING,
                bait = listOf(Bait.WOBBLER),
                comment = "Ловил на джиг, глубина 5 метров.",
                user = sampleUser,
                fishingFromTheShore = false,
                isPublic = false
            ),
            FishingReport(
                userId = UUID.randomUUID(),
                type = FishingType.FISHING_LOG,
                name = "Отчет без фото",
                water = Water(waterName = "Чистый пруд", latitude = 0.0, longitude = 0.0),
                photo = emptyList(),
                fishingStartAt = calendar.apply { set(2024, Calendar.MAY, 1) }.time.toInstant(),
                fishingEndAt = calendar.apply { set(2024, Calendar.MAY, 1) }.time.toInstant().plusSeconds(3600 * 3),
                weight = 0.5,
                fish = listOf(Fish(name = "Карась", count = 2)),
                fishingMethod = FishingMethod.BOBBER,
                bait = listOf(Bait.BREAD),
                comment = "Забыл телефон дома, фоток нет.",
                user = sampleUser,
                fishingFromTheShore = true,
                isPublic = true
            )
        )
        MainScreen(
            homeUiState = HomeUiState.Success(sampleReports),
            repository = com.example.fishing.data.MockFishingRepository(),
            onReportClick = {}
        )
    }
}
