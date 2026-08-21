package com.example.fishing.data.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NominatimService @Inject constructor() {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        defaultRequest {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        }
    }

    suspend fun search(query: String): List<NominatimResponse> {
        return try {
            val response = client.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", query)
                parameter("format", "json")
                parameter("polygon_geojson", 1)
                parameter("limit", 10)
                parameter("accept-language", "ru")
            }
            if (response.status.value == 200) {
                response.body()
            } else {
                android.util.Log.e("Nominatim", "Error response: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("Nominatim", "Search failed", e)
            emptyList()
        }
    }
}
