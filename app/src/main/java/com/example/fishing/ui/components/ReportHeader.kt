package com.example.fishing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Фото карусель
        ReportPhotoCarousel(photos = report.photo)
        
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
            OverlayBadge(
                text = "${pagerState.currentPage + 1}/${photos.size}",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            )
        }
    }
}

@Composable
fun ReportSummary(report: FishingReport, modifier: Modifier = Modifier) {
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")) }
    
    val fishAndMethod = remember(report.fish, report.fishingMethod) {
        val fishName = report.fish.firstOrNull()?.name ?: "Рыба"
        val methodName = report.fishingMethod.russianName
        "$fishName • $methodName"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = fishAndMethod,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateFormatter.format(report.fishingTime),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = " • ",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color.Black // Figma shows black dot? actually it's material-theme/sys/light/on-surface according to design tokens
            )
            Text(
                text = report.water.waterName,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!report.isPublic) {
                DraftBadge()
            }
            if (report.type == FishingType.HAUL) {
                TrophyBadge()
            }
        }
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
            photo = listOf(""),
            fishingTime = Date(),
            weight = 3.2,
            fish = listOf(Fish(name = "Окунь", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(),
            comment = "",
            user = User(name = "Никита Белозерцев", image = "", email = ""),
            fishingFromTheShore = true,
            isPublic = false
        )
        ReportHeader(report = sampleReport, modifier = Modifier.padding(16.dp))
    }
}
