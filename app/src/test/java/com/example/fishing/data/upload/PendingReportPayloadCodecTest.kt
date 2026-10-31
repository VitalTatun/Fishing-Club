package com.example.fishing.data.upload

import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingPhoto
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.User
import com.example.fishing.model.Water
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Date
import java.util.UUID

class PendingReportPayloadCodecTest {

    private val codec = PendingReportPayloadCodec()

    private val user = User(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        image = "https://example.com/avatar.jpg",
        name = "Иван",
        email = "ivan@example.com"
    )

    private fun report(
        publishedAt: Date? = Date(1_700_000_000_000),
        createdAt: Date? = Date(1_700_000_100_000)
    ) = FishingReport(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        userId = user.id,
        publishedAt = publishedAt,
        type = FishingType.HAUL,
        name = "Спиннинг • Щука",
        water = Water(
            id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
            waterName = "Озеро Нарочь",
            latitude = 54.85,
            longitude = 26.77,
            isPaid = true
        ),
        spotLat = 54.851,
        spotLng = 26.771,
        photos = listOf(
            FishingPhoto(
                id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
                url = "/data/photos/one.jpg"
            ),
            FishingPhoto(
                id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
                url = "/data/photos/two.jpg"
            )
        ),
        fishingStartAt = Instant.ofEpochMilli(1_699_999_000_000),
        fishingEndAt = Instant.ofEpochMilli(1_699_999_900_000),
        weight = 3.5,
        fish = listOf(
            Fish(
                id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
                name = "Щука",
                count = 2
            )
        ),
        fishingMethod = FishingMethod.SPINNING,
        bait = listOf(Bait.WOBBLER, Bait.EDIBLE_RUBBER),
        comment = "Клёв был отличный",
        user = user,
        fishingFromTheShore = false,
        isPublic = true,
        createdAt = createdAt
    )

    @Test
    fun `round trip preserves every report field`() {
        val original = report()

        val decoded = codec.decode(codec.encode(original))

        assertEquals(original.id, decoded.id)
        assertEquals(original.userId, decoded.userId)
        assertEquals(original.publishedAt, decoded.publishedAt)
        assertEquals(original.type, decoded.type)
        assertEquals(original.name, decoded.name)
        assertEquals(original.water, decoded.water)
        assertEquals(original.spotLat, decoded.spotLat)
        assertEquals(original.spotLng, decoded.spotLng)
        assertEquals(original.photos, decoded.photos)
        assertEquals(original.fishingStartAt, decoded.fishingStartAt)
        assertEquals(original.fishingEndAt, decoded.fishingEndAt)
        assertEquals(original.weight, decoded.weight, 0.0)
        assertEquals(original.fish, decoded.fish)
        assertEquals(original.fishingMethod, decoded.fishingMethod)
        assertEquals(original.bait, decoded.bait)
        assertEquals(original.comment, decoded.comment)
        assertEquals(original.user, decoded.user)
        assertEquals(original.fishingFromTheShore, decoded.fishingFromTheShore)
        assertEquals(original.isPublic, decoded.isPublic)
        assertEquals(original.createdAt, decoded.createdAt)
    }

    @Test
    fun `round trip keeps draft without publish time`() {
        val original = report(publishedAt = null, createdAt = null).copy(isPublic = false)

        val decoded = codec.decode(codec.encode(original))

        assertNull(decoded.publishedAt)
        assertNull(decoded.createdAt)
        assertEquals(false, decoded.isPublic)
    }

    @Test
    fun `unknown enum names decode to safe defaults`() {
        val raw = codec.encode(report()).replace("HAUL", "REMOVED_TYPE")

        val decoded = codec.decode(raw)

        assertEquals(FishingType.FISHING_LOG, decoded.type)
    }

    @Test
    fun `photo paths round trip and malformed payload yields empty list`() {
        val paths = listOf("/data/photos/one.jpg", "/data/photos/two.jpg")
        assertEquals(paths, codec.decodePaths(codec.encodePaths(paths)))
        assertTrue(codec.decodePaths("not json").isEmpty())
    }
}
