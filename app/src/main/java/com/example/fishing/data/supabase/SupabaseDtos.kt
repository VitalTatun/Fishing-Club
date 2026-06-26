package com.example.fishing.data.supabase

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

internal object UuidSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

@Serializable
data class FishingDto(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    @Serializable(with = UuidSerializer::class) @SerialName("user_id") val userId: UUID,
    @SerialName("published_at") val publishedAt: String? = null,
    val type: String,
    val name: String,
    @SerialName("water_name") val waterName: String? = null,
    @SerialName("water_lat") val waterLat: Double? = null,
    @SerialName("water_lng") val waterLng: Double? = null,
    @SerialName("water_paid") val waterPaid: Boolean = false,
    @SerialName("spot_lat") val spotLat: Double? = null,
    @SerialName("spot_lng") val spotLng: Double? = null,
    @SerialName("fishing_time") val fishingTime: String,
    val weight: Double = 0.0,
    @SerialName("fishing_method") val fishingMethod: String? = null,
    val comment: String? = null,
    val shore: Boolean = true,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class FishDto(
    @Serializable(with = UuidSerializer::class) val id: UUID = UUID.randomUUID(),
    @Serializable(with = UuidSerializer::class) @SerialName("fishing_id") val fishingId: UUID,
    val name: String,
    val count: Int = 0
)

@Serializable
data class BaitDto(
    @Serializable(with = UuidSerializer::class) @SerialName("fishing_id") val fishingId: UUID,
    @SerialName("bait_code") val baitCode: String
)

@Serializable
data class PhotoDto(
    @Serializable(with = UuidSerializer::class) val id: UUID = UUID.randomUUID(),
    @Serializable(with = UuidSerializer::class) @SerialName("fishing_id") val fishingId: UUID,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("sort_order") val sortOrder: Int? = 0,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ProfileDto(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
