package com.example.fishing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),

    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            if (report.photo.isNotEmpty()) {
                FishingReportPhotos(photos = report.photo)
            }
            FishingReportHeader(report = report)
            FishingReportFooter(report = report)
        }
    }
}

@Composable
private fun FishingReportHeader(report: FishingReport) {
    val dateFormatter = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru"))

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = report.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp
                ),
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            
            if (report.type == FishingType.HAUL) {
                TrophyBadge()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = "Bookmark",
                tint = Color(0xFFFF3E00),
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = "${dateFormatter.format(report.fishingTime)}  •  ${report.water.waterName}",
            style = MaterialTheme.typography.bodySmall,
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
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
        
        // Photo count
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 20.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "${pagerState.currentPage + 1} из ${photos.size}",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun FishingReportFooter(report: FishingReport) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!report.isPublic) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color(0xFF3E5481)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

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
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {  }
        )
    }
}

@Composable
fun TrophyBadge() {
    Surface(
        color = Color(0xFFFFD71D),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "Трофей",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color(0xFF50250A),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TagChip(
    text: String,
    icon: ImageVector,
    containerColor: Color = Color.White,
    borderColor: Color = Color(0xFFB6C3E5),
    contentColor: Color = Color(0xFF3E5481)
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
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
        type = FishingType.HAUL,
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
