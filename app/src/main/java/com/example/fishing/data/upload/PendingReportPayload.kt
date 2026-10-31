package com.example.fishing.data.upload

import com.example.fishing.data.supabase.UuidSerializer
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingPhoto
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.User
import com.example.fishing.model.Water
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stable, versioned representation of a report waiting for upload. Domain
 * models are not [Serializable], and Java serialization would break on model
 * changes, so the queue stores this explicit DTO instead.
 */
@Serializable
internal data class PendingReportPayload(
    val schemaVersion: Int = SCHEMA_VERSION,
    @Serializable(with = UuidSerializer::class) val id: UUID,
    @Serializable(with = UuidSerializer::class) val userId: UUID,
    val publishedAtMillis: Long? = null,
    val type: String,
    val name: String,
    val water: PendingWaterPayload,
    val spotLat: Double? = null,
    val spotLng: Double? = null,
    val photos: List<PendingPhotoPayload>,
    val fishingStartAtMillis: Long? = null,
    val fishingEndAtMillis: Long? = null,
    val weight: Double,
    val fish: List<PendingFishPayload>,
    val fishingMethod: String,
    val bait: List<String>,
    val comment: String,
    val user: PendingUserPayload,
    val fishingFromTheShore: Boolean,
    val isPublic: Boolean,
    val createdAtMillis: Long? = null
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
internal data class PendingWaterPayload(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val waterName: String,
    val latitude: Double,
    val longitude: Double,
    val isPaid: Boolean
)

@Serializable
internal data class PendingPhotoPayload(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val url: String
)

@Serializable
internal data class PendingFishPayload(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String,
    val count: Int
)

@Serializable
internal data class PendingUserPayload(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val image: String,
    val name: String,
    val email: String
)

/** Encodes reports for [com.example.fishing.data.local.entity.PendingReportUploadEntity]. */
@Singleton
class PendingReportPayloadCodec @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(report: FishingReport): String = json.encodeToString(
        PendingReportPayload.serializer(),
        report.toPayload()
    )

    fun decode(raw: String): FishingReport = json.decodeFromString(
        PendingReportPayload.serializer(),
        raw
    ).toDomain()

    fun encodePaths(paths: List<String>): String = json.encodeToString(paths)

    fun decodePaths(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
}

private fun FishingReport.toPayload(): PendingReportPayload = PendingReportPayload(
    id = id,
    userId = userId,
    publishedAtMillis = publishedAt?.time,
    type = type.name,
    name = name,
    water = PendingWaterPayload(
        id = water.id,
        waterName = water.waterName,
        latitude = water.latitude,
        longitude = water.longitude,
        isPaid = water.isPaid
    ),
    spotLat = spotLat,
    spotLng = spotLng,
    photos = photos.map { PendingPhotoPayload(id = it.id, url = it.url) },
    fishingStartAtMillis = fishingStartAt?.toEpochMilli(),
    fishingEndAtMillis = fishingEndAt?.toEpochMilli(),
    weight = weight,
    fish = fish.map { PendingFishPayload(id = it.id, name = it.name, count = it.count) },
    fishingMethod = fishingMethod.name,
    bait = bait.map { it.name },
    comment = comment,
    user = PendingUserPayload(
        id = user.id,
        image = user.image,
        name = user.name,
        email = user.email
    ),
    fishingFromTheShore = fishingFromTheShore,
    isPublic = isPublic,
    createdAtMillis = createdAt?.time
)

private fun PendingReportPayload.toDomain(): FishingReport = FishingReport(
    id = id,
    userId = userId,
    publishedAt = publishedAtMillis?.let(::Date),
    type = runCatching { FishingType.valueOf(type) }.getOrDefault(FishingType.FISHING_LOG),
    name = name,
    water = Water(
        id = water.id,
        waterName = water.waterName,
        latitude = water.latitude,
        longitude = water.longitude,
        isPaid = water.isPaid
    ),
    spotLat = spotLat,
    spotLng = spotLng,
    photos = photos.map { FishingPhoto(id = it.id, url = it.url) },
    fishingStartAt = fishingStartAtMillis?.let(Instant::ofEpochMilli),
    fishingEndAt = fishingEndAtMillis?.let(Instant::ofEpochMilli),
    weight = weight,
    fish = fish.map { Fish(id = it.id, name = it.name, count = it.count) },
    fishingMethod = runCatching { FishingMethod.valueOf(fishingMethod) }
        .getOrDefault(FishingMethod.NONE),
    bait = bait.mapNotNull { name -> runCatching { Bait.valueOf(name) }.getOrNull() },
    comment = comment,
    user = User(
        id = user.id,
        image = user.image,
        name = user.name,
        email = user.email
    ),
    fishingFromTheShore = fishingFromTheShore,
    isPublic = isPublic,
    createdAt = createdAtMillis?.let(::Date)
)
