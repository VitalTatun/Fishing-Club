package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.util.*

@Composable
fun ReportLocationSection(report: FishingReport, modifier: Modifier = Modifier) {
    val primaryColor = Color(0xFF3E5481)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.water.waterName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "GPS координаты: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "${report.water.latitude} - ${report.water.longitude}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryColor
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Копировать",
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Map Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE3F2FD))
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportLocationSectionPreview() {
    FishingTheme {
        val sampleReport = FishingReport(
            type = FishingType.FISHING_LOG,
            name = "Тестовый отчет",
            water = Water(
                waterName = "Озеро Нарочь",
                latitude = 54.8510,
                longitude = 26.7086
            ),
            photo = listOf(),
            fishingTime = Date(),
            weight = 0.0,
            fish = listOf(),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(),
            comment = "",
            user = User(name = "Иван Иванов", image = "", email = ""),
            fishingFromTheShore = true,
            isPublic = true
        )
        ReportLocationSection(
            report = sampleReport,
            modifier = Modifier.padding(16.dp)
        )
    }
}
