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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.util.*

@Preview(showBackground = true)
@Composable
fun ReportGeneralInfoPreview() {
    val mockReport = FishingReport(
        type = FishingType.FISHING_LOG,
        name = "Летняя рыбалка на Оке",
        water = Water(waterName = "Река Ока", latitude = 54.0, longitude = 37.0),
        photo = listOf(),
        fishingTime = Calendar.getInstance().apply {
            set(2023, Calendar.AUGUST, 14, 7, 0)
        }.time,
        weight = 3.2,
        fish = listOf(
            Fish(name = "Карась", count = 2),
            Fish(name = "Окунь", count = 2),
            Fish(name = "Лещ", count = 2),
            Fish(name = "Подлещик", count = 2)
        ),
        fishingMethod = FishingMethod.BOBBER,
        bait = listOf(Bait.BLOODWORM, Bait.MAGGOT),
        comment = "Отличный клев утром!",
        user = User(image = "", name = "Виталий", email = "vital@example.com"),
        fishingFromTheShore = true,
        isPublic = true
    )
    
    FishingTheme {
        Surface(modifier = Modifier.padding(horizontal = 16.dp)) {
            ReportGeneralInfo(report = mockReport)
        }
    }
}

@Composable
fun ReportGeneralInfo(report: FishingReport, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoDetailsList(report = report)
        ReportCatchSection(report = report)
        ReportDescriptionSection(report = report)
    }
}


@Composable
private fun InfoDetailsList(report: FishingReport) {
    val dateFormatter = SimpleDateFormat("d MMMM yyyy • H:mm", Locale.forLanguageTag("ru"))

    val items = listOf(
        "Способ ловли" to when (report.fishingMethod) {
            FishingMethod.BOBBER -> "Поплавок"
            FishingMethod.SPINNING -> "Спиннинг"
            FishingMethod.FEEDER -> "Фидер"
            FishingMethod.FLY_FISHING -> "Нахлыст"
            else -> "Не указан"
        },
        "Наживка" to report.bait.joinToString(", ") { it.russianName },
        "Дата" to dateFormatter.format(report.fishingTime),
        "Ловля с берега" to if (report.fishingFromTheShore) "Да" else "Нет",
        "Общий вес" to "${report.weight.toString().replace('.', ',')} кг"
    )

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = "Общая информация",
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
            items.forEachIndexed { index, item ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    items.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    else -> RoundedCornerShape(4.dp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = item.first, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(text = item.second, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}






