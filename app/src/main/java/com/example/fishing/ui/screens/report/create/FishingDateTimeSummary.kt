package com.example.fishing.ui.screens.report.create

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.fishing.R
import com.example.fishing.model.FishingDateTimeError
import com.example.fishing.ui.components.FishingListItem
import com.example.fishing.viewmodel.CreateReportViewModel
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun FishingDateTimeSummary(
    viewModel: CreateReportViewModel,
    onClick: () -> Unit,
) {
    val dateTimeErrors = viewModel.dateTimeErrors()
    val startDateSupportingText = when {
        FishingDateTimeError.START_IN_FUTURE in dateTimeErrors ->
            stringResource(R.string.error_start_in_future)

        FishingDateTimeError.START_MISSING in dateTimeErrors ->
            stringResource(R.string.error_start_missing)

        FishingDateTimeError.END_DATE_BEFORE_START in dateTimeErrors ->
            stringResource(R.string.error_end_date_before_start)

        else -> null
    }
    val timeSupportingText = when {
        FishingDateTimeError.END_IN_FUTURE in dateTimeErrors ->
            stringResource(R.string.error_end_in_future)

        FishingDateTimeError.END_NOT_AFTER_START in dateTimeErrors ->
            stringResource(R.string.error_end_not_after_start)

        FishingDateTimeError.END_MISSING in dateTimeErrors ->
            stringResource(R.string.error_end_missing)

        else -> null
    }
    val startDate = viewModel.formStartDate.orEmpty()
    val endDate = viewModel.formEndDate.orEmpty()
    val startTime = viewModel.formStartTime.orEmpty()
    val endTime = viewModel.formEndTime.orEmpty()
    val hasAnyValue = startDate.isNotBlank() || endDate.isNotBlank() ||
        startTime.isNotBlank() || endTime.isNotBlank()
    val notSpecified = stringResource(R.string.not_specified)
    val dateRange = remember(startDate, endDate, notSpecified) {
        formatDateRange(startDate, endDate, notSpecified)
    }
    val timeRange = remember(startTime, endTime, notSpecified) {
        formatTimeRange(startTime, endTime, notSpecified)
    }
    val startAt = viewModel.fishingStartAt()
    val endAt = viewModel.fishingEndAt()
    val duration = remember(startAt, endAt, notSpecified) {
        formatDuration(startAt, endAt) ?: notSpecified
    }

    FishingListItem(
        title = stringResource(R.string.date_time_title),
        supportingText = stringResource(R.string.date_time_summary_supporting),
        leadingIcon = Icons.Outlined.CalendarMonth,
        onRowClick = onClick,
    )

    if (hasAnyValue) {
        FishingListItem(
            title = stringResource(R.string.date),
            trailingText = dateRange,
            supportingText = startDateSupportingText,
            isError = startDateSupportingText != null,
            modifier = Modifier.padding(start = 40.dp),
        )
        FishingListItem(
            title = stringResource(R.string.time),
            trailingText = timeRange,
            supportingText = timeSupportingText,
            isError = timeSupportingText != null,
            modifier = Modifier.padding(start = 40.dp),
        )
        FishingListItem(
            title = stringResource(R.string.fishing_duration),
            trailingText = duration,
            modifier = Modifier.padding(start = 40.dp),
        )
    }
}

private fun formatDateRange(startText: String, endText: String, notSpecified: String): String {
    val locale = Locale.forLanguageTag("ru")
    val sourceFormat = SimpleDateFormat("d MMM yyyy", locale)
    val start = parseDate(startText, sourceFormat)
    val end = parseDate(endText, sourceFormat)
    val fullFormat = SimpleDateFormat("d MMMM yyyy", locale)
    val monthFormat = SimpleDateFormat("MMMM", locale)
    val monthAndYearFormat = SimpleDateFormat("MMMM yyyy", locale)

    if (start == null && end == null) return "$notSpecified - $notSpecified"
    if (start == null) return "$notSpecified - ${fullFormat.format(end!!)}"
    if (end == null) return "${fullFormat.format(start)} - $notSpecified"

    val startCalendar = Calendar.getInstance().apply { time = start }
    val endCalendar = Calendar.getInstance().apply { time = end }
    val sameYear = startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR)
    val sameMonth = sameYear &&
        startCalendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH)
    val dateRange = when {
        startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR) &&
            startCalendar.get(Calendar.DAY_OF_YEAR) == endCalendar.get(Calendar.DAY_OF_YEAR) ->
            fullFormat.format(start)

        sameMonth ->
            "${startCalendar.get(Calendar.DAY_OF_MONTH)} - " +
                "${endCalendar.get(Calendar.DAY_OF_MONTH)} " +
                monthAndYearFormat.format(end).replaceFirstChar { it.titlecase(locale) }

        sameYear ->
            "${startCalendar.get(Calendar.DAY_OF_MONTH)} ${monthFormat.format(start)} - " +
                "${endCalendar.get(Calendar.DAY_OF_MONTH)} ${monthAndYearFormat.format(end)}"

        else -> "${fullFormat.format(start)} - ${fullFormat.format(end)}"
    }
    return dateRange
}

private fun parseDate(value: String, formatter: SimpleDateFormat): Date? {
    if (value.isBlank()) return null
    return runCatching { formatter.parse(value) }.getOrNull()
}

private fun formatTimeRange(start: String, end: String, notSpecified: String): String {
    return "${start.ifBlank { notSpecified }} - ${end.ifBlank { notSpecified }}"
}

private fun formatDuration(start: Instant?, end: Instant?): String? {
    if (start == null || end == null || !start.isBefore(end)) return null

    val duration = Duration.between(start, end)
    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    return buildList {
        if (days > 0) add("$days ${russianPlural(days, "день", "дня", "дней")}")
        if (hours > 0) add("$hours ${russianPlural(hours, "час", "часа", "часов")}")
        if (minutes > 0 || isEmpty()) {
            add("$minutes ${russianPlural(minutes, "минута", "минуты", "минут")}")
        }
    }.joinToString(" ")
}

private fun russianPlural(value: Long, one: String, few: String, many: String): String {
    val mod100 = value % 100
    val mod10 = value % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1L -> one
        mod10 in 2L..4L -> few
        else -> many
    }
}
