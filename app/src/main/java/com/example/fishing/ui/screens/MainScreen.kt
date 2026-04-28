package com.example.fishing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.*
import com.example.fishing.ui.components.FishingReportItem
import com.example.fishing.ui.theme.FishingTheme
import java.util.*

sealed class BottomNavItem(val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("Главная", Icons.Default.Home)
    object Map : BottomNavItem("Карта", Icons.Default.Map)
    object Profile : BottomNavItem("Профиль", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(reports: List<FishingReport>, onReportClick: (FishingReport) -> Unit) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Map,
        BottomNavItem.Profile
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(items[selectedItem].title) }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedItem) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5)),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reports) { report ->
                            FishingReportItem(report = report, onClick = { onReportClick(report) })
                        }
                    }
                }
                1 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Экран карты")
                    }
                }
                2 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Экран профиля")
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
                type = FishingType.FISHING_LOG,
                name = "Смеркалось...",
                water = Water(waterName = "Водохранилище Крылово", latitude = 0.0, longitude = 0.0),
                photo = listOf(android.R.drawable.ic_menu_gallery),
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
        MainScreen(reports = sampleReports, onReportClick = {})
    }
}
