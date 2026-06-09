package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun GeneralInfoSection(
    fishingDate: String,
    onFishingDateChange: (String) -> Unit,
    fishingStartTime: String,
    onFishingStartTimeChange: (String) -> Unit,
    fishingFromShore: Boolean,
    onFishingFromShoreChange: (Boolean) -> Unit,
    isPublic: Boolean,
    onPublicChange: (Boolean) -> Unit,
    isPaidWater: Boolean,
    onPaidWaterChange: (Boolean) -> Unit,
    weight: Float,
    onWeightChange: (Float) -> Unit
) {
    SectionCard(contentPadding = PaddingValues(bottom = 16.dp)) {
        SectionHeader(
            title = "Общая информация",
            modifier = Modifier.padding(horizontal = 16.dp),
            showArrow = false
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportPickerField(
                value = fishingDate,
                onValueChange = onFishingDateChange,
                label = "Дата *",
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
            ReportPickerField(
                value = fishingStartTime,
                onValueChange = onFishingStartTimeChange,
                label = "Время начала *",
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                modifier = Modifier
                    .widthIn(min = 130.dp)
                    .weight(0.9f)
            )
        }
        SwitchRow("Ловля с берега", fishingFromShore, onFishingFromShoreChange)
        SwitchRow("Опубликовать", isPublic, onPublicChange)
        SwitchRow("Платный водоем", isPaidWater, onPaidWaterChange)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            InfoRow(
                label = "Общий вес",
                value = if (weight == 0f) "Не указан" else "${(weight * 10).roundToInt() / 10f} кг",
                contentPadding = PaddingValues(0.dp)
            )
            Slider(
                value = weight,
                onValueChange = onWeightChange,
                valueRange = 0f..20f
            )
        }
    }
}
