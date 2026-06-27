package com.example.fishing.model

import java.util.Date
import java.util.UUID

data class MarkerDomain(
    val id: UUID,
    val name: String,
    val waterName: String,
    val waterLat: Double,
    val waterLng: Double,
    val type: FishingType,
    val fishingMethod: FishingMethod,
    val fishingTime: Date,
    val isPublic: Boolean
)