package com.example.fishing.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseReportRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient
) : ReportRemoteDataSource {

    override suspend fun reportExists(reportId: UUID): Boolean =
        supabase.postgrest["fishing"]
            .select(Columns.list("id")) {
                filter { eq("id", reportId) }
                limit(1)
            }
            .decodeList<JsonObject>()
            .isNotEmpty()

    override suspend fun uploadPhoto(storagePath: String, bytes: ByteArray) {
        supabase.storage.from(BUCKET).upload(storagePath, bytes) {
            upsert = true
        }
    }

    override suspend fun publishReport(payload: JsonObject): PublishReportResult {
        val response = supabase.postgrest.rpc(
            function = "publish_report",
            parameters = buildJsonObject {
                put("payload", payload)
            }
        )
        return parsePublishReportResult(response.data)
    }

    override suspend fun deletePhotos(paths: List<String>) {
        if (paths.isNotEmpty()) {
            supabase.storage.from(BUCKET).delete(paths)
        }
    }

    private companion object {
        const val BUCKET = "fishing_photos"
    }
}

/**
 * The RPC returns `{ "id": ..., "removed_photo_paths": [...] }`.
 * Accept both a bare object and an array-wrapped row for safety; an unexpected
 * body only means "nothing to delete", never a failed publish.
 */
internal fun parsePublishReportResult(body: String): PublishReportResult {
    val element = runCatching { REPORT_JSON.parseToJsonElement(body) }.getOrNull()
        ?: return PublishReportResult(reportId = null, removedPhotoPaths = emptyList())
    val obj = (element as? JsonObject)
        ?: ((element as? JsonArray)?.firstOrNull() as? JsonObject)
        ?: return PublishReportResult(reportId = null, removedPhotoPaths = emptyList())
    val reportId = (obj["id"] as? JsonPrimitive)?.contentOrNull
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val removed = (obj["removed_photo_paths"] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        .orEmpty()
    return PublishReportResult(reportId = reportId, removedPhotoPaths = removed)
}

private val REPORT_JSON = Json { ignoreUnknownKeys = true }
