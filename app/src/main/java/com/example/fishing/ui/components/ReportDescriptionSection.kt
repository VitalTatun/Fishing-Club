package com.example.fishing.ui.components

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
import androidx.compose.ui.unit.sp
import com.example.fishing.model.*
import java.util.Date

@Composable
fun ReportDescriptionSection(report: FishingReport, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "Комментарий",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
        )
        Text(
            text = report.comment,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportDescriptionSectionPreview() {
    val sampleReport = FishingReport(
        type = FishingType.FISHING_LOG,
        name = "Тестовый отчет",
        water = Water(waterName = "Озеро", latitude = 0.0, longitude = 0.0),
        photo = listOf(),
        fishingTime = Date(),
        weight = 5.4,
        fish = listOf(),
        fishingMethod = FishingMethod.FEEDER,
        bait = listOf(),
        comment = "В этот раз разведал неглубокую часть водохранилища и поймал парочку красивых рыб! Замешав вечерком плотву с орехом от Feeder.by с утра поехал на мелководную часть вдх посмотреть как там обстоят дела с рыбкой...",
        user = User(name = "Виталий", image = "", email = ""),
        fishingFromTheShore = true,
        isPublic = true
    )
    Box(
        modifier = Modifier
            .background(Color(0xFFF7F8FA))
            .padding(16.dp)
    ) {
        ReportDescriptionSection(report = sampleReport)
    }
}

