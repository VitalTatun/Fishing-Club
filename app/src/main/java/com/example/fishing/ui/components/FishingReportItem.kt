package com.example.fishing.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.unit.sp
import com.example.fishing.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FishingReportItem(
    report: FishingReport,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column {
            // Top Image Area (only show if photos are present)
            if (report.photo.isNotEmpty()) {
                Image(
                    painter = painterResource(id = report.photo.first()),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Title and Status Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Surface(
                        color = Color(0xFFDDE2FB), // Light blue-ish tag
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (report.isPublic) "Опубликовано" else "Не опубликовано",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF3F51B5)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date and Water
                Text(
                    text = "${dateFormatter.format(report.fishingTime)} • ${report.water.waterName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom row with Tags and Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagChip(
                            text = report.type.displayName,
                            containerColor = Color(0xFFC8E6C9), // Green
                            contentColor = Color(0xFF1B5E20)
                        )
                        
                        TagChip(
                            text = when(report.fishingMethod) {
                                FishingMethod.SPINNING -> "Спиннинг"
                                FishingMethod.BOBBER -> "Поплавок"
                                FishingMethod.FEEDER -> "Фидер"
                                FishingMethod.FLY_FISHING -> "Нахлыст"
                                else -> "Не указан"
                            },
                            containerColor = Color(0xFFDDE2FB),
                            contentColor = Color(0xFF3F51B5)
                        )
                        
                        if (report.fish.isNotEmpty()) {
                            TagChip(
                                text = report.fish.first().name,
                                containerColor = Color(0xFFDDE2FB),
                                contentColor = Color(0xFF3F51B5)
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
            color = contentColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FishingReportItemPreview() {
    val sampleUser = User(name = "Иван", image = "", email = "ivan@example.com")
    val sampleWater = Water(waterName = "Река Кама", latitude = 55.0, longitude = 60.0)
    val sampleReport = FishingReport(
        type = FishingType.FISHING_LOG,
        name = "Смеркалось...",
        water = sampleWater,
        photo = emptyList(),
        fishingTime = Date(),
        weight = 2.5,
        fish = listOf(Fish(name = "Окунь", count = 5)),
        fishingMethod = FishingMethod.SPINNING,
        bait = listOf(Bait.WOBBLER),
        comment = "Прекрасное утро.",
        user = sampleUser,
        fishingFromTheShore = true,
        isPublic = false
    )
    Box(modifier = Modifier.background(Color.LightGray).padding(16.dp)) {
        FishingReportItem(report = sampleReport)
    }
}
