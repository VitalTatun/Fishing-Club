package com.example.fishing.testutil

import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.User
import com.example.fishing.model.Water
import java.time.Instant
import java.util.Date
import java.util.UUID

fun testUser(id: UUID = UUID.randomUUID()): User = User(
    id = id,
    name = "Test User",
    email = "test@example.com",
    image = ""
)

fun testReport(
    id: UUID = UUID.randomUUID(),
    userId: UUID = UUID.randomUUID(),
    type: FishingType = FishingType.FISHING_LOG,
    name: String = "Test Report",
    waterName: String = "Test Lake",
    isPaidWater: Boolean = false,
    fishingStartAt: Instant? = Instant.parse("2026-05-01T08:00:00Z"),
    fish: List<Fish> = listOf(Fish(id = UUID.randomUUID(), name = "Pike", count = 1)),
    fishingMethod: FishingMethod = FishingMethod.SPINNING,
    comment: String = "Test comment",
    publishedAt: Date? = null,
    createdAt: Date? = null,
    isPublic: Boolean = true
): FishingReport = FishingReport(
    id = id,
    userId = userId,
    publishedAt = publishedAt,
    type = type,
    name = name,
    water = Water(
        waterName = waterName,
        latitude = 55.0,
        longitude = 37.0,
        isPaid = isPaidWater
    ),
    photos = emptyList(),
    fishingStartAt = fishingStartAt,
    weight = 1.0,
    fish = fish,
    fishingMethod = fishingMethod,
    bait = listOf(Bait.WOBBLER),
    comment = comment,
    user = testUser(userId),
    fishingFromTheShore = true,
    isPublic = isPublic,
    createdAt = createdAt
)

fun testMarker(
    id: UUID = UUID.randomUUID(),
    type: FishingType = FishingType.FISHING_LOG,
    isPaidWater: Boolean = false,
    fishNames: List<String> = listOf("Pike"),
    fishingMethod: FishingMethod = FishingMethod.SPINNING
): MarkerDomain = MarkerDomain(
    id = id,
    name = "Test Report",
    waterName = "Test Lake",
    waterLat = 55.0,
    waterLng = 37.0,
    type = type,
    fishingMethod = fishingMethod,
    fishingStartAt = null,
    isPublic = true,
    isPaidWater = isPaidWater,
    fishNames = fishNames
)
