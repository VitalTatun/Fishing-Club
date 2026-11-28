package com.example.fishing.model

/**
 * One fish condition. [speciesId] is null for a free-text name that the
 * catalog could not resolve; matching then falls back to the name.
 */
data class SelectedFish(
    val name: String,
    val speciesId: String? = null
) {
    /** Identity for deduplication: catalog id first, then the display name. */
    fun sameAs(other: SelectedFish): Boolean =
        if (speciesId != null && other.speciesId != null) {
            speciesId == other.speciesId
        } else {
            name == other.name
        }
}
