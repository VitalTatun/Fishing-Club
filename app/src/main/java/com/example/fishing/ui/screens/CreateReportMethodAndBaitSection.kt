package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.fishing.model.Bait
import com.example.fishing.model.FishingMethod

@Composable
internal fun MethodAndBaitSection(
    selectedMethod: FishingMethod,
    selectedBaits: List<Bait>,
    onArrowClick: () -> Unit
) {
    SectionCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
        SectionHeader(
            title = "Способ ловли и наживка*",
            subtitle = if (selectedMethod == FishingMethod.NONE) "Обязательное" else null,
            onArrowClick = onArrowClick
        )
        if (selectedMethod != FishingMethod.NONE) {
            InfoRow(label = "Способ ловли", value = selectedMethod.russianName)
            if (selectedBaits.isNotEmpty()) {
                HorizontalDivider(color = CreateReportColors.Divider)
                InfoRow(
                    label = "Наживка",
                    value = selectedBaits.joinToString(", ") { it.russianName }
                )
            }
        }
    }
}
