package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    fishingDate: String,
    onFishingDateChange: (String) -> Unit,
    fishingStartTime: String,
    onFishingStartTimeChange: (String) -> Unit,
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
                date = fishingDate,
                onDateChange = onFishingDateChange,
                time = fishingStartTime,
                onTimeChange = onFishingStartTimeChange
            )

            SwitchRow(
                title = "Опубликовать",
                checked = isPublic,
                onCheckedChange = onPublicChange,
                supportingText = "Ваш отчет пополнит карту уловов и вдохновит других рыбаков. Если хотите сохранить место в секрете — просто отключите публикацию."
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
        FishingType.HAUL -> "Трофей — для особого улова: фото обязательно, в отчете только одна рыба, остальные поля без изменений."
        FishingType.FISHING_LOG -> "Отчет — для обычных заметок: можно без фото, количество рыб не ограничено."
    }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FishingType.entries.forEach { type ->
                FilterChip(
                    selected = reportType == type,
                    onClick = { onReportTypeChange(type) },
                    label = { Text(type.displayName) },
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
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportDateTimeRow(
    date: String,
    onDateChange: (String) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    if (showDatePicker) {
        FishingDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                millis?.let {
                    val d = Date(it)
                    val formatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
                    onDateChange(formatter.format(d))
                }
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    timePickerState.hour,
                    timePickerState.minute
                )
                onTimeChange(formattedTime)
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Дата",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = date.ifEmpty { "—" },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showDatePicker = true }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Время",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = time.ifEmpty { "—" },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showTimePicker = true }
            )
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
            fishingDate = "29 июля 2026",
            onFishingDateChange = {},
            fishingStartTime = "16:30",
            onFishingStartTimeChange = {}
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
            date = "2 августа 2026",
            onDateChange = {},
            time = "19:30",
            onTimeChange = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FishingDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(datePickerState.selectedDateMillis) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Отмена")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Отмена")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("OK")
            }
        },
        text = { content() }
    )
}
