package com.example.fishing.model

import java.time.Instant
import java.util.UUID

data class MarkerDomain(
    val id: UUID,
    val name: String,
    val waterName: String,
    val waterLat: Double,
    val waterLng: Double,
    val type: FishingType,
    val fishingMethod: FishingMethod,
    val fishingStartAt: Instant?,
    val isPublic: Boolean,
    val isPaidWater: Boolean = false,
    /**
     * Canonical names for server-returned markers, raw names for own offline
     * rows whose species could not be resolved.
     */
    val fishNames: List<String> = emptyList(),
    /** Catalog ids resolved for the report's fish, used by the species filter. */
    val fishSpeciesIds: Set<String> = emptySet()
)
