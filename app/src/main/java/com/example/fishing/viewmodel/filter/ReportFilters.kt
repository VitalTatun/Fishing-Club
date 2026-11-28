package com.example.fishing.viewmodel.filter

import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.MapMarkerFilters
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.SelectedFish
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
    /** Map-only: show the viewer's own reports instead of public ones. */
    val isOwnOnly: Boolean = false,
    /** Selected fish; an empty list means "any fish". */
    val selectedFish: List<SelectedFish> = emptyList(),
    /** Map filters are multi-select; the search screen uses a single value. */
    val selectedMethods: Set<FishingMethod> = emptySet()
) {
    val isActive: Boolean
        get() = isFavoritesSelected ||
            isTrophySelected ||
            isPaidSelected ||
            isOwnOnly ||
            selectedFish.isNotEmpty() ||
            selectedMethods.isNotEmpty()

    /** Number of active map conditions, used by the map badge. */
    val activeCount: Int
        get() = listOf(
            isOwnOnly,
            isTrophySelected,
            isPaidSelected,
            selectedFish.isNotEmpty(),
            selectedMethods.isNotEmpty()
        ).count { it }

    fun matches(report: FishingReport, favoriteIds: Set<UUID>): Boolean {
        if (isFavoritesSelected && report.id !in favoriteIds) return false
        if (isTrophySelected && report.type != FishingType.HAUL) return false
        if (isPaidSelected && !report.water.isPaid) return false
        if (!matchesFish(
                names = report.fish.map { it.name },
                speciesIds = report.fish.mapNotNull { it.speciesId }
            )
        ) {
            return false
        }
        if (selectedMethods.isNotEmpty() && report.fishingMethod !in selectedMethods) return false
        return true
    }

    fun matches(marker: MarkerDomain, favoriteIds: Set<UUID>): Boolean {
        if (isFavoritesSelected && marker.id !in favoriteIds) return false
        if (isTrophySelected && marker.type != FishingType.HAUL) return false
        if (isPaidSelected && !marker.isPaidWater) return false
        if (!matchesFish(marker.fishNames, marker.fishSpeciesIds)) return false
        if (selectedMethods.isNotEmpty() && marker.fishingMethod !in selectedMethods) return false
        return true
    }

    /**
     * A report matches when at least one selected fish matches. Catalog ids
     * are checked first; the display name keeps free-text fish working.
     */
    private fun matchesFish(names: List<String>, speciesIds: Collection<String>): Boolean {
        if (selectedFish.isEmpty()) return true
        return selectedFish.any { fish ->
            (fish.speciesId != null && speciesIds.contains(fish.speciesId)) ||
                names.contains(fish.name)
        }
    }
}

/** Server-side projection of the map filters. Favorites do not leave the client. */
fun ReportFilters.toMapMarkerFilters(): MapMarkerFilters = MapMarkerFilters(
    trophyOnly = isTrophySelected,
    paidOnly = isPaidSelected,
    methods = selectedMethods,
    speciesIds = selectedFish.mapNotNull { it.speciesId }.toSet()
)

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
