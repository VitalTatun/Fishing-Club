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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CatchSection(
    onArrowClick: () -> Unit = {}
) {
    SectionCard(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        SectionHeader(
            title = "Улов*",
            subtitle = "Обязательное",
            onArrowClick = onArrowClick
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Карась 2 шт.", "Окунь 2 шт.", "Лещ 2 шт.", "Подлещик 2 шт.").forEach { chip ->
                AssistChip(
                    onClick = {},
                    label = { Text(chip, maxLines = 1) },
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
