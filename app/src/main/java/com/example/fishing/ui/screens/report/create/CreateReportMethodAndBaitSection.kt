package com.example.fishing.ui.screens.report.create

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
        Section(
            title = "Способ ловли и наживка*",
            hasData = selectedMethod != FishingMethod.NONE || selectedBaits.isNotEmpty(),
            onArrowClick = onArrowClick
        )
        if (selectedMethod != FishingMethod.NONE) {
            InfoRow(
                label = "Способ ловли",
                value = selectedMethod.russianName,
                contentPadding = PaddingValues(vertical = 16.dp)
            )
            if (selectedBaits.isNotEmpty()) {
                HorizontalDivider(color = CreateReportColors.Divider)
                InfoRow(
                    label = "Наживка",
                    value = selectedBaits.joinToString(", ") { it.russianName },
                    contentPadding = PaddingValues(vertical = 16.dp)
                )
            }
        }
    }
}
