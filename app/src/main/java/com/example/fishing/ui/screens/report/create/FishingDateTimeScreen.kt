package com.example.fishing.ui.screens.report.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import com.example.fishing.R
import com.example.fishing.model.FishingDateTimeDraft
import com.example.fishing.model.FishingDateTimeError
import com.example.fishing.model.FishingDateTimeValidator
import com.example.fishing.ui.components.FishingListItem
import com.example.fishing.ui.components.SectionGroup
import com.example.fishing.ui.theme.FishingTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    )
    val contentMaxWidth = if (isExpandedWidth) 1_120.dp else 640.dp

    var startDateValue by rememberSaveable(startDateText) {
        mutableStateOf(parseDateText(startDateText)?.toString().orEmpty())
    }
    var endDateValue by rememberSaveable(startDateText, endDateText) {
        val parsedStart = parseDateText(startDateText)
        mutableStateOf(
            (parseDateText(endDateText) ?: parsedStart.takeIf { endDateText.isBlank() })
                ?.toString()
                .orEmpty()
        )
    }
    val startDate = remember(startDateValue) { parseIsoDate(startDateValue) }
    val endDate = remember(endDateValue) { parseIsoDate(endDateValue) }
    var startTimeValue by rememberSaveable(startTimeText) {
        mutableStateOf(startTimeText)
    }
    var endTimeValue by rememberSaveable(endTimeText) {
        mutableStateOf(endTimeText)
    }
    val startTime = startTimeValue.takeIf(String::isNotBlank)
    val endTime = endTimeValue.takeIf(String::isNotBlank)
    var datesMatch by rememberSaveable(startDateText, endDateText) {
        mutableStateOf(endDateText.isBlank() || startDateText == endDateText)
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var pickerTargetName by rememberSaveable { mutableStateOf(PickerTarget.START.name) }
    val pickerTarget = PickerTarget.valueOf(pickerTargetName)

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
    }
    val dateTimeErrors = FishingDateTimeValidator.validate(
        draft = FishingDateTimeDraft(
            startDate = startDate,
            startTime = startTime?.let(::parseTimeText),
            endDate = endDate,
            endTime = endTime?.let(::parseTimeText),
        ),
        now = Instant.now(),
    )
    val startDateError = if (FishingDateTimeError.START_IN_FUTURE in dateTimeErrors) {
        stringResource(R.string.error_start_in_future)
    } else {
        null
    }
    val endDateError = when {
        FishingDateTimeError.END_DATE_BEFORE_START in dateTimeErrors ->
            stringResource(R.string.error_end_date_before_start)

        FishingDateTimeError.END_IN_FUTURE in dateTimeErrors ->
            stringResource(R.string.error_end_in_future)

        else -> null
    }
    val timeOrderError = FishingDateTimeError.END_NOT_AFTER_START in dateTimeErrors
    val canConfirm = dateTimeErrors.isEmpty()

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
                        startDateValue = selectedDate.toString()
                        if (datesMatch) endDateValue = selectedDate.toString()
                    } else {
                        endDateValue = selectedDate.toString()
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
                if (pickerTarget == PickerTarget.START) {
                    startTimeValue = formatted
                } else {
                    endTimeValue = formatted
                }
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
                title = { Text(stringResource(R.string.date_time_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
                                    startTimeValue,
                                    formatDateForViewModel(selectedEndDate),
                                    endTimeValue,
                                )
                            }
                        },
                        enabled = canConfirm,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.date_time_done),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = contentMaxWidth)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isExpandedWidth) 24.dp else 0.dp),
            ) {
                FishingDateTimeSections(
                    isExpandedWidth = isExpandedWidth,
                    startDateText = startDate?.format(dateFormatter)
                        ?: stringResource(R.string.not_specified),
                    endDateText = endDate?.format(dateFormatter)
                        ?: stringResource(R.string.not_specified),
                    startDateError = startDateError,
                    endDateError = endDateError,
                    datesMatch = datesMatch,
                    startTime = startTimeValue,
                    endTime = endTimeValue,
                    showOrderError = timeOrderError,
                    onStartDateClick = {
                        pickerTargetName = PickerTarget.START.name
                        showDatePicker = true
                    },
                    onEndDateClick = {
                        pickerTargetName = PickerTarget.END.name
                        showDatePicker = true
                    },
                    onDatesMatchChange = { checked ->
                        datesMatch = checked
                        if (checked) endDateValue = startDateValue
                    },
                    onStartTimeClick = {
                        pickerTargetName = PickerTarget.START.name
                        showTimePicker = true
                    },
                    onEndTimeClick = {
                        pickerTargetName = PickerTarget.END.name
                        showTimePicker = true
                    },
                )

                Text(
                    text = stringResource(R.string.required_fields),
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
}

@Composable
private fun FishingDateTimeSections(
    isExpandedWidth: Boolean,
    startDateText: String,
    endDateText: String,
    startDateError: String?,
    endDateError: String?,
    datesMatch: Boolean,
    startTime: String,
    endTime: String,
    showOrderError: Boolean,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onDatesMatchChange: (Boolean) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
) {
    if (isExpandedWidth) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top,
        ) {
            DateSection(
                startDateText = startDateText,
                endDateText = endDateText,
                startDateError = startDateError,
                endDateError = endDateError,
                datesMatch = datesMatch,
                onStartDateClick = onStartDateClick,
                onEndDateClick = onEndDateClick,
                onDatesMatchChange = onDatesMatchChange,
                modifier = Modifier.weight(1f),
                showDivider = false,
            )

            TimeSection(
                startTime = startTime,
                endTime = endTime,
                showOrderError = showOrderError,
                onStartTimeClick = onStartTimeClick,
                onEndTimeClick = onEndTimeClick,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            DateSection(
                startDateText = startDateText,
                endDateText = endDateText,
                startDateError = startDateError,
                endDateError = endDateError,
                datesMatch = datesMatch,
                onStartDateClick = onStartDateClick,
                onEndDateClick = onEndDateClick,
                onDatesMatchChange = onDatesMatchChange,
            )

            TimeSection(
                startTime = startTime,
                endTime = endTime,
                showOrderError = showOrderError,
                onStartTimeClick = onStartTimeClick,
                onEndTimeClick = onEndTimeClick,
            )
        }
    }
}

@Composable
private fun DateSection(
    startDateText: String,
    endDateText: String,
    startDateError: String?,
    endDateError: String?,
    datesMatch: Boolean,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onDatesMatchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    SectionGroup(
        modifier = modifier,
        title = stringResource(R.string.date_range_required),
        showDivider = showDivider,
    ) {
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
                    label = stringResource(R.string.fishing_start),
                    value = startDateText,
                    supportingText = startDateError,
                    isError = startDateError != null,
                    onClick = onStartDateClick,
                )
                DateField(
                    label = stringResource(R.string.fishing_end),
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
                            text = stringResource(R.string.dates_match),
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
    modifier: Modifier = Modifier,
) {
    SectionGroup(
        modifier = modifier,
        title = stringResource(R.string.time_range_required),
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
                    label = stringResource(R.string.fishing_start),
                    value = startTime,
                    isError = showOrderError,
                    modifier = Modifier.weight(1f),
                    onClick = onStartTimeClick,
                )
                TimeField(
                    label = stringResource(R.string.fishing_end),
                    value = endTime,
                    modifier = Modifier.weight(1f),
                    onClick = onEndTimeClick,
                )
            }
            if (showOrderError) {
                SupportingError(
                    text = stringResource(R.string.error_fishing_time_order),
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

private fun parseIsoDate(value: String): LocalDate? {
    if (value.isBlank()) return null
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

private fun formatDateForViewModel(date: LocalDate): String {
    val localDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
    return SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
        .format(Date.from(localDate))
}

private fun parseTimeText(value: String): LocalTime? {
    val separator = value.indexOf(':')
    if (separator <= 0 || separator == value.lastIndex) return null

    val hour = value.substring(0, separator).toIntOrNull() ?: return null
    val minute = value.substring(separator + 1).toIntOrNull() ?: return null
    return runCatching { LocalTime.of(hour, minute) }.getOrNull()
}

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Composable
private fun FishingDateTimeScreenPreview() {
    FishingTheme(dynamicColor = false) {
        FishingDateTimeScreen()
    }
}
