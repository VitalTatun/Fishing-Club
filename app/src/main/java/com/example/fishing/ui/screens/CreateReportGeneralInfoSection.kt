package com.example.fishing.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GeneralInfoSection(
    fishingDate: String,
    onFishingDateChange: (String) -> Unit,
    fishingStartTime: String,
    onFishingStartTimeChange: (String) -> Unit,
    fishingFromShore: Boolean,
    onFishingFromShoreChange: (Boolean) -> Unit,
    isPublic: Boolean,
    onPublicChange: (Boolean) -> Unit,
    isPaidWater: Boolean,
    onPaidWaterChange: (Boolean) -> Unit,
    weight: Float,
    onWeightChange: (Float) -> Unit,
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
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
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

    SectionCard(contentPadding = PaddingValues(bottom = 16.dp)) {
        SectionHeader(
            title = "Общая информация",
            modifier = Modifier.padding(horizontal = 16.dp),
            showArrow = false,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReportPickerField(
                value = fishingDate,
                onValueChange = onFishingDateChange,
                label = "Дата *",
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDatePicker = true }
            )
            ReportPickerField(
                value = fishingStartTime,
                onValueChange = onFishingStartTimeChange,
                label = "Время начала *",
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                onClick = { showTimePicker = true }
            )
        }
        SwitchRow("Ловля с берега", fishingFromShore, onFishingFromShoreChange)
        SwitchRow("Опубликовать", isPublic, onPublicChange)
        SwitchRow("Платный водоем", isPaidWater, onPaidWaterChange)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            InfoRow(
                label = "Общий вес",
                value = if (weight == 0f) "Не указан" else "${(weight * 10).roundToInt() / 10f} кг",
                contentPadding = PaddingValues(0.dp),
            )
            Slider(
                value = weight,
                onValueChange = onWeightChange,
                valueRange = 0f..10f,
            )
        }
    }
}
