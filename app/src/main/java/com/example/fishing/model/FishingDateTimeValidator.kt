package com.example.fishing.model

import java.time.Instant

enum class FishingDateTimeError {
    START_MISSING,
    END_MISSING,
    END_NOT_AFTER_START,
    START_IN_FUTURE,
    END_IN_FUTURE
}

object FishingDateTimeValidator {

    fun validate(start: Instant?, end: Instant?, now: Instant): List<FishingDateTimeError> {
        if (start == null) return listOf(FishingDateTimeError.START_MISSING)
        if (end == null) return listOf(FishingDateTimeError.END_MISSING)

        val errors = mutableListOf<FishingDateTimeError>()

        if (!start.isBefore(end)) {
            errors.add(FishingDateTimeError.END_NOT_AFTER_START)
        }

        if (start.isAfter(now)) {
            errors.add(FishingDateTimeError.START_IN_FUTURE)
        }

        if (end.isAfter(now)) {
            errors.add(FishingDateTimeError.END_IN_FUTURE)
        }

        return errors
    }

    fun isValid(start: Instant?, end: Instant?, now: Instant): Boolean {
        return validate(start, end, now).isEmpty()
    }

    fun isValid(start: Instant?, end: Instant?): Boolean {
        if (start == null || end == null) return false
        return start.isBefore(end)
    }
}