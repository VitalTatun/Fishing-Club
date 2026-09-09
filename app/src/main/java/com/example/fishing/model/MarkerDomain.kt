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
    val fishNames: List<String> = emptyList()
)
