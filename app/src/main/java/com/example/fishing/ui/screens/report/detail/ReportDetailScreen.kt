package com.example.fishing.ui.screens.report.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.components.*
import com.example.fishing.ui.theme.FishingTheme
import org.osmdroid.util.GeoPoint
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    report: FishingReport,
    onBackClick: () -> Unit,
    onMapClick: (GeoPoint) -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { }, // Title is now in the content
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.favorites),
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.menu),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                onClick = { menuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.Outlined.BorderColor, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (report.isPublic) stringResource(R.string.make_private) else stringResource(R.string.make_public)) },
                                onClick = { menuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Lock, contentDescription = null)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }

                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Шапка отчета (Фото карусель + Заголовок, Дата, Статус)
            ReportHeader(report = report)
            
            // 2. Описание отчета
            ReportDescriptionSection(report = report)

            // 3. Баннер публикации (если черновик)
            if (!report.isPublic) {
                PublishBanner()
            }

            // 4. Секция местоположения
            ReportLocationSection(
                report = report,
                onMapClick = {
                    onMapClick(GeoPoint(report.water.latitude, report.water.longitude))
                }
            )
            
            // 5. Детальная информация в виде сетки
            ReportInfoGrid(report = report)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailLoadingScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = { Text(text = stringResource(R.string.loading)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true, name = "Обычный отчет")
@Composable
fun ReportDetailScreenPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val sampleUser = User(name = "Виталий", image = "", email = "vital@example.com")
        val sampleReport = FishingReport(
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Смеркалось",
            water = Water(waterName = "Минское Море", latitude = 54.32344, longitude = 54.23425),
            photo = emptyList(),
            fishingTime = Date(),
            weight = 3.2,
            fish = listOf(
                Fish(name = "Карась", count = 2),
                Fish(name = "Окунь", count = 2)
            ),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(Bait.BLOODWORM, Bait.MAGGOT),
            comment = "В этот раз разведал неглубокую часть водохранилища и поймал парочку красивых рыб!",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = false
        )
        ReportDetailScreen(report = sampleReport, onBackClick = {})
    }
}

@Preview(showBackground = true, name = "Трофей")
@Composable
fun ReportDetailScreenTrophyPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val sampleUser = User(name = "Виталий", image = "", email = "vital@example.com")
        val sampleReport = FishingReport(
            userId = UUID.randomUUID(),
            type = FishingType.HAUL,
            name = "Тот самый улов!",
            water = Water(waterName = "Неман", latitude = 53.9, longitude = 25.3),
            photo = emptyList(),
            fishingTime = Date(),
            weight = 12.5,
            fish = listOf(
                Fish(name = "Щука", count = 1),
                Fish(name = "Сом", count = 1)
            ),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Наконец-то поймал свой трофей! Щука на 8.5 кг и сом на 4 кг. Взяли на воблер, поклёвка была мощная.",
            user = sampleUser,
            fishingFromTheShore = false,
            isPublic = false
        )
        ReportDetailScreen(report = sampleReport, onBackClick = {})
    }
}
