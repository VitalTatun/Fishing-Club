package com.example.fishing.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SupabaseReportRemoteDataSourceTest {

    private val reportId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    @Test
    fun `bare object returns the report id and removed paths`() {
        val result = parsePublishReportResult(
            """{"id":"$reportId","removed_photo_paths":["u/r/a.jpg","u/r/b.jpg"]}"""
        )

        assertEquals(reportId, result.reportId)
        assertEquals(listOf("u/r/a.jpg", "u/r/b.jpg"), result.removedPhotoPaths)
    }

    @Test
    fun `array wrapped row is accepted`() {
        val result = parsePublishReportResult(
            """[{"id":"$reportId","removed_photo_paths":["u/r/a.jpg"]}]"""
        )

        assertEquals(reportId, result.reportId)
        assertEquals(listOf("u/r/a.jpg"), result.removedPhotoPaths)
    }

    @Test
    fun `missing removed paths means nothing to delete`() {
        val result = parsePublishReportResult("""{"id":"$reportId"}""")

        assertEquals(reportId, result.reportId)
        assertTrue(result.removedPhotoPaths.isEmpty())
    }

    @Test
    fun `unexpected or broken body is not a publish failure`() {
        val broken = parsePublishReportResult("not json")
        val wrongShape = parsePublishReportResult("""{"result":"ok"}""")
        val wrongTypes = parsePublishReportResult(
            """{"id":{"nested":true},"removed_photo_paths":"u/r/a.jpg"}"""
        )

        assertNull(broken.reportId)
        assertTrue(broken.removedPhotoPaths.isEmpty())
        assertNull(wrongShape.reportId)
        assertTrue(wrongShape.removedPhotoPaths.isEmpty())
        assertNull(wrongTypes.reportId)
        assertTrue(wrongTypes.removedPhotoPaths.isEmpty())
    }

    @Test
    fun `non uuid id does not discard removed paths`() {
        val result = parsePublishReportResult(
            """{"id":"legacy","removed_photo_paths":["u/r/a.jpg"]}"""
        )

        assertNull(result.reportId)
        assertEquals(listOf("u/r/a.jpg"), result.removedPhotoPaths)
    }
}
