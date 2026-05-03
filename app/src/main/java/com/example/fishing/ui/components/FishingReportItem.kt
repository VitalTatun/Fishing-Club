package com.example.fishing.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FishingReportItem(
    report: FishingReport,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val dateFormatter = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru"))
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            // Top Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (report.photo.isNotEmpty()) {
                    Image(
                        painter = painterResource(id = report.photo.first()),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Photo count overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "1/${report.photo.size}",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF0F0F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет фото", color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tags Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TagChip(
                    text = report.type.displayName,
                    containerColor = Color(0xFFE8EAF6),
                    contentColor = Color(0xFF3F51B5)
                )
                
                TagChip(
                    text = when(report.fishingMethod) {
                        FishingMethod.SPINNING -> "Спиннинг"
                        FishingMethod.BOBBER -> "Поплавок"
                        FishingMethod.FEEDER -> "Фидер"
                        FishingMethod.FLY_FISHING -> "Нахлыст"
                        else -> "Не указан"
                    },
                    containerColor = Color(0xFFDCEDC8),
                    contentColor = Color(0xFF33691E)
                )

                if (report.fish.isNotEmpty()) {
                    TagChip(
                        text = report.fish.first().name,
                        containerColor = Color(0xFFDCEDC8),
                        contentColor = Color(0xFF33691E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title and Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Color.Black
                    )
                    Text(
                        text = "${dateFormatter.format(report.fishingTime)} • ${report.water.waterName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Bookmark Icon
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Bookmark",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )

                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun TagChip(text: String, containerColor: Color, contentColor: Color) {
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
    Box(modifier = Modifier.background(Color(0xFFF5F5F5)).padding(16.dp)) {
        FishingReportItem(report = sampleReport)
    }
}
