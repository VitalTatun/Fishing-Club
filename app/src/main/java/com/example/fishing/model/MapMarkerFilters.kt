package com.example.fishing.model

/**
 * Server-side marker filters. Kept separate from the UI filter state so the
 * data layer does not depend on the map screen's draft/apply lifecycle.
 */
data class MapMarkerFilters(
    val trophyOnly: Boolean = false,
    val paidOnly: Boolean = false,
    val method: FishingMethod? = null,
    /** Catalog `fish_species.id`; the filter is single-select. */
    val speciesId: String? = null
) {
    val isActive: Boolean
        get() = trophyOnly || paidOnly || method != null || speciesId != null
}
