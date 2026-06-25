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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.util.*

// 1. Модуль заголовка отчета (Фото + Инфо)
@Composable
fun ReportHeader(report: FishingReport, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
//            .background(Color.White),
//            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
        )
    {
        // Фото карусель
        ReportPhotoCarousel(photos = report.photo)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
            // Основная информация (Теги + Автор/Название)
            ReportSummary(report = report)
        }
    }
}

// 2. Фото Карусель
@Composable
fun ReportPhotoCarousel(photos: List<String>, modifier: Modifier = Modifier) {
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
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 8.dp
        ) { index ->
            AsyncImage(
                model = photos[index],
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }

        if (photos.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 24.dp),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(30.dp),
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${photos.size}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    )
                )
            }
        }
    }
}

@Composable
fun ReportSummary(report: FishingReport, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportInfoRow(report = report)
    }
}

@Composable
fun ReportInfoRow(report: FishingReport, modifier: Modifier = Modifier) {

    val dateFormatter = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru"))



        // Вертикальный стек (Название + Имя)
        Column(modifier = modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = report.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ReportBadges(report = report)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateFormatter.format(report.fishingTime),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                )
                Text(
                    text = "  •  ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = report.water.waterName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
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
        if (!report.isPublic) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = Color(0xFF5C78A3)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
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
fun ReportSummaryPreview() {
    FishingTheme {
        val sampleReport = FishingReport(
            userId = UUID.randomUUID(),
            type = FishingType.HAUL,
            name = "Смеркалось",
            water = Water(waterName = "Минское Море", latitude = 54.32344, longitude = 54.23425),
            photo = emptyList(),
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
        ReportSummary(
            report = sampleReport,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportHeaderPreview() {
    FishingTheme {
        val sampleReport = FishingReport(
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Смеркалось",
            water = Water(waterName = "Минское Море", latitude = 54.32344, longitude = 54.23425),
            photo = emptyList(),
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
