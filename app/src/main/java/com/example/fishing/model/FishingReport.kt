package com.example.fishing.model

import java.util.Date
import java.util.UUID

data class FishingReport(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    var publishedAt: Date? = null,
    var type: FishingType,
    var name: String,
    var water: Water,
    var spotLat: Double? = null,
    var spotLng: Double? = null,
    var photo: List<String>, // Paths or URLs
    var fishingTime: Date,
    var weight: Double,
    var fish: List<Fish>,
    var fishingMethod: FishingMethod,
    var bait: List<Bait>,
    var comment: String,
    val user: User,
    var fishingFromTheShore: Boolean,
    var isPublic: Boolean
)
