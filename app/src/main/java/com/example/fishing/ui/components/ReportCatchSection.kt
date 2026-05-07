package com.example.fishing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.*
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportCatchSection(report: FishingReport, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "Улов",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            color = Color.Transparent
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                report.fish.forEach { fish ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF7F8FA),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Text(
                            text = "${fish.name} ${fish.count} шт.",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportCatchSectionPreview() {
    val sampleReport = FishingReport(
        type = FishingType.FISHING_LOG,
        name = "Тестовый отчет",
        water = Water(waterName = "Озеро", latitude = 0.0, longitude = 0.0),
        photo = listOf(),
        fishingTime = Date(),
        weight = 5.4,
        fish = listOf(
            Fish(name = "Карп", count = 3),
            Fish(name = "Щука", count = 1),
            Fish(name = "Линь", count = 5),
            Fish(name = "Карась", count = 10)
        ),
        fishingMethod = FishingMethod.FEEDER,
        bait = listOf(),
        comment = "",
        user = User(name = "Виталий", image = "", email = ""),
        fishingFromTheShore = true,
        isPublic = true
    )
    Box(modifier = Modifier.padding(16.dp).background(Color(0xFFF7F8FA))) {
        ReportCatchSection(report = sampleReport)
    }
}
