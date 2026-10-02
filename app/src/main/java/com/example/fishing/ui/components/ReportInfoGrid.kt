package com.example.fishing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.R
import com.example.fishing.model.*
import com.example.fishing.ui.theme.FishingTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun ReportInfoGrid(report: FishingReport, modifier: Modifier = Modifier) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru")).withZone(ZoneId.systemDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("H:mm", Locale.forLanguageTag("ru")).withZone(ZoneId.systemDefault()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val timeRangeValue = when {
            report.fishingStartAt != null && report.fishingEndAt != null ->
                "${timeFormatter.format(report.fishingStartAt)} - ${timeFormatter.format(report.fishingEndAt)}"
            report.fishingStartAt != null -> timeFormatter.format(report.fishingStartAt)
            report.fishingEndAt != null -> timeFormatter.format(report.fishingEndAt)
            else -> null
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoGridItem(
                title = stringResource(R.string.date),
                value = report.fishingStartAt?.let { dateFormatter.format(it) } ?: stringResource(R.string.not_specified),
                modifier = Modifier.weight(1f)
            )
            InfoGridItem(
                title = stringResource(R.string.time),
                value = timeRangeValue ?: stringResource(R.string.not_specified),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoGridItem(
                title = stringResource(R.string.duration),
                value = report.durationFormatted() ?: stringResource(R.string.not_specified),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoGridItem(
                title = stringResource(R.string.bait),
                value = report.bait.map { stringResource(it.labelRes) }.joinToString(", "),
                modifier = Modifier.weight(1f)
            )
            InfoGridItem(
                title = stringResource(R.string.fishing_method),
                value = stringResource(report.fishingMethod.labelRes),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.catch_label),
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
                            text = stringResource(R.string.fish_count, fish.name, fish.count),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            InfoGridItem(
                title = stringResource(R.string.total_weight),
                value = if (report.weight > 0.0) "${"%.1f".format(report.weight).replace('.', ',')} ${stringResource(R.string.weight_unit)}" else stringResource(R.string.not_specified),
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

@Preview(showBackground = true, name = "Обычный отчет")
@Composable
private fun ReportInfoGridPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val sampleReport = FishingReport(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Смеркалось",
            water = Water(waterName = "Минское Море", latitude = 54.32344, longitude = 54.23425),
            photos = emptyList(),
            fishingStartAt = Instant.now().minusSeconds(3600 * 9),
            fishingEndAt = Instant.now(),
            weight = 3.2,
            fish = listOf(
                Fish(id = UUID.randomUUID(), name = "Карась", count = 2),
                Fish(id = UUID.randomUUID(), name = "Окунь", count = 2)
            ),
            fishingMethod = FishingMethod.BOBBER,
            bait = listOf(Bait.BLOODWORM, Bait.MAGGOT),
            comment = "В этот раз разведал неглубокую часть водохранилища!",
            user = User(name = "Виталий", image = "", email = "vital@example.com"),
            fishingFromTheShore = true,
            isPublic = true
        )
        ReportInfoGrid(report = sampleReport)
    }
}

@Preview(showBackground = true, name = "Трофей")
@Composable
private fun ReportInfoGridTrophyPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val sampleReport = FishingReport(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            type = FishingType.HAUL,
            name = "Тот самый улов!",
            water = Water(waterName = "Неман", latitude = 53.9, longitude = 25.3),
            photos = emptyList(),
            fishingStartAt = Instant.now().minusSeconds(3600 * 9),
            fishingEndAt = Instant.now(),
            weight = 12.5,
            fish = listOf(
                Fish(id = UUID.randomUUID(), name = "Щука", count = 1),
                Fish(id = UUID.randomUUID(), name = "Сом", count = 1)
            ),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Наконец-то поймал свой трофей!",
            user = User(name = "Виталий", image = "", email = "vital@example.com"),
            fishingFromTheShore = false,
            isPublic = true
        )
        ReportInfoGrid(report = sampleReport)
    }
}

@Preview(showBackground = true, name = "Пустой отчет")
@Composable
private fun ReportInfoGridEmptyPreview() {
    FishingTheme(darkTheme = false, dynamicColor = false) {
        val sampleReport = FishingReport(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            type = FishingType.FISHING_LOG,
            name = "Без деталей",
            water = Water(waterName = "Озеро", latitude = 0.0, longitude = 0.0),
            photos = emptyList(),
            fishingStartAt = null,
            fishingEndAt = null,
            weight = 0.0,
            fish = emptyList(),
            fishingMethod = FishingMethod.FEEDER,
            bait = emptyList(),
            comment = "",
            user = User(name = "Виталий", image = "", email = "vital@example.com"),
            fishingFromTheShore = true,
            isPublic = true
        )
        ReportInfoGrid(report = sampleReport)
    }
}