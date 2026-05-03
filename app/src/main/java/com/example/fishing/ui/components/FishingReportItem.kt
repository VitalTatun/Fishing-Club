package com.example.fishing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FishingReportItem(
    report: FishingReport,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            FishingReportHeader(report = report)

            if (report.photo.isNotEmpty()) {
                FishingReportPhotos(photos = report.photo)
                Spacer(modifier = Modifier.height(10.dp))
            }
            
            FishingReportFooter(report = report)
        }
    }
}

@Composable
private fun FishingReportHeader(report: FishingReport) {
    val dateFormatter = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru"))
    val interactionSource = remember { MutableInteractionSource() }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = report.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            
            if (!report.isPublic) {
                StatusBadge(text = "Не опубликовано")
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {  }
            )
        }

        Text(
            text = "${dateFormatter.format(report.fishingTime)}  •  ${report.water.waterName}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun FishingReportPhotos(photos: List<Int>) {
    val pagerState = rememberPagerState { photos.size }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.8f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            Image(
                painter = painterResource(id = photos[index]),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // Photo count
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${photos.size}",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun FishingReportFooter(report: FishingReport) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TagChip(text = "Отчет", icon = Icons.Outlined.Verified)
            
            if (report.fish.isNotEmpty()) {
                TagChip(text = report.fish.first().name, icon = Icons.Default.SetMeal)
            }

            TagChip(
                text = when(report.fishingMethod) {
                    FishingMethod.SPINNING -> "Спиннинг"
                    FishingMethod.BOBBER -> "Поплавок"
                    FishingMethod.FEEDER -> "Фидер"
                    else -> "Метод"
                },
                icon = Icons.Default.Phishing
            )
        }

        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = "Bookmark",
            tint = Color(0xFFC62828),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun StatusBadge(text: String) {
    Surface(
        color = Color(0xFFE8EAF6),
        shape = RoundedCornerShape(5.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color(0xFF3F51B5),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TagChip(text: String, icon: ImageVector) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FishingReportItemPreview() {
    val sampleUser = User(name = "Иван", image = "", email = "ivan@example.com")
    val sampleWater = Water(waterName = "Водохранилище Крылово", latitude = 55.0, longitude = 60.0)
    val calendar = Calendar.getInstance().apply { 
        set(2023, Calendar.AUGUST, 22) 
    }
    val sampleReport = FishingReport(
        type = FishingType.FISHING_LOG,
        name = "Смеркалось...",
        water = sampleWater,
        photo = List(8) { android.R.drawable.ic_menu_gallery },
        fishingTime = calendar.time,
        weight = 2.5,
        fish = listOf(Fish(name = "Окунь", count = 5)),
        fishingMethod = FishingMethod.SPINNING,
        bait = listOf(Bait.WOBBLER),
        comment = "Прекрасное утро.",
        user = sampleUser,
        fishingFromTheShore = true,
        isPublic = false
    )
    Box(modifier = Modifier.background(Color.White)) {
        FishingReportItem(report = sampleReport)
    }
}
