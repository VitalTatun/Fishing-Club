package com.example.fishing.data.report

import com.example.fishing.data.local.entity.MarkerEntity
import com.example.fishing.data.local.entity.ReportDetailsEntity
import com.example.fishing.data.supabase.BaitDto
import com.example.fishing.data.supabase.FishDto
import com.example.fishing.data.supabase.FishingDto
import com.example.fishing.data.supabase.PhotoDto
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingPhoto
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.User
import com.example.fishing.model.Water
import java.util.UUID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private val reportJson = Json { ignoreUnknownKeys = true }

/** Builds the local cache row for a report and its already-fetched children. */
internal fun FishingDto.toReportDetailsEntity(
    fish: List<FishDto>,
    baits: List<BaitDto>,
    photos: List<PhotoDto>,
    authorName: String?,
    authorAvatar: String?
): ReportDetailsEntity {
    return ReportDetailsEntity(
        id = id,
        userId = userId,
        publishedAt = publishedAt,
        type = type,
        name = name,
        waterName = waterName,
        waterLat = waterLat,
        waterLng = waterLng,
        waterPaid = waterPaid,
        spotLat = spotLat,
        spotLng = spotLng,
        fishingStartAt = fishingStartAt,
        fishingEndAt = fishingEndAt,
        weight = weight,
        fishingMethod = fishingMethod,
        comment = comment,
        shore = shore,
        isPublic = isPublic,
        imageUrls = photos.map { it.storagePath },
        fishJson = reportJson.encodeToString(fish),
        baitsJson = reportJson.encodeToString(baits),
        authorName = authorName,
        authorAvatar = authorAvatar,
        createdAt = createdAt,
        likesCount = likesCount
    )
}

internal fun RemoteReport.toReportDetailsEntity(
    resolveImageUrl: (String) -> String
): ReportDetailsEntity = fishing.toReportDetailsEntity(
    fish = fish,
    baits = baits,
    photos = photos,
    authorName = author?.name,
    authorAvatar = author?.avatarUrl?.let(resolveImageUrl)
)

/**
 * Direct remote-to-domain mapping for scenarios that never touch the Room cache
 * (the public user screen), so they do not have to build a cache row first.
 */
internal fun RemoteReport.toDomain(resolveImageUrl: (String) -> String): FishingReport =
    toReportDetailsEntity(resolveImageUrl).toDomain()

internal fun FishingDto.toMarkerEntity(fishNames: List<String>): MarkerEntity {
    return MarkerEntity(
        id = id,
        userId = userId,
        name = name,
        waterName = waterName ?: "",
        waterLat = waterLat ?: 0.0,
        waterLng = waterLng ?: 0.0,
        type = type,
        fishingMethod = fishingMethod ?: "",
        fishingStartAt = fishingStartAt,
        isPublic = isPublic,
        isPaidWater = waterPaid,
        fishNames = fishNames
    )
}

internal fun MarkerEntity.toDomain(): MarkerDomain {
    return MarkerDomain(
        id = id,
        name = name,
        waterName = waterName,
        waterLat = waterLat,
        waterLng = waterLng,
        type = parseEnumValue(type, FishingType.entries.toTypedArray()) ?: FishingType.FISHING_LOG,
        fishingMethod = parseEnumValue(fishingMethod, FishingMethod.entries.toTypedArray()) ?: FishingMethod.NONE,
        fishingStartAt = SupabaseDateFormats.parseInstant(fishingStartAt),
        isPublic = isPublic,
        isPaidWater = isPaidWater,
        fishNames = fishNames
    )
}

internal fun ReportDetailsEntity.toDomain(): FishingReport {
    val fish = reportJson.decodeFromString<List<FishDto>>(fishJson)
    val baits = reportJson.decodeFromString<List<BaitDto>>(baitsJson)

    return FishingReport(
        id = id,
        userId = userId,
        publishedAt = publishedAt?.let { SupabaseDateFormats.parseDate(it) },
        type = parseEnumValue(type, FishingType.entries.toTypedArray()) ?: FishingType.FISHING_LOG,
        name = name,
        water = Water(
            waterName = waterName ?: "",
            latitude = waterLat ?: 0.0,
            longitude = waterLng ?: 0.0,
            isPaid = waterPaid
        ),
        spotLat = spotLat,
        spotLng = spotLng,
        photos = imageUrls.map { url ->
            // Photo ids are not stored in Room; derive a stable one from the path
            // so recompositions keep the same key.
            FishingPhoto(id = UUID.nameUUIDFromBytes(url.toByteArray()), url = url)
        },
        fishingStartAt = SupabaseDateFormats.parseInstant(fishingStartAt),
        fishingEndAt = SupabaseDateFormats.parseInstant(fishingEndAt),
        weight = weight,
        fish = fish.map { Fish(id = it.id, name = it.name, count = it.count) },
        fishingMethod = parseEnumValue(fishingMethod, FishingMethod.entries.toTypedArray()) ?: FishingMethod.NONE,
        bait = baits.map {
            parseEnumValue(it.baitCode, Bait.entries.toTypedArray()) ?: Bait.NONE
        },
        comment = comment ?: "",
        user = User(
            id = userId,
            name = authorName.orEmpty(),
            email = "",
            image = authorAvatar.orEmpty()
        ),
        fishingFromTheShore = shore,
        isPublic = isPublic,
        createdAt = createdAt?.let { SupabaseDateFormats.parseDate(it) }
    )
}

internal fun FishingReport.toFishingDto(now: String): FishingDto {
    return FishingDto(
        id = id,
        userId = userId,
        publishedAt = publishedAt?.let { SupabaseDateFormats.formatDate(it) },
        type = type.name,
        name = name,
        waterName = water.waterName,
        waterLat = water.latitude,
        waterLng = water.longitude,
        waterPaid = water.isPaid,
        spotLat = spotLat,
        spotLng = spotLng,
        fishingStartAt = SupabaseDateFormats.formatInstant(fishingStartAt),
        fishingEndAt = SupabaseDateFormats.formatInstant(fishingEndAt),
        weight = weight,
        fishingMethod = fishingMethod.name,
        comment = comment,
        shore = fishingFromTheShore,
        isPublic = isPublic,
        createdAt = createdAt?.let { SupabaseDateFormats.formatDate(it) } ?: now,
        updatedAt = now
    )
}

/**
 * Builds the payload for the atomic [publish_report] RPC. Missing optional
 * values are simply omitted: the SQL function treats a missing key as null.
 */
internal fun buildPublishPayload(
    dto: FishingDto,
    fish: List<FishDto>,
    baits: List<BaitDto>,
    photos: List<PhotoDto>
): JsonObject = buildJsonObject {
    put("id", dto.id.toString())
    put("user_id", dto.userId.toString())
    put("type", dto.type)
    put("name", dto.name)
    dto.waterName?.let { put("water_name", it) }
    dto.waterLat?.let { put("water_lat", it) }
    dto.waterLng?.let { put("water_lng", it) }
    put("water_paid", dto.waterPaid)
    dto.spotLat?.let { put("spot_lat", it) }
    dto.spotLng?.let { put("spot_lng", it) }
    dto.fishingStartAt?.let { put("fishing_start_at", it) }
    dto.fishingEndAt?.let { put("fishing_end_at", it) }
    put("weight", dto.weight)
    dto.fishingMethod?.let { put("fishing_method", it) }
    dto.comment?.let { put("comment", it) }
    put("shore", dto.shore)
    put("is_public", dto.isPublic)
    dto.createdAt?.let { put("created_at", it) }
    dto.publishedAt?.let { put("published_at", it) }

    putJsonArray("fish") {
        fish.forEach { item ->
            addJsonObject {
                put("id", item.id.toString())
                put("name", item.name)
                put("count", item.count)
            }
        }
    }
    putJsonArray("baits") {
        baits.forEach { item ->
            addJsonObject { put("bait_code", item.baitCode) }
        }
    }
    putJsonArray("photos") {
        photos.forEach { item ->
            addJsonObject {
                put("id", item.id.toString())
                put("storage_path", item.storagePath)
                put("sort_order", item.sortOrder ?: 0)
            }
        }
    }
}

internal fun Fish.toFishDto(fishingId: UUID): FishDto {
    return FishDto(
        id = id,
        fishingId = fishingId,
        name = name,
        count = count
    )
}
