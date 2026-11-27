package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishingType
import com.example.fishing.ui.theme.FishingTheme

@Composable
internal fun ReportTypeSelector(
    reportType: FishingType,
    onReportTypeChange: (FishingType) -> Unit,
    modifier: Modifier = Modifier
) {
    val supportingText = when (reportType) {
        FishingType.HAUL -> stringResource(R.string.report_type_trophy_supporting)
        FishingType.FISHING_LOG -> stringResource(R.string.report_type_log_supporting)
    }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FishingType.entries.forEach { type ->
                FilterChip(
                    selected = reportType == type,
                    onClick = { onReportTypeChange(type) },
                    label = { Text(stringResource(type.labelRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                )
            }
        }
        Text(
            text = supportingText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true, name = "Тип отчета")
@Composable
private fun ReportTypeSelectorPreview() {
    FishingTheme {
        ReportTypeSelector(
            reportType = FishingType.FISHING_LOG,
            onReportTypeChange = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FishingDatePickerDialog(
    initialSelectedDateMillis: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(datePickerState.selectedDateMillis) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
internal fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(R.string.ok))
            }
        },
        text = { content() }
    )
}
