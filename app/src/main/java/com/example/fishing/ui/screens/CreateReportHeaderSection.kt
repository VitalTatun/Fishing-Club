package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ReportHeaderSection(
    title: String,
    onTitleChange: (String) -> Unit,
    reportType: String,
    onReportTypeChange: (String) -> Unit
) {
    SectionCard(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 20.dp)
    ) {
        ReportTextField(
            value = title,
            onValueChange = onTitleChange,
            label = "Отчет *",
            placeholder = "Название отчета"
        )
        Spacer(Modifier.height(16.dp))
        ReportDropdownField(
            value = reportType,
            onValueChange = onReportTypeChange,
            label = "Тип",
            options = listOf("Отчет", "Трофей")
        )
    }
}
