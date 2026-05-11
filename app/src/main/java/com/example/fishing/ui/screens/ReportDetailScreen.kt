package com.example.fishing.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.components.*
import com.example.fishing.ui.theme.FishingTheme
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    report: FishingReport,
    onBackClick: () -> Unit,
    onMapClick: (GeoPoint) -> Unit = {}
) {
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("ru"))
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Column {
                        Text(
                            text = report.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateFormatter.format(report.fishingTime),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Редактировать */ }) {
                        Icon(
                            imageVector = Icons.Outlined.BorderColor,
                            contentDescription = "Редактировать",
//                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { /* TODO: Удалить */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Удалить",
//                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Сохранить",
                            tint = Color.Red
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
                .padding(bottom = 32.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Шапка отчета (Фото карусель + Заголовок, Дата, Статус)
            ReportHeader(report = report)
            MoodSection(selectedMood = 5)
            // Баннер публикации (если черновик)
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
            // Общая информация (Mood, Details, Catch)
            ReportGeneralInfo(report = report) 
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportDetailScreenPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val sampleUser = User(name = "Виталий", image = "", email = "vital@example.com")
        val sampleReport = FishingReport(
            type = FishingType.FISHING_LOG,
            name = "Смеркалось",
            water = Water(waterName = "Минское Море", latitude = 54.32344, longitude = 54.23425),
            photo = listOf(R.drawable.example),
            fishingTime = Date(),
            weight = 3.2,
            fish = listOf(
                Fish(name = "Карась", count = 2),
                Fish(name = "Окунь", count = 2),
                Fish(name = "Лещ", count = 2),
                Fish(name = "Подлещик", count = 2)
            ),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(Bait.BLOODWORM, Bait.MAGGOT),
            comment = "В этот раз разведал неглубокую часть водохранилища и поймал парочку красивых рыб! Замешав вечерком плотву с орехом от Feeder.by с утра поехал на мелководную часть вдх посмотреть как там обстоят дела с рыбкой...",
            user = sampleUser,
            fishingFromTheShore = true,
            isPublic = false
        )
        ReportDetailScreen(report = sampleReport, onBackClick = {})
    }
}
