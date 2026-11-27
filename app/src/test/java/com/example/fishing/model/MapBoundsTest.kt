package com.example.fishing.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapBoundsTest {

    private val bounds = MapBounds(south = 53.0, north = 54.0, west = 27.0, east = 28.0)

    @Test
    fun `expand adds a margin on every side`() {
        val expanded = bounds.expandedBy(0.25)

        assertTrue(expanded.south < bounds.south)
        assertTrue(expanded.north > bounds.north)
        assertTrue(expanded.west < bounds.west)
        assertTrue(expanded.east > bounds.east)
        assertTrue(expanded.contains(bounds))
    }

    @Test
    fun `contains accepts a fully nested viewport`() {
        val nested = MapBounds(south = 53.2, north = 53.8, west = 27.2, east = 27.8)

        assertTrue(bounds.contains(nested))
        assertTrue(bounds.contains(bounds))
    }

    @Test
    fun `contains rejects a viewport crossing the edge`() {
        assertFalse(bounds.contains(bounds.copy(north = 54.1)))
        assertFalse(bounds.contains(bounds.copy(west = 26.9)))
        assertFalse(bounds.contains(bounds.copy(east = 28.1)))
        assertFalse(bounds.contains(bounds.copy(south = 52.9)))
    }

    @Test
    fun `panning inside the expanded box stays inside it`() {
        val loaded = bounds.expandedBy(0.25)
        val moved = MapBounds(south = 53.1, north = 54.1, west = 27.1, east = 28.1)

        assertTrue(loaded.contains(moved))
    }
}
