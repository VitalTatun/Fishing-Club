package com.example.fishing.data

import com.example.fishing.data.supabase.*
import com.example.fishing.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    private val authRepository: AuthRepository
) : FishingRepository {

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    )

    override fun getAllReports(userId: UUID?): Flow<List<FishingReport>> = flow {
        if (!authRepository.isLoggedIn()) {
            emit(emptyList())
            return@flow
        }
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

        val reports = fishings.map { dto ->
            dto.toDomain(
                fish = fishByFishing[dto.id] ?: emptyList(),
                baits = baitsByFishing[dto.id] ?: emptyList(),
                photos = photosByFishing[dto.id] ?: emptyList()
            )
        }
        emit(reports)
    }

    override suspend fun saveReport(report: FishingReport) {
        try {
            val now = formatDate(Date())
            supabase.postgrest["fishing"].insert(report.toFishingDto(now))
            supabase.postgrest["fishing_fish"].insert(report.fish.map { it.toFishDto(report.id) })
            supabase.postgrest["fishing_baits"].insert(report.bait.map { BaitDto(report.id, it.name) })
            
            // Загружаем фото в Storage и сохраняем storage paths
            val storagePaths = report.photo.mapIndexed { index, localPath ->
                uploadPhotoToStorage(localPath, report.userId, report.id)
            }
            
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

    private suspend fun uploadPhotoToStorage(localPath: String, userId: UUID, fishingId: UUID): String {
        return try {
            val file = File(localPath)
            if (!file.exists()) {
                return localPath // Fallback to local path if file doesn't exist
            }
            
            val bytes = file.readBytes()
            val photoId = UUID.randomUUID()
            val storagePath = "$userId/$fishingId/$photoId.jpg"
            
            supabase.storage.from("fishing_photos").upload(storagePath, bytes) {
                upsert = true
            }
            
            storagePath
        } catch (e: Exception) {
            e.printStackTrace()
            localPath // Fallback to local path on error
        }
    }

    override suspend fun getPhotoSignedUrl(storagePath: String): String? {
        return try {
            // Если это уже URL или локальный путь - возвращаем как есть
            if (storagePath.startsWith("http") || storagePath.startsWith("/")) {
                return storagePath
            }
            
            supabase.storage.from("fishing_photos").createSignedUrl(
                path = storagePath,
                expiresIn = 24.hours
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun isStoragePath(path: String): Boolean {
        return !path.startsWith("http") && !path.startsWith("/")
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
