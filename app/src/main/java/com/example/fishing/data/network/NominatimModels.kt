package com.example.fishing.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@Serializable
data class NominatimResponse(
    @SerialName("place_id") val placeId: Long,
    @SerialName("licence") val licence: String,
    @SerialName("osm_type") val osmType: String,
    @SerialName("osm_id") val osmId: Long,
    @SerialName("lat") val lat: String,
    @SerialName("lon") val lon: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("class") val category: String,
    @SerialName("type") val type: String,
    @SerialName("importance") val importance: Double,
    @SerialName("geojson") val geojson: JsonElement? = null
)

fun NominatimResponse.getPolygonPoints(): List<List<Double>>? {
    val geojsonObj = geojson?.jsonObject ?: return null
    val type = geojsonObj["type"]?.toString()?.removeSurrounding("\"")
    val coordinates = geojsonObj["coordinates"]?.jsonArray ?: return null

    return when (type) {
        "Polygon" -> {
            // Polygon coordinates are List<List<List<Double>>> (outer ring + holes)
            coordinates.getOrNull(0)?.jsonArray?.map { point ->
                point.jsonArray.map { it.toString().toDouble() }
            }
        }
        "MultiPolygon" -> {
            // MultiPolygon coordinates are List<List<List<List<Double>>>>
            // We'll take the first ring of the first polygon for simplicity in this prototype
            coordinates.getOrNull(0)?.jsonArray?.getOrNull(0)?.jsonArray?.map { point ->
                point.jsonArray.map { it.toString().toDouble() }
            }
        }
        "LineString" -> {
            // LineString coordinates are List<List<Double>>
            coordinates.map { point ->
                point.jsonArray.map { it.toString().toDouble() }
            }
        }
        else -> null
    }
}
