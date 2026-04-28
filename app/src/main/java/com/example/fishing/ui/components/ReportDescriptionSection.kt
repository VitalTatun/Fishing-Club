package com.example.fishing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.fishing.model.FishingReport

@Composable
fun ReportDescriptionSection(report: FishingReport, modifier: Modifier = Modifier) {
    Text(
        text = report.comment,
        style = MaterialTheme.typography.bodyLarge,
        lineHeight = 24.sp,
        modifier = modifier
    )
}
