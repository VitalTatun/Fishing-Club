package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishingType
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportHeaderSection(
    reportType: FishingType,
    onReportTypeChange: (FishingType) -> Unit,
    isPublic: Boolean,
    onPublicChange: (Boolean) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportTypeSelector(
                reportType = reportType,
                onReportTypeChange = onReportTypeChange
            )

            ReportDateTimeRow(
                startDate = startDate,
                onStartDateChange = onStartDateChange,
                startTime = startTime,
                onStartTimeChange = onStartTimeChange,
                endDate = endDate,
                onEndDateChange = onEndDateChange,
                endTime = endTime,
                onEndTimeChange = onEndTimeChange
            )

            SwitchRow(
                title = stringResource(R.string.publish),
                checked = isPublic,
                onCheckedChange = onPublicChange,
                supportingText = stringResource(R.string.publish_supporting)
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportDateTimeRow(
    startDate: String,
    onStartDateChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val currentTime = Calendar.getInstance()

    if (showStartDatePicker) {
        FishingDatePickerDialog(
            onDismiss = { showStartDatePicker = false },
            onConfirm = { millis ->
                millis?.let {
                    val d = Date(it)
                    val formatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
                    onStartDateChange(formatter.format(d))
                }
                showStartDatePicker = false
            }
        )
    }

    if (showStartTimePicker) {
        val startTimePickerState = rememberTimePickerState(
            initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
            initialMinute = currentTime.get(Calendar.MINUTE),
            is24Hour = true,
        )
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    startTimePickerState.hour,
                    startTimePickerState.minute
                )
                onStartTimeChange(formattedTime)
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = startTimePickerState)
        }
    }

    if (showEndDatePicker) {
        FishingDatePickerDialog(
            onDismiss = { showEndDatePicker = false },
            onConfirm = { millis ->
                millis?.let {
                    val d = Date(it)
                    val formatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
                    onEndDateChange(formatter.format(d))
                }
                showEndDatePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        val endTimePickerState = rememberTimePickerState(
            initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
            initialMinute = currentTime.get(Calendar.MINUTE),
            is24Hour = true,
        )
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    endTimePickerState.hour,
                    endTimePickerState.minute
                )
                onEndTimeChange(formattedTime)
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = endTimePickerState)
        }
    }

    DateTimeRow(
        overline = stringResource(R.string.fishing_start),
        date = startDate,
        onDateClick = { showStartDatePicker = true },
        time = startTime,
        onTimeClick = { showStartTimePicker = true }
    )
    DateTimeRow(
        overline = stringResource(R.string.fishing_end),
        date = endDate,
        onDateClick = { showEndDatePicker = true },
        time = endTime,
        onTimeClick = { showEndTimePicker = true }
    )
}

@Composable
private fun DateTimeRow(
    overline: String,
    date: String,
    onDateClick: () -> Unit,
    time: String,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = overline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.date),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )

            FilledTonalButton(
                onClick = onDateClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = date.ifEmpty { "—" },
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.time),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            FilledTonalButton(
                onClick = onTimeClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = time.ifEmpty { "—" },
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Preview(showBackground = true, name = "Отчет")
@Composable
private fun ReportHeaderSectionPreview() {
    FishingTheme {
        ReportHeaderSection(
            reportType = FishingType.FISHING_LOG,
            onReportTypeChange = {},
            isPublic = false,
            onPublicChange = {},
            startDate = "29 июля 2026",
            onStartDateChange = {},
            startTime = "16:30",
            onStartTimeChange = {},
            endDate = "29 июля 2026",
            onEndDateChange = {},
            endTime = "21:00",
            onEndTimeChange = {}
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

@Preview(showBackground = true, name = "Дата и время")
@Composable
private fun ReportDateTimeRowPreview() {
    FishingTheme {
        ReportDateTimeRow(
            startDate = "2 августа 2026",
            onStartDateChange = {},
            startTime = "19:30",
            onStartTimeChange = {},
            endDate = "3 августа 2026",
            onEndDateChange = {},
            endTime = "03:00",
            onEndTimeChange = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FishingDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState()

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
