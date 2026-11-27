package com.example.fishing.model

/**
 * Rectangular geographic viewport of the map.
 *
 * Deliberately simple: no antimeridian handling, no projection. Belarus and its
 * neighbours fit into a plain min/max box, which is what the server query uses.
 */
data class MapBounds(
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double
) {
    /**
     * Grows the box by [fraction] of its span on every side, so small camera
     * movements do not trigger a new request. A 25% margin is the map default.
     */
    fun expandedBy(fraction: Double): MapBounds {
        val latPadding = (north - south) * fraction
        val lngPadding = (east - west) * fraction
        return MapBounds(
            south = south - latPadding,
            north = north + latPadding,
            west = west - lngPadding,
            east = east + lngPadding
        )
    }

    /** True when [other] lies completely inside this box. */
    fun contains(other: MapBounds): Boolean =
        other.south >= south &&
            other.north <= north &&
            other.west >= west &&
            other.east <= east
}
