package com.example.fishing.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NominatimSearchResult {
    data class Success(val results: List<NominatimResponse>) : NominatimSearchResult
    data object Failure : NominatimSearchResult
}

@Singleton
class NominatimService @Inject constructor(
    private val client: HttpClient
) : Closeable {

    suspend fun search(query: String): NominatimSearchResult {
        return try {
            val response = client.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", query)
                parameter("format", "json")
                parameter("polygon_geojson", 1)
                parameter("limit", 10)
                parameter("accept-language", "ru")
            }
            if (response.status.isSuccess()) {
                NominatimSearchResult.Success(response.body())
            } else {
                NominatimSearchResult.Failure
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            NominatimSearchResult.Failure
        }
    }

    override fun close() {
        client.close()
    }
}
