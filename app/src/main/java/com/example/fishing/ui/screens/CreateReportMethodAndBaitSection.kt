package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun MethodAndBaitSection() {
    SectionCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
        SectionHeader(title = "Способ ловли и наживка*")
        InfoRow(label = "Способ ловли", value = "Поплавок")
        HorizontalDivider(color = CreateReportColors.Divider)
        InfoRow(label = "Наживка", value = "Мотыль, Опарыш")
    }
}
