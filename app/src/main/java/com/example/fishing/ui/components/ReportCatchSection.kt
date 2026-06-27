package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.util.Date
import java.util.UUID

@Composable
fun ReportCatchSection(report: FishingReport, modifier: Modifier = Modifier) {
    val allItems = report.fish.map { it.name to "${it.count} шт." } +
            ("Общий вес" to if (report.weight > 0.0) "${"%.1f".format(report.weight).replace('.', ',')} кг" else "Не указан")

    SectionCard(
        title = "Улов",
        items = allItems.map { SectionEntry.TextItem(it.first, it.second) },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun ReportCatchSectionPreview() {
    val sampleReport = FishingReport(
        userId = UUID.randomUUID(),
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
    Box(modifier = Modifier.padding(16.dp).background(MaterialTheme.colorScheme.background)) {
        ReportCatchSection(report = sampleReport)
    }
}
