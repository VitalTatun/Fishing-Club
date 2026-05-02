package com.example.fishing.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.util.*

// 1. Модуль заголовка отчета (Фото + Инфо)
@Composable
fun ReportHeader(report: FishingReport, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        )
    {
        // Фото карусель
        ReportPhotoCarousel(photos = report.photo)

        Column(
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            // Основная информация (Теги + Автор/Название)
            ReportSummary(report = report)


        }
        // Баннер публикации (если черновик)
        if (!report.isPublic) {
            PublishBanner()
        }
    }
}

// 2. Фото Карусель
@Composable
fun ReportPhotoCarousel(photos: List<Int>, modifier: Modifier = Modifier) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState { photos.size }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            pageSpacing = 8.dp
        ) { index ->
            Image(
                painter = painterResource(id = photos[index]),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Счетчик страниц
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 20.dp),
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "${pagerState.currentPage + 1} из ${photos.size}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun ReportSummary(report: FishingReport, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportStatusTags(report = report)
        ReportInfoRow(report = report)
    }
}

@Composable
fun ReportInfoRow(report: FishingReport, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Аватар пользователя
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .border(2.dp, Color.White, CircleShape)
                .border(1.dp, Color.Gray.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Вертикальный стек (Название + Имя)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = report.user.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }

        if (!report.isPublic) {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Опубликовать",
                )
            }
        }
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = "Сохранить",
            )
        }
    }
}

@Composable
fun ReportStatusTags(report: FishingReport, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(
            text = report.type.displayName,
            containerColor = Color(0xFFE8EAF6),
            contentColor = Color(0xFF3F51B5)
        )
        if (report.isPublic) {
            StatusChip(
                text = "Опубликован",
                containerColor = Color(0xFFDCEDC8),
                contentColor = Color(0xFF689F38)
            )
        } else {
            StatusChip(
                text = "Не опубликован",
                containerColor = Color(0xFFF5F5F5),
                contentColor = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun StatusChip(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = contentColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportHeaderPreview() {
    FishingTheme {
        val sampleReport = FishingReport(
            type = FishingType.FISHING_LOG,
            name = "Смеркалось",
            water = Water(waterName = "Минское Море", latitude = 54.32344, longitude = 54.23425),
            photo = listOf(android.R.drawable.ic_menu_gallery),
            fishingTime = Date(),
            weight = 3.2,
            fish = listOf(),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(),
            comment = "",
            user = User(name = "Никита Белозерцев", image = "", email = ""),
            fishingFromTheShore = true,
            isPublic = false
        )
        Column(modifier = Modifier.padding(16.dp)) {
            ReportHeader(report = sampleReport)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ReportHeader(report = sampleReport.copy(isPublic = true))
        }
    }
}
