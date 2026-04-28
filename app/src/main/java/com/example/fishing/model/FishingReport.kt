package com.example.fishing.model

import java.util.Date
import java.util.UUID

data class FishingReport(
    val id: UUID = UUID.randomUUID(),
    var type: FishingType,
    var name: String,
    var water: Water,
    var photo: List<Int>, // List of drawable resource IDs for testing
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
