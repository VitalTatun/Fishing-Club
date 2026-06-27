package com.example.fishing.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishing.data.FishingRepository
import com.example.fishing.model.*
import com.example.fishing.model.FishingType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MapReportSheetContent(
    report: FishingReport,
    repository: FishingRepository,
    onPhotoClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("d MMMM yyyy • H:mm", Locale.forLanguageTag("ru"))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Одно фото (первое из списка)
        if (report.photo.isNotEmpty()) {
            PhotoImage(
                photoPath = report.photo.first(),
                repository = repository,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPhotoClick(0) },
                contentScale = ContentScale.Crop,
                contentDescription = report.name
            )
        }

        // Заголовок + дата
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            Text(
                text = dateFormatter.format(report.fishingTime),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Информация: водоём + метод + наживка + вес
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MapInfoRow(label = "Водоём", value = report.water.waterName)
            MapInfoRow(
                label = "Метод",
                value = when (report.fishingMethod) {
                    FishingMethod.BOBBER -> "Поплавок"
                    FishingMethod.SPINNING -> "Спиннинг"
                    FishingMethod.FEEDER -> "Фидер"
                    FishingMethod.FLY_FISHING -> "Нахлыст"
                    else -> "Не указан"
                }
            )
            if (report.bait.isNotEmpty()) {
                MapInfoRow(
                    label = "Наживка",
                    value = report.bait.joinToString(", ") { it.russianName }
                )
            }
            if (report.weight > 0.0) {
                MapInfoRow(
                    label = "Вес",
                    value = "${"%.1f".format(report.weight).replace('.', ',')} кг"
                )
            }
            if (report.fish.isNotEmpty()) {
                MapInfoRow(
                    label = "Улов",
                    value = report.fish.joinToString(", ") { "${it.name} ${it.count} шт." }
                )
            }
        }

        // Комментарий (если есть)
        if (report.comment.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Комментарий",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MapInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
