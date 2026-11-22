package com.example.fishing.model

/**
 * A permanent entry of the fish catalog shared with the achievements system.
 *
 * [id] is a stable Latin slug (e.g. `esox_lucius`) and never changes when the
 * display name is edited. [aliases] holds spelling variants, cases and common
 * synonyms used by search and by name-to-species resolution.
 */
data class FishSpecies(
    val id: String,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val isPopular: Boolean = false,
    val sortOrder: Int = 0
)
