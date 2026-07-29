package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.fishing.ui.theme.FishingTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportHeaderSection(
    reportType: String,
    onReportTypeChange: (String) -> Unit,
    isPublic: Boolean,
    onPublicChange: (Boolean) -> Unit,
    fishingDate: String,
    onFishingDateChange: (String) -> Unit,
    fishingStartTime: String,
    onFishingStartTimeChange: (String) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Date(millis)
                        val formatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
                        onFishingDateChange(formatter.format(date))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            SectionCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Выберите время",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Отмена")
                        }
                        TextButton(onClick = {
                            val formattedTime = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            onFishingStartTimeChange(formattedTime)
                            showTimePicker = false
                        }) {
                            Text("OK")
                        }
                    }
                }
            }
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
            "Трофей" -> "Трофей — для особого улова: фото обязательно, в отчете только одна рыба, остальные поля без изменений."
            else -> "Отчет - для обычных заметок: можно без фото, количество рыб не ограничено."
        }

        var expanded by remember { mutableStateOf(false) }
        val options = listOf("Отчет", "Трофей")

        Column {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = reportType,
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
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onReportTypeChange(option)
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
            reportType = "Отчет",
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
            reportType = "Трофей",
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
