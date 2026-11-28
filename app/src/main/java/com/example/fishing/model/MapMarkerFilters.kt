package com.example.fishing.model

/**
 * Server-side marker filters. Kept separate from the UI filter state so the
 * data layer does not depend on the map screen's draft/apply lifecycle.
 */
data class MapMarkerFilters(
    val trophyOnly: Boolean = false,
    val paidOnly: Boolean = false,
    val method: FishingMethod? = null,
    /** Catalog `fish_species.id` values; an empty set means "any fish". */
    val speciesIds: Set<String> = emptySet()
) {
    val isActive: Boolean
        get() = trophyOnly || paidOnly || method != null || speciesIds.isNotEmpty()
}
