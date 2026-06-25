package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fishing.model.Fish
import kotlin.math.roundToInt

@Composable
internal fun CatchSection(
    selectedFish: List<Fish> = emptyList(),
    onArrowClick: () -> Unit = {},
    weight: Float = 0f,
    onWeightChange: (Float) -> Unit = {},
) {
    SectionCard(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 0.dp
        )
    ) {
        SectionHeader(
            title = "Улов*",
            onArrowClick = onArrowClick
        )
        if (selectedFish.isNotEmpty()) {
            selectedFish.forEachIndexed { index, fish ->
                InfoRow(
                    label = fish.name,
                    value = "${fish.count} шт.",
                    contentPadding = PaddingValues(vertical = 16.dp)
                )
                if (index < selectedFish.lastIndex) {
                    HorizontalDivider(
                        color = CreateReportColors.Divider
                    )
                }
            }
            Column(
                modifier = Modifier.padding(vertical = 16.dp)

            ) {
                InfoRow(
                    label = "Общий вес",
                    value = if (weight == 0f) "Не указан" else "${(weight * 10).roundToInt() / 10f} кг",
                    contentPadding = PaddingValues(vertical = 4.dp),
                )
                Slider(
                    value = weight,
                    onValueChange = onWeightChange,
                    valueRange = 0f..10f,
                )
            }
        }
    }
}
