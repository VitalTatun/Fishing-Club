package com.example.fishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "Улов",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            allItems.forEachIndexed { index, item ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    allItems.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    else -> RoundedCornerShape(4.dp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(FishingTheme.colors.secondaryBackground)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.first,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = item.second,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
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
