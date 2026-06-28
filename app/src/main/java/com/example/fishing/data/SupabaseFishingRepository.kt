package com.example.fishing.data

import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.entity.MarkerEntity
import com.example.fishing.data.local.entity.ReportDetailsEntity
import com.example.fishing.data.supabase.*
import com.example.fishing.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import kotlin.time.Duration.Companion.hours
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseFishingRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val markerDao: MarkerDao,
    private val reportDetailsDao: ReportDetailsDao
) : FishingRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    )

    override fun getAllReports(userId: UUID?): Flow<List<FishingReport>> {
        val flow = if (userId != null) {
            reportDetailsDao.getByUserId(userId)
        } else {
            reportDetailsDao.getAll()
        }
        return flow.map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshAllReports(userId: UUID?) {
        if (!authRepository.isLoggedIn()) return
        try {
            val fishings = supabase.postgrest["fishing"].select {
                userId?.let { filter { eq("user_id", it) } }
                order("fishing_time", Order.DESCENDING)
            }.decodeList<FishingDto>()

            val fish = supabase.postgrest["fishing_fish"].select().decodeList<FishDto>()
            val baits = supabase.postgrest["fishing_baits"].select().decodeList<BaitDto>()
            val photos = supabase.postgrest["fishing_photos"].select {
                order("sort_order", Order.ASCENDING)
            }.decodeList<PhotoDto>()

            val fishByFishing = fish.groupBy { it.fishingId }
            val baitsByFishing = baits.groupBy { it.fishingId }
            val photosByFishing = photos.groupBy { it.fishingId }

            val entities = fishings.map { dto ->
                dto.toReportDetailsEntity(
                    fish = fishByFishing[dto.id] ?: emptyList(),
                    baits = baitsByFishing[dto.id] ?: emptyList(),
                    photos = photosByFishing[dto.id] ?: emptyList()
                )
            }
            reportDetailsDao.deleteAll()
            reportDetailsDao.insertAll(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getMapMarkers(): Flow<List<MarkerDomain>> = markerDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getReportDetails(id: UUID): Flow<FishingReport?> = reportDetailsDao.getById(id).map { entity ->
        entity?.toDomain()
    }

    suspend fun refreshMapMarkers() {
        if (!authRepository.isLoggedIn()) return
        try {
            val fishings = supabase.postgrest["fishing"].select {
                order("fishing_time", Order.DESCENDING)
            }.decodeList<FishingDto>()

            val entities = fishings.map { it.toMarkerEntity() }
            markerDao.deleteAll()
            markerDao.insertAll(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refreshReportDetails(id: UUID) {
        if (!authRepository.isLoggedIn()) return
        try {
            val fishing = supabase.postgrest["fishing"].select {
                filter { eq("id", id) }
            }.decodeList<FishingDto>().firstOrNull() ?: return

            val fish = supabase.postgrest["fishing_fish"].select {
                filter { eq("fishing_id", id) }
            }.decodeList<FishDto>()

            val baits = supabase.postgrest["fishing_baits"].select {
                filter { eq("fishing_id", id) }
            }.decodeList<BaitDto>()

            val photos = supabase.postgrest["fishing_photos"].select {
                filter { eq("fishing_id", id) }
                order("sort_order", Order.ASCENDING)
            }.decodeList<PhotoDto>()

            val entity = fishing.toReportDetailsEntity(fish, baits, photos)
            reportDetailsDao.insert(entity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun saveReport(report: FishingReport) {
        try {
            val now = formatDate(Date())
            supabase.postgrest["fishing"].insert(report.toFishingDto(now))

            val storagePaths = report.photo.mapIndexed { index, localPath ->
                val ext = File(localPath).extension.ifEmpty { "jpg" }
                val storagePath = "${report.userId}/${report.id}/${UUID.randomUUID()}.$ext"
                try {
                    val file = File(localPath)
                    if (file.exists()) {
                        supabase.storage.from("fishing_photos").upload(storagePath, file.readBytes()) {
                            upsert = true
                        }
                        storagePath
                    } else {
                        localPath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    localPath
                }
            }

            supabase.postgrest["fishing_fish"].insert(report.fish.map { it.toFishDto(report.id) })
            supabase.postgrest["fishing_baits"].insert(report.bait.map { BaitDto(report.id, it.name) })
            supabase.postgrest["fishing_photos"].insert(
                storagePaths.mapIndexed { index, path ->
                    PhotoDto(
                        fishingId = report.id,
                        storagePath = path,
                        sortOrder = index
                    )
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteReport(id: UUID) {
        try {
            supabase.postgrest["fishing"].delete {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getPhotoSignedUrl(storagePath: String): String? {
        return try {
            if (storagePath.startsWith("http") || storagePath.startsWith("/")) {
                return storagePath
            }
            val signedUrl = supabase.storage.from("fishing_photos").createSignedUrl(
                path = storagePath,
                expiresIn = 24.hours
            )
            // createSignedUrl may return a relative path — prepend project URL
            if (signedUrl.startsWith("http")) signedUrl
            else SupabaseConfig.URL.trimEnd('/') + signedUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun isStoragePath(path: String): Boolean {
        return !path.startsWith("http") && !path.startsWith("/")
    }

    private fun parseDate(value: String): Date? {
        for (format in dateFormats) {
            try {
                return format.parse(value)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun formatDate(date: Date): String {
        return dateFormats[0].format(date)
    }

    private fun enumValueOf(name: String, values: Array<out Enum<*>>): Enum<*>? {
        return values.firstOrNull {
            it.name.equals(name, ignoreCase = true) ||
            it.name.replace("_", "").equals(name, ignoreCase = true)
        }
    }

    private fun FishingDto.toDomain(
        fish: List<FishDto>,
        baits: List<BaitDto>,
        photos: List<PhotoDto>
    ): FishingReport {
        return FishingReport(
            id = id,
            userId = userId,
            publishedAt = publishedAt?.let { parseDate(it) },
            type = (enumValueOf(type, FishingType.entries.toTypedArray()) as? FishingType) ?: FishingType.FISHING_LOG,
            name = name,
            water = Water(
                waterName = waterName ?: "",
                latitude = waterLat ?: 0.0,
                longitude = waterLng ?: 0.0,
                isPaid = waterPaid
            ),
            spotLat = spotLat,
            spotLng = spotLng,
            photo = photos.map { it.storagePath },
            fishingTime = parseDate(fishingTime) ?: Date(),
            weight = weight,
            fish = fish.map { Fish(id = it.id, name = it.name, count = it.count) },
            fishingMethod = (enumValueOf(fishingMethod ?: "", FishingMethod.entries.toTypedArray()) as? FishingMethod) ?: FishingMethod.NONE,
            bait = baits.map { (enumValueOf(it.baitCode, Bait.entries.toTypedArray()) as? Bait) ?: Bait.NONE },
            comment = comment ?: "",
            user = authRepository.currentUser() ?: User(id = userId, name = "", email = "", image = ""),
            fishingFromTheShore = shore,
            isPublic = isPublic
        )
    }

    private fun FishingDto.toMarkerEntity(): MarkerEntity {
        return MarkerEntity(
            id = id,
            name = name,
            waterName = waterName ?: "",
            waterLat = waterLat ?: 0.0,
            waterLng = waterLng ?: 0.0,
            type = type,
            fishingMethod = fishingMethod ?: "",
            fishingTime = fishingTime,
            isPublic = isPublic
        )
    }

    private fun FishingDto.toReportDetailsEntity(
        fish: List<FishDto>,
        baits: List<BaitDto>,
        photos: List<PhotoDto>
    ): ReportDetailsEntity {
        return ReportDetailsEntity(
            id = id,
            userId = userId,
            publishedAt = publishedAt,
            type = type,
            name = name,
            waterName = waterName,
            waterLat = waterLat,
            waterLng = waterLng,
            waterPaid = waterPaid,
            spotLat = spotLat,
            spotLng = spotLng,
            fishingTime = fishingTime,
            weight = weight,
            fishingMethod = fishingMethod,
            comment = comment,
            shore = shore,
            isPublic = isPublic,
            imageUrls = photos.map { it.storagePath },
            fishJson = json.encodeToString(fish),
            baitsJson = json.encodeToString(baits)
        )
    }

    private fun MarkerEntity.toDomain(): MarkerDomain {
        return MarkerDomain(
            id = id,
            name = name,
            waterName = waterName,
            waterLat = waterLat,
            waterLng = waterLng,
            type = (enumValueOf(type, FishingType.entries.toTypedArray()) as? FishingType) ?: FishingType.FISHING_LOG,
            fishingMethod = (enumValueOf(fishingMethod, FishingMethod.entries.toTypedArray()) as? FishingMethod) ?: FishingMethod.NONE,
            fishingTime = parseDate(fishingTime) ?: Date(),
            isPublic = isPublic
        )
    }

    private fun ReportDetailsEntity.toDomain(): FishingReport {
        val fish = json.decodeFromString<List<FishDto>>(fishJson)
        val baits = json.decodeFromString<List<BaitDto>>(baitsJson)

        return FishingReport(
            id = id,
            userId = userId,
            publishedAt = publishedAt?.let { parseDate(it) },
            type = (enumValueOf(type, FishingType.entries.toTypedArray()) as? FishingType) ?: FishingType.FISHING_LOG,
            name = name,
            water = Water(
                waterName = waterName ?: "",
                latitude = waterLat ?: 0.0,
                longitude = waterLng ?: 0.0,
                isPaid = waterPaid
            ),
            spotLat = spotLat,
            spotLng = spotLng,
            photo = imageUrls,
            fishingTime = parseDate(fishingTime) ?: Date(),
            weight = weight,
            fish = fish.map { Fish(id = it.id, name = it.name, count = it.count) },
            fishingMethod = (enumValueOf(fishingMethod ?: "", FishingMethod.entries.toTypedArray()) as? FishingMethod) ?: FishingMethod.NONE,
            bait = baits.map { (enumValueOf(it.baitCode, Bait.entries.toTypedArray()) as? Bait) ?: Bait.NONE },
            comment = comment ?: "",
            user = authRepository.currentUser() ?: User(id = userId, name = "", email = "", image = ""),
            fishingFromTheShore = shore,
            isPublic = isPublic
        )
    }

    private fun FishingReport.toFishingDto(now: String): FishingDto {
        return FishingDto(
            id = id,
            userId = userId,
            publishedAt = publishedAt?.let { formatDate(it) },
            type = type.name,
            name = name,
            waterName = water.waterName,
            waterLat = water.latitude,
            waterLng = water.longitude,
            waterPaid = water.isPaid,
            spotLat = spotLat,
            spotLng = spotLng,
            fishingTime = formatDate(fishingTime),
            weight = weight,
            fishingMethod = fishingMethod.name,
            comment = comment,
            shore = fishingFromTheShore,
            isPublic = isPublic,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun Fish.toFishDto(fishingId: UUID): FishDto {
        return FishDto(
            id = id,
            fishingId = fishingId,
            name = name,
            count = count
        )
    }
}