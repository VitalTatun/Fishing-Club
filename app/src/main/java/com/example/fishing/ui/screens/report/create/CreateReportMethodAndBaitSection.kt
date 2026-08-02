package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fishing.model.Bait
import com.example.fishing.model.FishingMethod

@Composable
internal fun MethodAndBaitSection(
    selectedMethod: FishingMethod,
    selectedBaits: List<Bait>,
    onArrowClick: () -> Unit,
    isRequired: Boolean = false
) {
    SectionCard(contentPadding = PaddingValues(horizontal = 0.dp)) {
        Section(
            title = "Способ ловли и наживка",
            hasData = selectedMethod != FishingMethod.NONE || selectedBaits.isNotEmpty(),
            isRequired = isRequired,
            onArrowClick = onArrowClick
        )
        if (selectedMethod != FishingMethod.NONE) {
            InfoRow(
                label = "Способ ловли",
                value = selectedMethod.russianName,
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
            )
            if (selectedBaits.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = CreateReportColors.Divider
                )
                InfoRow(
                    label = "Наживка",
                    value = selectedBaits.joinToString(", ") { it.russianName },
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 32.dp)
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
