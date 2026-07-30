package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                    val date = Date(it)
                    val formatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
                    onFishingDateChange(formatter.format(date))
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
                onFishingStartTimeChange(formattedTime)
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val supportingText = when (reportType) {
            FishingType.HAUL -> "Трофей — для особого улова: фото обязательно, в отчете только одна рыба, остальные поля без изменений."
            FishingType.FISHING_LOG -> "Отчет - для обычных заметок: можно без фото, количество рыб не ограничено."
        }

        var expanded by remember { mutableStateOf(false) }

        Column {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = reportType.displayName,
                    onValueChange = {},
                    label = { Text("Тип") },
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    FishingType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                onReportTypeChange(type)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Text(
                text = supportingText,
                color = CreateReportColors.OnSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = fishingDate.ifEmpty { " " },
                onValueChange = {},
                label = { Text("Дата *") },
                readOnly = true,
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDatePicker)
                    }
                }
            )
            OutlinedTextField(
                value = fishingStartTime.ifEmpty { " " },
                onValueChange = {},
                label = { Text("Время *") },
                readOnly = true,
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTimePicker)
                    }
                }
            )
        }
        SwitchRow(
            title = "Опубликовать",
            checked = isPublic,
            onCheckedChange = onPublicChange,
            supportingText = "Ваш отчет пополнит карту уловов и вдохновит других рыбаков. Если хотите сохранить место в секрете - просто отключите публикацию."
        )
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

@Preview(showBackground = true, name = "Трофей")
@Composable
private fun ReportHeaderSectionTrophyPreview() {
    FishingTheme {
        ReportHeaderSection(
            reportType = FishingType.HAUL,
            onReportTypeChange = {},
            isPublic = true,
            onPublicChange = {},
            fishingDate = "29 июля 2026",
            onFishingDateChange = {},
            fishingStartTime = "16:30",
            onFishingStartTimeChange = {}
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
