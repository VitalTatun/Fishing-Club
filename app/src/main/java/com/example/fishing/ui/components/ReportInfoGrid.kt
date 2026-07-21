package com.example.fishing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportInfoGrid(report: FishingReport, modifier: Modifier = Modifier) {
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")) }
    val timeFormatter = remember { SimpleDateFormat("H:mm", Locale.forLanguageTag("ru")) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoGridItem(
                title = "Дата",
                value = dateFormatter.format(report.fishingTime),
                modifier = Modifier.weight(1f)
            )
            InfoGridItem(
                title = "Время",
                value = timeFormatter.format(report.fishingTime),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoGridItem(
                title = "Наживка",
                value = report.bait.joinToString(", ") { it.russianName },
                modifier = Modifier.weight(1f)
            )
            InfoGridItem(
                title = "Способ ловли",
                value = report.fishingMethod.russianName,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Улов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (report.type == FishingType.HAUL) {
                        TrophyBadge()
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                if (report.fish.isEmpty()) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    report.fish.forEach { fish ->
                        Text(
                            text = "${fish.name} — ${fish.count} шт.",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            InfoGridItem(
                title = "Общий вес",
                value = if (report.weight > 0.0) "${"%.1f".format(report.weight).replace('.', ',')} кг." else "Не указан",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InfoGridItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
