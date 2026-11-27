package com.example.fishing.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class FishingDateTimeDraft(
    val startDate: LocalDate?,
    val startTime: LocalTime?,
    val endDate: LocalDate?,
    val endTime: LocalTime?,
)

enum class FishingDateTimeError {
    START_MISSING,
    END_MISSING,
    END_DATE_BEFORE_START,
    END_NOT_AFTER_START,
    START_IN_FUTURE,
    END_IN_FUTURE,
}

object FishingDateTimeValidator {

    fun validate(
        draft: FishingDateTimeDraft,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<FishingDateTimeError> {
        val errors = mutableListOf<FishingDateTimeError>()
        val today = now.atZone(zoneId).toLocalDate()
        val startMissing = draft.startDate == null || draft.startTime == null
        val endMissing = draft.endDate == null || draft.endTime == null

        if (draft.startDate?.isAfter(today) == true) {
            errors.add(FishingDateTimeError.START_IN_FUTURE)
        }
        if (draft.endDate?.isAfter(today) == true) {
            errors.add(FishingDateTimeError.END_IN_FUTURE)
        }

        if (startMissing) {
            errors.add(FishingDateTimeError.START_MISSING)
        }
        if (!startMissing && endMissing) {
            errors.add(FishingDateTimeError.END_MISSING)
        }

        val startDate = draft.startDate
        val endDate = draft.endDate
        if (startDate != null && endDate != null) {
            when {
                endDate.isBefore(startDate) ->
                    errors.add(FishingDateTimeError.END_DATE_BEFORE_START)

                !startMissing &&
                    !endMissing &&
                    !LocalDateTime.of(startDate, draft.startTime)
                        .isBefore(LocalDateTime.of(endDate, draft.endTime)) ->
                    errors.add(FishingDateTimeError.END_NOT_AFTER_START)
            }
        }

        val startAt = draft.startDate?.let { date ->
            draft.startTime?.let(date::atTime)?.atZone(zoneId)?.toInstant()
        }
        val endAt = draft.endDate?.let { date ->
            draft.endTime?.let(date::atTime)?.atZone(zoneId)?.toInstant()
        }

        if (startAt?.isAfter(now) == true &&
            FishingDateTimeError.START_IN_FUTURE !in errors
        ) {
            errors.add(FishingDateTimeError.START_IN_FUTURE)
        }
        if (endAt?.isAfter(now) == true &&
            FishingDateTimeError.END_IN_FUTURE !in errors
        ) {
            errors.add(FishingDateTimeError.END_IN_FUTURE)
        }

        return errors
    }

    fun validate(
        start: Instant?,
        end: Instant?,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<FishingDateTimeError> {
        val startLocal = start?.atZone(zoneId)
        val endLocal = end?.atZone(zoneId)
        return validate(
            draft = FishingDateTimeDraft(
                startDate = startLocal?.toLocalDate(),
                startTime = startLocal?.toLocalTime(),
                endDate = endLocal?.toLocalDate(),
                endTime = endLocal?.toLocalTime(),
            ),
            now = now,
            zoneId = zoneId,
        )
    }

    fun isValid(start: Instant?, end: Instant?, now: Instant): Boolean {
        return validate(start, end, now).isEmpty()
    }

    fun isValid(start: Instant?, end: Instant?): Boolean {
        if (start == null || end == null) return false
        return start.isBefore(end)
    }
}
