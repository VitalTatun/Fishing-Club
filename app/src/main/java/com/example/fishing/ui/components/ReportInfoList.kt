package com.example.fishing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fishing.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportInfoList(report: FishingReport, modifier: Modifier = Modifier) {
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy • HH:mm", Locale.forLanguageTag("ru"))
    Column(modifier = modifier) {
        DetailRow("Дата отчета", dateFormatter.format(report.fishingTime))
        DetailRow(
            label = "Способ ловли",
            value = when (report.fishingMethod) {
                FishingMethod.BOBBER -> "Поплавок"
                FishingMethod.SPINNING -> "Спиннинг"
                FishingMethod.FEEDER -> "Фидер"
                FishingMethod.FLY_FISHING -> "Нахлыст"
                else -> "Не указан"
            }
        )
        DetailRow("Наживка", report.bait.joinToString(", ") { it.russianName })
        DetailRow("Ловля с берега", if (report.fishingFromTheShore) "Да" else "Нет")
        DetailRow("Общий вес", "${report.weight} кг.")
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontWeight = FontWeight.Medium)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
