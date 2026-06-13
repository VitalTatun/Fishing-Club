package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fishing.model.Fish

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CatchSection(
    selectedFish: List<Fish> = emptyList(),
    onArrowClick: () -> Unit = {},
) {
    SectionCard(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = if (selectedFish.isNotEmpty()) 16.dp else 0.dp
        )
    ) {
        SectionHeader(
            title = "Улов*",
            subtitle = "Обязательное",
            onArrowClick = onArrowClick
        )
        if (selectedFish.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedFish.forEach { fish ->
                    AssistChip(
                        onClick = {},
                        label = { Text("${fish.name} ${fish.count} шт.", maxLines = 1) },
                        shape = RoundedCornerShape(8.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = CreateReportColors.Surface,
                            labelColor = CreateReportColors.OnSurfaceVariant
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = CreateReportColors.OutlineVariant
                        )
                    )
                }
            }
        }
    }
}
