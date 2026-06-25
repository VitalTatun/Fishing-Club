package com.example.fishing.data.room

import com.example.fishing.model.*
import java.util.UUID

fun FishingReportWithDetails.toDomain(user: User): FishingReport {
    return FishingReport(
        id = fishing.id,
        userId = fishing.userId,
        publishedAt = fishing.publishedAt,
        type = FishingType.valueOf(fishing.type),
        name = fishing.name,
        water = Water(
            waterName = fishing.waterName ?: "",
            latitude = fishing.waterLat ?: 0.0,
            longitude = fishing.waterLng ?: 0.0,
            isPaid = fishing.waterPaid
        ),
        spotLat = fishing.spotLat,
        spotLng = fishing.spotLng,
        photo = photos.map { it.storagePath },
        fishingTime = fishing.fishingTime,
        weight = fishing.weight,
        fish = fish.map { it.toDomain() },
        fishingMethod = FishingMethod.valueOf(fishing.fishingMethod ?: FishingMethod.NONE.name),
        bait = baits.map { Bait.valueOf(it.baitCode) },
        comment = fishing.comment ?: "",
        user = user,
        fishingFromTheShore = fishing.shore,
        isPublic = fishing.isPublic,
        isSynced = fishing.isSynced
    )
}

fun FishEntity.toDomain(): Fish {
    return Fish(
        id = id,
        name = name,
        count = count
    )
}

fun FishingReport.toEntity(): FishingEntity {
    return FishingEntity(
        id = id,
        userId = userId,
        publishedAt = publishedAt,
        type = type.name,
        name = name,
        waterName = water.waterName,
        waterLat = water.latitude,
        waterLng = water.longitude,
        waterPaid = water.isPaid,
        spotLat = spotLat,
        spotLng = spotLng,
        fishingTime = fishingTime,
        fishingMethod = fishingMethod.name,
        comment = comment,
        isPublic = isPublic,
        weight = weight,
        shore = fishingFromTheShore,
        isSynced = isSynced
    )
}

fun Fish.toEntity(fishingId: UUID): FishEntity {
    return FishEntity(
        id = id,
        fishingId = fishingId,
        name = name,
        count = count
    )
}

fun ProfileEntity.toDomain(): User {
    return User(
        id = id,
        name = name ?: "",
        email = email ?: "",
        image = avatarUrl ?: ""
    )
}

fun User.toEntity(): ProfileEntity {
    return ProfileEntity(
        id = id,
        name = name,
        email = email,
        avatarUrl = image
    )
}
