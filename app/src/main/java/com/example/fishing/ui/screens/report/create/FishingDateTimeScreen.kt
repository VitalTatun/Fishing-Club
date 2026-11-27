package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.unit.sp
import com.example.fishing.model.FishingDateTimeError
import com.example.fishing.model.FishingDateTimeValidator
import com.example.fishing.ui.components.FishingListItem
import com.example.fishing.ui.components.SectionGroup
import com.example.fishing.ui.theme.FishingTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Standalone date and time step for creating a fishing report.
 *
 * The screen edits local values and commits them through [onConfirm].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingDateTimeScreen(
    startDateText: String = "",
    startTimeText: String = "",
    endDateText: String = "",
    endTimeText: String = "",
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onConfirm: (startDate: String, startTime: String, endDate: String, endTime: String) -> Unit = { _, _, _, _ -> },
) {
    var startDate by remember(startDateText) {
        mutableStateOf(parseDateText(startDateText))
    }
    var endDate by remember(startDateText, endDateText) {
        val parsedStart = parseDateText(startDateText)
        mutableStateOf(parseDateText(endDateText) ?: parsedStart.takeIf { endDateText.isBlank() })
    }
    var startTime by remember(startTimeText) {
        mutableStateOf(startTimeText.takeIf(String::isNotBlank))
    }
    var endTime by remember(endTimeText) {
        mutableStateOf(endTimeText.takeIf(String::isNotBlank))
    }
    var datesMatch by remember(startDateText, endDateText) {
        mutableStateOf(endDateText.isBlank() || startDateText == endDateText)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickerTarget by remember { mutableStateOf(PickerTarget.START) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
    }
    val startDateTime = combineDateAndTime(startDate, startTime)
    val endDateTime = combineDateAndTime(endDate, endTime)
    val startInstant = startDateTime?.atZone(ZoneId.systemDefault())?.toInstant()
    val endInstant = endDateTime?.atZone(ZoneId.systemDefault())?.toInstant()
    val dateTimeErrors = FishingDateTimeValidator.validate(startInstant, endInstant, Instant.now())
    val today = LocalDate.now()
    val endDateBeforeStartDate = startDate?.let { selectedStart ->
        endDate?.isBefore(selectedStart) == true
    } == true
    val startDateError = if (
        startDate?.isAfter(today) == true ||
        FishingDateTimeError.START_IN_FUTURE in dateTimeErrors
    ) {
        "Дата начала рыбалки не может быть позже настоящей даты"
    } else {
        null
    }
    val endDateError = if (
        endDateBeforeStartDate
    ) {
        "Дата окончания рыбалки не может быть раньше даты начала"
    } else if (
        endDate?.isAfter(today) == true ||
        FishingDateTimeError.END_IN_FUTURE in dateTimeErrors
    ) {
        "Дата окончания рыбалки не может быть позже настоящей даты"
    } else {
        null
    }
    val timeOrderError = FishingDateTimeError.END_NOT_AFTER_START in dateTimeErrors &&
        !endDateBeforeStartDate
    val canConfirm = startInstant != null && endInstant != null && dateTimeErrors.isEmpty()

    if (showDatePicker) {
        val selectedDate = if (pickerTarget == PickerTarget.START) startDate else endDate
        FishingDatePickerDialog(
            initialSelectedDateMillis = selectedDate
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli(),
            onDismiss = { showDatePicker = false },
            onConfirm = { selectedMillis ->
                selectedMillis?.let { millis ->
                    val selectedDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    if (pickerTarget == PickerTarget.START) {
                        startDate = selectedDate
                        if (datesMatch) endDate = selectedDate
                    } else {
                        endDate = selectedDate
                    }
                }
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        val currentTime = remember { java.util.Calendar.getInstance() }
        val currentSelection = if (pickerTarget == PickerTarget.START) startTime else endTime
        val selectedParts = currentSelection?.split(":")
        val timePickerState = rememberTimePickerState(
            initialHour = selectedParts?.getOrNull(0)?.toIntOrNull()
                ?: currentTime.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = selectedParts?.getOrNull(1)?.toIntOrNull()
                ?: currentTime.get(java.util.Calendar.MINUTE),
            is24Hour = true,
        )
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val formatted = "%02d:%02d".format(
                    Locale.getDefault(),
                    timePickerState.hour,
                    timePickerState.minute
                )
                if (pickerTarget == PickerTarget.START) startTime = formatted else endTime = formatted
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Дата и время") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val selectedStartDate = startDate
                            val selectedEndDate = if (datesMatch) startDate else endDate
                            if (selectedStartDate != null && selectedEndDate != null) {
                                onConfirm(
                                    formatDateForViewModel(selectedStartDate),
                                    startTime.orEmpty(),
                                    formatDateForViewModel(selectedEndDate),
                                    endTime.orEmpty(),
                                )
                            }
                        },
                        enabled = canConfirm,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Готово",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            DateSection(
                startDateText = startDate?.format(dateFormatter) ?: "Не указан",
                endDateText = endDate?.format(dateFormatter) ?: "Не указан",
                startDate = startDate,
                startDateError = startDateError,
                endDateError = endDateError,
                datesMatch = datesMatch,
                onStartDateClick = {
                    pickerTarget = PickerTarget.START
                    showDatePicker = true
                },
                onEndDateClick = {
                    pickerTarget = PickerTarget.END
                    showDatePicker = true
                },
                onDatesMatchChange = { checked ->
                    datesMatch = checked
                    if (checked) endDate = startDate
                },
            )

            TimeSection(
                startTime = startTime.orEmpty(),
                endTime = endTime.orEmpty(),
                showOrderError = timeOrderError,
                onStartTimeClick = {
                    pickerTarget = PickerTarget.START
                    showTimePicker = true
                },
                onEndTimeClick = {
                    pickerTarget = PickerTarget.END
                    showTimePicker = true
                },
            )

            Text(
                text = "*Обязательные поля",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
        }
    }
}

@Composable
private fun DateSection(
    startDateText: String,
    endDateText: String,
    startDate: LocalDate?,
    startDateError: String?,
    endDateError: String?,
    datesMatch: Boolean,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onDatesMatchChange: (Boolean) -> Unit,
) {
    SectionGroup(title = "Дата начала и окончания рыбалки *") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                DateField(
                    label = "Начало",
                    value = startDateText,
                    supportingText = startDateError,
                    isError = startDateError != null,
                    onClick = onStartDateClick,
                )
                DateField(
                    label = "Окончание",
                    value = endDateText,
                    enabled = !datesMatch,
                    supportingText = endDateError,
                    isError = endDateError != null,
                    onClick = onEndDateClick,
                )
            }

            FishingListItem(
                titleContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = datesMatch,
                            onCheckedChange = onDatesMatchChange,
                        )
                        Text(
                            text = "Даты совпадают",
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                contentPaddingStart = 16.dp,
                contentPaddingEnd = 16.dp,
                verticalContentPadding = 0.dp,
            )
        }
    }
}

@Composable
private fun TimeSection(
    startTime: String,
    endTime: String,
    showOrderError: Boolean,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
) {
    SectionGroup(
        title = "Время начала и окончания рыбалки *",
        showDivider = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TimeField(
                    label = "Начало",
                    value = startTime,
                    isError = showOrderError,
                    modifier = Modifier.weight(1f),
                    onClick = onStartTimeClick,
                )
                TimeField(
                    label = "Окончание",
                    value = endTime,
                    modifier = Modifier.weight(1f),
                    onClick = onEndTimeClick,
                )
            }
            if (showOrderError) {
                SupportingError(
                    text = "Время начала рыбалки не может быть позже окончания",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    onClick: () -> Unit,
) {
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            },
            isError = isError,
            supportingText = supportingText?.let { message ->
                { Text(text = message) }
            },
            singleLine = true,
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
            )
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onClick: () -> Unit,
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            },
            isError = isError,
            singleLine = true,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        )
    }
}

@Composable
private fun SupportingError(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 4.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

private enum class PickerTarget { START, END }

private fun parseDateText(value: String): LocalDate? {
    if (value.isBlank()) return null
    val parsed = runCatching {
        SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru")).parse(value)
    }.getOrNull() ?: return null
    return parsed.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun formatDateForViewModel(date: LocalDate): String {
    val localDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
    return SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
        .format(Date.from(localDate))
}

private fun combineDateAndTime(date: LocalDate?, time: String?): LocalDateTime? {
    if (date == null || time == null) return null
    val (hour, minute) = time.split(":").mapNotNull(String::toIntOrNull)
        .takeIf { it.size == 2 } ?: return null
    return runCatching { date.atTime(hour, minute) }.getOrNull()
}

@Preview(showBackground = true, widthDp = 412, heightDp = 820)
@Composable
private fun FishingDateTimeScreenPreview() {
    FishingTheme(dynamicColor = false) {
        FishingDateTimeScreen()
    }
}
