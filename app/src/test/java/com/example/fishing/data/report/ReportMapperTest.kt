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
import com.example.fishing.model.User
import com.example.fishing.model.Water
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportMapperTest {

    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val reportId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    // --- dates -----------------------------------------------------------------

    @Test
    fun `parseDate accepts every timestamp shape the backend has produced`() {
        val expected = SupabaseDateFormats.parseDate("2026-11-07T19:03:49+03:00")
        assertEquals(
            expected,
            SupabaseDateFormats.parseDate("2026-11-07T19:03:49.000+03:00")
        )
        assertEquals(expected, SupabaseDateFormats.parseDate("2026-11-07T16:03:49Z"))
        assertEquals(expected, SupabaseDateFormats.parseDate("2026-11-07T16:03:49.000Z"))
    }

    @Test
    fun `parseDate returns null for an unknown format`() {
        assertNull(SupabaseDateFormats.parseDate("07.11.2026"))
    }

    @Test
    fun `formatDate round-trips through parseDate`() {
        val date = Date(1_700_000_000_000L)

        val formatted = SupabaseDateFormats.formatDate(date)

        assertEquals(date, SupabaseDateFormats.parseDate(formatted))
    }

    @Test
    fun `parseInstant falls back to the date parser and rejects blanks`() {
        assertNull(SupabaseDateFormats.parseInstant(null))
        assertNull(SupabaseDateFormats.parseInstant("  "))
        assertNull(SupabaseDateFormats.parseInstant("not a date"))

        assertEquals(
            Instant.parse("2026-11-07T16:03:49Z"),
            SupabaseDateFormats.parseInstant("2026-11-07T16:03:49Z")
        )
        assertEquals(
            Instant.parse("2026-11-07T16:03:49Z"),
            SupabaseDateFormats.parseInstant("2026-11-07T19:03:49+03:00")
        )
    }

    @Test
    fun `formatInstant emits canonical ISO form`() {
        assertEquals(
            "2026-11-07T16:03:49Z",
            SupabaseDateFormats.formatInstant(Instant.parse("2026-11-07T16:03:49Z"))
        )
        assertNull(SupabaseDateFormats.formatInstant(null))
    }

    // --- enums -----------------------------------------------------------------

    @Test
    fun `parseEnumValue is case-insensitive and tolerates missing underscores`() {
        assertEquals(
            FishingMethod.SPINNING,
            parseEnumValue("SPINNING", FishingMethod.entries.toTypedArray())
        )
        assertEquals(
            FishingMethod.SPINNING,
            parseEnumValue("spinning", FishingMethod.entries.toTypedArray())
        )
        assertEquals(
            FishingType.FISHING_LOG,
            parseEnumValue("fishinglog", FishingType.entries.toTypedArray())
        )
    }

    @Test
    fun `parseEnumValue returns null for blanks and unknown values`() {
        val values = FishingType.entries.toTypedArray()
        assertNull(parseEnumValue(null, values))
        assertNull(parseEnumValue("", values))
        assertNull(parseEnumValue("UNKNOWN", values))
    }

    // --- entity to domain ------------------------------------------------------

    @Test
    fun `entity to domain decodes children and derives stable photo ids`() {
        val entity = detailsEntity(
            fishJson = """[{"id":"00000000-0000-0000-0000-00000000000a","fishing_id":"$reportId","name":"Щука","count":2}]""",
            baitsJson = """[{"fishing_id":"$reportId","bait_code":"WOBBLER"}]""",
            imageUrls = listOf("$userId/$reportId/photo-1.jpg"),
            type = "FISHING_LOG",
            fishingMethod = "SPINNING",
            authorName = "Test User",
            authorAvatar = "avatar.jpg"
        )

        val report = entity.toDomain()

        assertEquals(FishingType.FISHING_LOG, report.type)
        assertEquals(FishingMethod.SPINNING, report.fishingMethod)
        assertEquals(listOf(Fish(UUID.fromString("00000000-0000-0000-0000-00000000000a"), "Щука", 2)), report.fish)
        assertEquals(listOf(Bait.WOBBLER), report.bait)
        assertEquals("Test User", report.user.name)
        assertEquals("avatar.jpg", report.user.image)
        assertEquals(
            UUID.nameUUIDFromBytes(entity.imageUrls.single().toByteArray()),
            report.photos.single().id
        )
    }

    @Test
    fun `entity to domain falls back to defaults for unknown enum values`() {
        val report = detailsEntity(
            type = "???",
            fishingMethod = null,
            baitsJson = """[{"fishing_id":"$reportId","bait_code":"???"}]"""
        ).toDomain()

        assertEquals(FishingType.FISHING_LOG, report.type)
        assertEquals(FishingMethod.NONE, report.fishingMethod)
        assertEquals(Bait.NONE, report.bait.single())
        assertEquals("", report.comment)
    }

    // --- domain to dto ---------------------------------------------------------

    @Test
    fun `report to dto formats instants and keeps draft publish time empty`() {
        val report = report(isPublic = false)

        val dto = report.toFishingDto("2026-11-07T16:03:49Z")

        assertEquals(report.id, dto.id)
        assertEquals("FISHING_LOG", dto.type)
        assertEquals("2026-11-07T16:03:49Z", dto.fishingStartAt)
        assertEquals("2026-11-07T16:03:49Z", dto.updatedAt)
        assertEquals("2026-11-07T16:03:49Z", dto.createdAt)
        assertNull(dto.publishedAt)
        assertFalse(dto.isPublic)
    }

    // --- publish payload -------------------------------------------------------

    @Test
    fun `publish payload omits optional values and includes children`() {
        val dto = report(isPublic = true).toFishingDto("2026-11-07T16:03:49Z")
            .copy(publishedAt = "2026-11-07T16:03:49Z", waterName = null)
        val fish = listOf(FishDto(UUID.randomUUID(), reportId, "Щука", 1))
        val baits = listOf(BaitDto(reportId, "WOBBLER"))
        val photos = listOf(PhotoDto(UUID.randomUUID(), reportId, "$userId/$reportId/p.jpg", 0))

        val payload = buildPublishPayload(dto, fish, baits, photos)

        assertNull(payload["water_name"])
        assertNull(payload["fishing_end_at"])
        assertEquals(reportId.toString(), payload["id"]?.jsonPrimitive?.content)
        assertTrue(payload["is_public"]!!.jsonPrimitive.boolean)
        assertEquals("2026-11-07T16:03:49Z", payload["published_at"]?.jsonPrimitive?.content)
        assertEquals(1, payload["fish"]!!.jsonArray.size)
        assertEquals(1, payload["baits"]!!.jsonArray.size)
        assertEquals(1, payload["photos"]!!.jsonArray.size)
    }

    // --- markers ---------------------------------------------------------------

    @Test
    fun `dto to marker entity fills defaults and keeps fish names`() {
        val dto = report(isPublic = true).toFishingDto("2026-11-07T16:03:49Z")

        val marker = dto.toMarkerEntity(fishNames = listOf("Щука"))

        assertEquals(reportId, marker.id)
        // The cache row is tagged with the report author, not the viewer.
        assertEquals(userId, marker.userId)
        assertEquals("Озеро Нарочь", marker.waterName)
        assertEquals(listOf("Щука"), marker.fishNames)
        assertTrue(marker.isPublic)
    }

    @Test
    fun `marker entity to domain maps enums and instants`() {
        val marker = MarkerEntity(
            id = reportId,
            userId = userId,
            name = "Спиннинг",
            waterName = "Озеро",
            waterLat = 54.0,
            waterLng = 26.0,
            type = "FISHING_LOG",
            fishingMethod = "SPINNING",
            fishingStartAt = "2026-11-07T16:03:49Z",
            isPublic = true,
            isPaidWater = false,
            fishNames = listOf("Щука")
        )

        val domain = marker.toDomain()

        assertEquals(FishingType.FISHING_LOG, domain.type)
        assertEquals(FishingMethod.SPINNING, domain.fishingMethod)
        assertEquals(Instant.parse("2026-11-07T16:03:49Z"), domain.fishingStartAt)
    }

    private fun report(isPublic: Boolean): FishingReport = FishingReport(
        id = reportId,
        userId = userId,
        type = FishingType.FISHING_LOG,
        name = "Спиннинг • Щука",
        water = Water(waterName = "Озеро Нарочь", latitude = 54.85, longitude = 26.77),
        photos = emptyList(),
        fishingStartAt = Instant.parse("2026-11-07T16:03:49Z"),
        weight = 2.0,
        fish = listOf(Fish(UUID.randomUUID(), "Щука", 1)),
        fishingMethod = FishingMethod.SPINNING,
        bait = listOf(Bait.WOBBLER),
        comment = "",
        user = User(userId, "Test User", "test@example.com", ""),
        fishingFromTheShore = true,
        isPublic = isPublic
    )

    private fun detailsEntity(
        fishJson: String = "[]",
        baitsJson: String = """[{"fishing_id":"$reportId","bait_code":"WOBBLER"}]""",
        imageUrls: List<String> = emptyList(),
        type: String = "FISHING_LOG",
        fishingMethod: String? = "NONE",
        authorName: String? = null,
        authorAvatar: String? = null
    ): ReportDetailsEntity = ReportDetailsEntity(
        id = reportId,
        userId = userId,
        publishedAt = null,
        type = type,
        name = "Спиннинг",
        waterName = "Озеро",
        waterLat = 54.0,
        waterLng = 26.0,
        waterPaid = false,
        spotLat = null,
        spotLng = null,
        fishingStartAt = "2026-11-07T16:03:49Z",
        fishingEndAt = null,
        weight = 1.0,
        fishingMethod = fishingMethod,
        comment = null,
        shore = true,
        isPublic = true,
        imageUrls = imageUrls,
        fishJson = fishJson,
        baitsJson = baitsJson,
        authorName = authorName,
        authorAvatar = authorAvatar,
        createdAt = null,
        likesCount = 0
    )
}
