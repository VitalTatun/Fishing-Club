package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.model.Fish
import com.example.fishing.ui.theme.FishingTheme

@Composable
internal fun CatchSection(
    selectedFish: List<Fish> = emptyList(),
    onArrowClick: () -> Unit = {},
    weight: Float = 0f,
) {
    SectionCard(
        contentPadding = PaddingValues(
            start = 0.dp
        )
    ) {
        Section(
            title = "Улов*",
            hasData = selectedFish.isNotEmpty(),
            onArrowClick = onArrowClick
        )
        if (selectedFish.isNotEmpty()) {
            selectedFish.forEachIndexed { index, fish ->
                InfoRow(
                    label = fish.name,
                    value = "${fish.count} шт.",
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                )
                if (index < selectedFish.lastIndex) {
                    HorizontalDivider(
                        color = CreateReportColors.Divider
                    )
                }
            }
            if (weight > 0f) {
                HorizontalDivider(color = CreateReportColors.Divider)
                InfoRow(
                    label = "Общий вес",
                    value = "${(weight * 10).toInt() / 10f} кг",
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Preview(showBackground = true, name = "Без данных")
@Composable
private fun CatchSectionEmptyPreview() {
    FishingTheme {
        CatchSection()
    }
}

@Preview(showBackground = true, name = "С данными")
@Composable
private fun CatchSectionWithDataPreview() {
    FishingTheme {
        CatchSection(
            selectedFish = listOf(
                Fish(name = "Окунь", count = 3),
                Fish(name = "Щука", count = 1)
            ),
            weight = 2.5f
        )
    }
}
