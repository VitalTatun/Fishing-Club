package com.example.fishing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.fishing.model.*
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
    
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ReportGeneralInfo(report = mockReport)
        }
    }
}

@Composable
fun ReportGeneralInfo(report: FishingReport, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(Color.White)
            .padding(all = 16.dp)
    )
    {
        Text(
            text = "Общая информация",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        MoodSection(selectedMood = 5) // Dummy data

        Spacer(modifier = Modifier.height(16.dp))

        InfoDetailsList(report = report)

        Spacer(modifier = Modifier.height(13.dp))

        CatchSection(report = report)
        Spacer(modifier = Modifier.height(13.dp))

        ReportDescriptionSection(report = report)

    }
}

@Composable
private fun MoodSection(selectedMood: Int) {
    Surface(
        color = Color(0xFFF0F2FA),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MoodIcon(Icons.Default.SentimentVeryDissatisfied, selectedMood == 1)
                MoodIcon(Icons.Default.SentimentDissatisfied, selectedMood == 2)
                MoodIcon(Icons.Default.SentimentNeutral, selectedMood == 3)
                MoodIcon(Icons.Default.SentimentSatisfied, selectedMood == 4)
                MoodIcon(Icons.Default.SentimentVerySatisfied, selectedMood == 5)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Очень плохая",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C6E91)
                )
                Text(
                    text = "Очень хорошая",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C6E91)
                )
            }
        }
    }
}

@Composable
private fun MoodIcon(icon: ImageVector, isSelected: Boolean) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) Color(0xFFFF5722) else Color(0xFF8E99BA),
        modifier = Modifier.size(40.dp)
    )
}

@Composable
private fun InfoDetailsList(report: FishingReport) {
    val dateFormatter = SimpleDateFormat("d MMMM yyyy • H:mm", Locale.forLanguageTag("ru"))
    
    Column {
        InfoRow(
            label = "Способ ловли",
            value = when (report.fishingMethod) {
                FishingMethod.BOBBER -> "Поплавок"
                FishingMethod.SPINNING -> "Спиннинг"
                FishingMethod.FEEDER -> "Фидер"
                FishingMethod.FLY_FISHING -> "Нахлыст"
                else -> "Не указан"
            }
        )
        InfoRow("Наживка", report.bait.joinToString(", ") { it.russianName })
        InfoRow("Дата", dateFormatter.format(report.fishingTime))
        InfoRow("Ловля с берега", if (report.fishingFromTheShore) "Да" else "Нет")
        InfoRow("Общий вес", "${report.weight.toString().replace('.', ',')} кг.")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatchSection(report: FishingReport) {
    Column {
        Text(
            text = "Улов",
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            report.fish.forEach { fish ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    color = Color.Transparent
                ) {
                    Text(
                        text = "${fish.name} ${fish.count} шт.",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}
