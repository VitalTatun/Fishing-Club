package com.example.fishing.viewmodel.filter

import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.MarkerDomain
import java.util.Calendar
import java.util.UUID

/**
 * Filter selection shared by the report list search and the map.
 *
 * Both screens used to implement these predicates independently; keeping them
 * here guarantees that a rule change is applied once and behaves identically
 * for [FishingReport] and [MarkerDomain].
 */
data class ReportFilters(
    val isFavoritesSelected: Boolean = false,
    val isTrophySelected: Boolean = false,
    val isPaidSelected: Boolean = false,
    val selectedCatch: String? = null,
    val selectedMethod: FishingMethod? = null
) {
    val isActive: Boolean
        get() = isFavoritesSelected ||
            isTrophySelected ||
            isPaidSelected ||
            selectedCatch != null ||
            selectedMethod != null

    fun matches(report: FishingReport, favoriteIds: Set<UUID>): Boolean {
        if (isFavoritesSelected && report.id !in favoriteIds) return false
        if (isTrophySelected && report.type != FishingType.HAUL) return false
        if (isPaidSelected && !report.water.isPaid) return false
        if (selectedCatch != null && report.fish.none { it.name == selectedCatch }) return false
        if (selectedMethod != null && report.fishingMethod != selectedMethod) return false
        return true
    }

    fun matches(marker: MarkerDomain, favoriteIds: Set<UUID>): Boolean {
        if (isFavoritesSelected && marker.id !in favoriteIds) return false
        if (isTrophySelected && marker.type != FishingType.HAUL) return false
        if (isPaidSelected && !marker.isPaidWater) return false
        if (selectedCatch != null && !marker.fishNames.contains(selectedCatch)) return false
        if (selectedMethod != null && marker.fishingMethod != selectedMethod) return false
        return true
    }
}

/**
 * Complete search form state. [query] and [selectedDateMillis] are specific to
 * the report list; the remaining filters are shared with the map.
 */
data class ReportSearchState(
    val query: String = "",
    val selectedDateMillis: Long? = null,
    val filters: ReportFilters = ReportFilters()
) {
    val isEmpty: Boolean
        get() = query.isBlank() && selectedDateMillis == null && !filters.isActive

    fun matches(report: FishingReport, favoriteIds: Set<UUID>): Boolean {
        if (!query.isBlank()) {
            val matchesQuery = report.name.contains(query, ignoreCase = true) ||
                report.water.waterName.contains(query, ignoreCase = true) ||
                report.comment.contains(query, ignoreCase = true) ||
                report.fish.any { it.name.contains(query, ignoreCase = true) }
            if (!matchesQuery) return false
        }

        if (selectedDateMillis != null) {
            val reportInstant = report.fishingStartAt ?: return false
            val reportCalendar = Calendar.getInstance().apply {
                timeInMillis = reportInstant.toEpochMilli()
            }
            val filterCalendar = Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
            }
            val sameDay = reportCalendar.get(Calendar.DAY_OF_YEAR) ==
                filterCalendar.get(Calendar.DAY_OF_YEAR)
            val sameYear = reportCalendar.get(Calendar.YEAR) ==
                filterCalendar.get(Calendar.YEAR)
            if (!sameDay || !sameYear) return false
        }

        return filters.matches(report, favoriteIds)
    }
}
