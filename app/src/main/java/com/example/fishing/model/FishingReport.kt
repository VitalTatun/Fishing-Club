package com.example.fishing.model

import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FishingReport(
    val id: UUID,
    val userId: UUID,
    var publishedAt: Date? = null,
    var type: FishingType,
    var name: String,
    var water: Water,
    var spotLat: Double? = null,
    var spotLng: Double? = null,
    var photos: List<FishingPhoto>,
    var fishingStartAt: Instant? = null,
    var fishingEndAt: Instant? = null,
    var weight: Double,
    var fish: List<Fish>,
    var fishingMethod: FishingMethod,
    var bait: List<Bait>,
    var comment: String,
    val user: User,
    var fishingFromTheShore: Boolean,
    var isPublic: Boolean,
    var createdAt: Date? = null
) {
    val duration: Duration?
        get() {
            val start = fishingStartAt ?: return null
            val end = fishingEndAt ?: return null
            return Duration.between(start, end)
        }

    fun durationFormatted(): String? {
        val d = duration ?: return null
        val totalMinutes = d.toMinutes()
        if (totalMinutes < 0) return null

        val days = d.toDays()
        val hours = d.toHours() % 24
        val minutes = totalMinutes % 60

        return buildString {
            if (days > 0) append("$days д ")
            if (hours > 0) append("$hours ч ")
            if (minutes > 0 || (days == 0L && hours == 0L)) append("$minutes мин")
        }.trim()
    }

    fun fishingStartDate(): String? {
        val instant = fishingStartAt ?: return null
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }

    fun fishingStartTime(): String? {
        val instant = fishingStartAt ?: return null
        val formatter = DateTimeFormatter.ofPattern("H:mm", Locale.forLanguageTag("ru"))
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }

    fun fishingEndTime(): String? {
        val instant = fishingEndAt ?: return null
        val formatter = DateTimeFormatter.ofPattern("H:mm", Locale.forLanguageTag("ru"))
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}

enum class ReportSortOrder {
    BY_PUBLISH_DATE,
    BY_FISHING_TIME
}
