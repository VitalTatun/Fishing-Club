package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.*
import com.example.fishing.ui.components.FishingReportItem
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.MainViewModel
import java.util.*

sealed class BottomNavItem(val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("Главная", Icons.Default.Home)
    object Map : BottomNavItem("Карта", Icons.Default.Map)
    object Profile : BottomNavItem("Профиль", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    reports: List<FishingReport>,
    allReports: List<FishingReport> = reports,
    isLoading: Boolean = false,
    selectedTab: Int = 0,
    viewModel: MainViewModel? = null,
    onTabSelected: (Int) -> Unit = {},
    onCreateReportClick: () -> Unit = {},
    onReportClick: (FishingReport) -> Unit,
    onDeleteReport: (FishingReport) -> Unit = {},
    onSearchClick: () -> Unit = {},
    userEmail: String? = null,
    onLogout: () -> Unit = {},
    errorText: String? = null,
    onErrorDismiss: () -> Unit = {},
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Map,
        BottomNavItem.Profile
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedTab) {
                            1 -> "Карта"
                            else -> "Fishing Journal"
                        },
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                        IconButton(onClick = onCreateReportClick) {
                            Icon(Icons.Default.Add, contentDescription = "Новый отчет")
                        }
                    }
                    if (selectedTab == 1) {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
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
                            Text("Повторить")
                        }
                    }
                ) {
                    Text(errorText)
                }
            }
            when (selectedTab) {
                0 -> {
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { viewModel?.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (reports.isEmpty() && !isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Нет отчетов",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            itemsIndexed(reports) { index, report ->
                                FishingReportItem(report = report, onClick = { onReportClick(report) }, onDeleteReport = onDeleteReport)
                                if (index < reports.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        thickness = 1.dp,
                                        color = Color.LightGray.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    MapScreen(
                        reports = allReports,
                        onReportClick = onReportClick,
                        viewModel = viewModel
                    )
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (userEmail != null) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.titleMedium,
                                color = CreateReportColors.OnSurface
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        OutlinedButton(
                            onClick = onLogout,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CreateReportColors.OnSurface
                            )
                        ) {
                            Text("Выйти")
                        }
                    }
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
                fishingTime = calendar.apply { set(2023, Calendar.AUGUST, 22) }.time,
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
                fishingTime = calendar.apply { set(2024, Calendar.MAY, 1) }.time,
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
            reports = sampleReports,
            onReportClick = {}
        )
    }
}
