package com.example.fishing.data.cleanup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class OrphanPhotoSelectionTest {

    private val now = Instant.parse("2026-11-01T12:00:00Z")
    private val olderThan = now.minusSeconds(24 * 60 * 60)

    private fun objectAt(path: String, hoursOld: Long?) = StoragePhotoObject(
        path = path,
        createdAt = hoursOld?.let { now.minusSeconds(it * 60 * 60) }
    )

    @Test
    fun `old unreferenced objects are selected`() {
        val objects = listOf(
            objectAt("user/report/a.jpg", hoursOld = 30),
            objectAt("user/report/b.jpg", hoursOld = 4)
        )

        val orphans = findOrphans(objects, referencedPaths = emptySet(), olderThan = olderThan)

        assertEquals(listOf("user/report/a.jpg"), orphans)
    }

    @Test
    fun `objects still referenced by a report are kept`() {
        val objects = listOf(
            objectAt("user/report/a.jpg", hoursOld = 30),
            objectAt("user/report/b.jpg", hoursOld = 30)
        )

        val orphans = findOrphans(
            objects,
            referencedPaths = setOf("user/report/b.jpg"),
            olderThan = olderThan
        )

        assertEquals(listOf("user/report/a.jpg"), orphans)
    }

    @Test
    fun `objects of unknown age are never deleted`() {
        val objects = listOf(objectAt("user/report/a.jpg", hoursOld = null))

        assertTrue(findOrphans(objects, emptySet(), olderThan).isEmpty())
    }

    @Test
    fun `objects younger than the grace period are never deleted`() {
        val objects = listOf(objectAt("user/report/a.jpg", hoursOld = 23))

        assertTrue(findOrphans(objects, emptySet(), olderThan).isEmpty())
    }
}
