package com.example.fishing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.util.*

@Preview(showBackground = true)
@Composable
fun ReportGeneralInfoDetailsListPreview() {
    FishingTheme {
        val mockReport = FishingReport(
            userId = UUID.randomUUID(),
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
        
        Surface(modifier = Modifier.padding(horizontal = 16.dp)) {
            InfoDetailsList(report = mockReport)
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
        SectionItem("Способ ловли", when (report.fishingMethod) {
            FishingMethod.BOBBER -> "Поплавок"
            FishingMethod.SPINNING -> "Спиннинг"
            FishingMethod.FEEDER -> "Фидер"
            FishingMethod.FLY_FISHING -> "Нахлыст"
            else -> "Не указан"
        }),
        SectionItem("Наживка", report.bait.joinToString(", ") { it.russianName }),
        SectionItem("Дата", dateFormatter.format(report.fishingTime))
    )

    SectionCard(
        title = "Общая информация",
        items = items
    )
}






