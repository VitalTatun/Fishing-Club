package com.example.fishing.data

import com.example.fishing.data.local.dao.FavoriteReportDao
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.entity.FavoriteReportEntity
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
import kotlinx.coroutines.CancellationException
import androidx.room.withTransaction
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    private val reportDetailsDao: ReportDetailsDao,
    private val favoriteReportDao: FavoriteReportDao,
    private val database: AppDatabase
) : FishingRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val datePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    )

    private fun getDateFormat(pattern: String): SimpleDateFormat {
        return SimpleDateFormat(pattern, Locale.US)
    }

    override fun getHomeReports(userId: UUID): Flow<List<FishingReport>> =
        reportDetailsDao.getHomeReports(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun refreshHomeReports(userId: UUID) {
        try {
            val fishings = supabase.postgrest["fishing"].select {
                filter { eq("user_id", userId) }
                order("fishing_start_at", Order.DESCENDING)
            }.decodeList<FishingDto>()

            val fish = supabase.postgrest["fishing_fish"].select().decodeList<FishDto>()
            val baits = supabase.postgrest["fishing_baits"].select().decodeList<BaitDto>()
            val photos = supabase.postgrest["fishing_photos"].select {
                order("sort_order", Order.ASCENDING)
            }.decodeList<PhotoDto>()
            val profiles = supabase.postgrest["profiles"].select().decodeList<ProfileDto>()

            val fishByFishing = fish.groupBy { it.fishingId }
            val baitsByFishing = baits.groupBy { it.fishingId }
            val photosByFishing = photos.groupBy { it.fishingId }
            val profilesById = profiles.associateBy { it.id }

            val entities = fishings.map { dto ->
                dto.toReportDetailsEntity(
                    fish = fishByFishing[dto.id] ?: emptyList(),
                    baits = baitsByFishing[dto.id] ?: emptyList(),
                    photos = photosByFishing[dto.id] ?: emptyList(),
                    author = profilesById[dto.userId]
                )
            }
            val favoriteLinks = supabase.postgrest["favorites"].select {
                filter { eq("user_id", userId) }
            }.decodeList<FavoriteDto>()
                .map { FavoriteReportEntity(userId = userId, reportId = it.fishingId) }

            val favoriteEntities = favoriteLinks
                .mapNotNull { fetchReportDetails(it.reportId) }

            database.withTransaction {
                // The home cache is one coherent snapshot: own reports plus bookmarked reports.
                reportDetailsDao.deleteAll()
                reportDetailsDao.insertAll((entities + favoriteEntities).distinctBy { it.id })
                favoriteReportDao.deleteAllForUser(userId)
                favoriteReportDao.insertAll(favoriteLinks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun getMapMarkers(): Flow<List<MarkerDomain>> = markerDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getFavoriteReports(userId: UUID): Flow<List<FishingReport>> =
        reportDetailsDao.getFavorites(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getReportDetails(id: UUID): Flow<FishingReport?> = reportDetailsDao.getById(id).map { entity ->
        entity?.toDomain()
    }

    override suspend fun refreshMapMarkers(): Result<List<MarkerDomain>> {
        val userId = authRepository.currentUser()?.id
            ?: return Result.failure(IllegalStateException("Нет активной сессии"))
        return try {
            val fishings = supabase.postgrest["fishing"].select(
                columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, fishing_fish(*)")
            ) {
                filter {
                    or {
                        eq("is_public", true)
                        eq("user_id", userId)
                    }
                }
                order("fishing_start_at", Order.DESCENDING)
            }.decodeList<FishingDto>()

            val entities = fishings.map { dto ->
                dto.toMarkerEntity(
                    fishNames = dto.fish.map { it.name }
                )
            }
            // Cache is replaced atomically so a mid-refresh failure cannot wipe
            // previously cached markers out from under the user.
            database.withTransaction {
                markerDao.deleteAll()
                markerDao.insertAll(entities)
            }
            Result.success(entities.map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshReportDetails(id: UUID): Boolean {
        if (authRepository.currentUser() == null) return false

        val fishing = supabase.postgrest["fishing"].select {
            filter { eq("id", id) }
        }.decodeList<FishingDto>().firstOrNull() ?: return false

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
        val author = supabase.postgrest["profiles"].select {
            filter { eq("id", fishing.userId) }
        }.decodeList<ProfileDto>().firstOrNull()

        val entity = fishing.toReportDetailsEntity(fish, baits, photos, author)
        reportDetailsDao.insert(entity)
        return true
    }

    override suspend fun saveReport(report: FishingReport): Result<Unit> {
        val now = formatDate(Date())
        val dto = report.toFishingDto(now)
        val uploadedPaths = mutableListOf<String>()

        return try {
            // 1. Upsert base report. This handles both new and existing reports.
            // If it exists, it will be updated with the latest data (comment, weight, etc.)
            supabase.postgrest["fishing"].upsert(dto)

            // 2. Upload only new photos.
            val storagePaths = report.photos.map { photo ->
                if (isStoragePath(photo.url)) {
                    photo.url
                } else {
                    val ext = File(photo.url).extension.ifEmpty { "jpg" }
                    // Deterministic storage path using stable photo.id
                    val storagePath = "${report.userId}/${report.id}/${photo.id}.$ext"
                    val file = File(photo.url)
                    if (!file.exists()) {
                        throw IllegalStateException("Photo file not found: ${photo.url}")
                    }
                    supabase.storage.from("fishing_photos").upload(storagePath, file.readBytes()) {
                        upsert = true
                    }
                    uploadedPaths.add(storagePath)
                    storagePath
                }
            }

            // 3. Prepare child DTOs.
            val fishDtos = report.fish.map { it.toFishDto(report.id) }
            val baitDtos = report.bait.map { BaitDto(report.id, it.name) }
            val photoDtos = report.photos.mapIndexed { index, photo ->
                val finalPath = storagePaths[index]
                PhotoDto(
                    id = photo.id,
                    fishingId = report.id,
                    storagePath = finalPath,
                    sortOrder = index
                )
            }

            // 4. Upsert children instead of delete+insert to maintain idempotency.
            // Unique constraints (like PK on fish.id) will prevent duplicates on retry.
            if (fishDtos.isNotEmpty()) {
                supabase.postgrest["fishing_fish"].upsert(fishDtos)
            }
            if (baitDtos.isNotEmpty()) {
                supabase.postgrest["fishing_baits"].upsert(baitDtos)
            }
            if (photoDtos.isNotEmpty()) {
                supabase.postgrest["fishing_photos"].upsert(photoDtos)
            }

            val markerEntity = dto.toMarkerEntity(report.fish.map { it.name })
            val detailsEntity = ReportDetailsEntity(
                id = report.id,
                userId = report.userId,
                publishedAt = dto.publishedAt,
                type = dto.type,
                name = dto.name,
                waterName = dto.waterName,
                waterLat = dto.waterLat,
                waterLng = dto.waterLng,
                waterPaid = dto.waterPaid,
                spotLat = dto.spotLat,
                spotLng = dto.spotLng,
                fishingStartAt = dto.fishingStartAt,
                fishingEndAt = dto.fishingEndAt,
                weight = dto.weight,
                fishingMethod = dto.fishingMethod,
                comment = dto.comment,
                shore = dto.shore,
                isPublic = dto.isPublic,
                imageUrls = storagePaths,
                fishJson = json.encodeToString(fishDtos),
                baitsJson = json.encodeToString(baitDtos),
                authorName = report.user.name,
                authorAvatar = report.user.image,
                createdAt = dto.createdAt
            )

            database.withTransaction {
                reportDetailsDao.insert(detailsEntity)
                if (dto.isPublic || dto.userId == authRepository.currentUser()?.id) {
                    markerDao.insertAll(listOf(markerEntity))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            // Cleanup: only delete photos uploaded in THIS specific call attempt.
            if (uploadedPaths.isNotEmpty()) {
                runCatching {
                    supabase.storage.from("fishing_photos").delete(uploadedPaths)
                }
            }
            Result.failure(e)
        }
    }


    override suspend fun deleteReport(id: UUID): Result<Unit> {
        return try {
            // 1. Fetch storage paths before deleting the report.
            // ON DELETE CASCADE would remove these records, making paths unrecoverable.
            val storagePaths = try {
                supabase.postgrest["fishing_photos"].select {
                    filter { eq("fishing_id", id) }
                }.decodeList<PhotoDto>().map { it.storagePath }
            } catch (e: Exception) {
                // If we can't even fetch paths, we can't proceed with safe cleanup.
                return Result.failure(e)
            }

            // 2. Delete Storage objects BEFORE deleting DB record.
            // If this fails (e.g. network), we don't delete the DB, allowing retry.
            if (storagePaths.isNotEmpty()) {
                try {
                    supabase.storage.from("fishing_photos").delete(storagePaths)
                } catch (e: Exception) {
                    // Suppress "404 Not Found" or similar errors that indicate files are already gone.
                    // If it's a real network/auth error, let it throw to stop the flow.
                    val errorBody = e.message ?: ""
                    val isNotFoundError = errorBody.contains("not found", ignoreCase = true) ||
                            errorBody.contains("404")

                    if (!isNotFoundError) {
                        return Result.failure(e)
                    }
                }
            }

            // 3. Delete the report from Supabase DB.
            // CASCADE removes fishing_fish, fishing_baits, fishing_photos, and favorites.
            supabase.postgrest["fishing"].delete {
                filter { eq("id", id) }
            }

            // 4. Clean local Room cache in a single transaction.
            database.withTransaction {
                reportDetailsDao.deleteById(id)
                markerDao.deleteById(id)
                favoriteReportDao.deleteByReportId(id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addFavorite(report: FishingReport) {
        val currentUser = authRepository.currentUser() ?: return
        try {
            supabase.postgrest["favorites"].insert(FavoriteDto(userId = currentUser.id, fishingId = report.id))
            favoriteReportDao.insertAll(listOf(FavoriteReportEntity(currentUser.id, report.id)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun removeFavorite(reportId: UUID) {
        val currentUser = authRepository.currentUser() ?: return
        try {
            supabase.postgrest["favorites"].delete {
                filter { eq("user_id", currentUser.id) }
                filter { eq("fishing_id", reportId) }
            }
            val cachedReport = reportDetailsDao.getByIdOneShot(reportId)
            if (cachedReport != null && cachedReport.userId != currentUser.id) {
                reportDetailsDao.deleteById(reportId)
            }
            favoriteReportDao.delete(currentUser.id, reportId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchReportDetails(fishingId: UUID): ReportDetailsEntity? {
        val fishing = supabase.postgrest["fishing"].select {
            filter { eq("id", fishingId) }
        }.decodeList<FishingDto>().firstOrNull() ?: return null

        val fish = supabase.postgrest["fishing_fish"].select {
            filter { eq("fishing_id", fishingId) }
        }.decodeList<FishDto>()

        val baits = supabase.postgrest["fishing_baits"].select {
            filter { eq("fishing_id", fishingId) }
        }.decodeList<BaitDto>()

        val photos = supabase.postgrest["fishing_photos"].select {
            filter { eq("fishing_id", fishingId) }
            order("sort_order", Order.ASCENDING)
        }.decodeList<PhotoDto>()

        val author = supabase.postgrest["profiles"].select {
            filter { eq("id", fishing.userId) }
        }.decodeList<ProfileDto>().firstOrNull()

        return fishing.toReportDetailsEntity(fish, baits, photos, author)
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
        for (pattern in datePatterns) {
            try {
                return getDateFormat(pattern).parse(value)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun formatDate(date: Date): String {
        return getDateFormat(datePatterns[0]).format(date)
    }

    private fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return try {
            Instant.parse(value)
        } catch (_: Exception) {
            try {
                val date = parseDate(value)
                date?.toInstant()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun formatInstant(instant: Instant?): String? {
        return instant?.let {
            DateTimeFormatter.ISO_INSTANT.format(it)
        }
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
            photos = photos.map { FishingPhoto(id = it.id, url = it.storagePath) },
            fishingStartAt = parseInstant(fishingStartAt),
            fishingEndAt = parseInstant(fishingEndAt),
            weight = weight,
            fish = fish.map { Fish(id = it.id, name = it.name, count = it.count) },
            fishingMethod = (enumValueOf(fishingMethod ?: "", FishingMethod.entries.toTypedArray()) as? FishingMethod) ?: FishingMethod.NONE,
            bait = baits.map { (enumValueOf(it.baitCode, Bait.entries.toTypedArray()) as? Bait) ?: Bait.NONE },
            comment = comment ?: "",
            user = authRepository.currentUser() ?: User(id = userId, name = "", email = "", image = ""),
            fishingFromTheShore = shore,
            isPublic = isPublic,
            createdAt = createdAt?.let { parseDate(it) }
        )
    }

    private fun FishingDto.toMarkerEntity(fishNames: List<String>): MarkerEntity {
        return MarkerEntity(
            id = id,
            name = name,
            waterName = waterName ?: "",
            waterLat = waterLat ?: 0.0,
            waterLng = waterLng ?: 0.0,
            type = type,
            fishingMethod = fishingMethod ?: "",
            fishingStartAt = fishingStartAt,
            isPublic = isPublic,
            isPaidWater = waterPaid,
            fishNames = fishNames
        )
    }

    private fun FishingDto.toReportDetailsEntity(
        fish: List<FishDto>,
        baits: List<BaitDto>,
        photos: List<PhotoDto>,
        author: ProfileDto?
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
            fishingStartAt = fishingStartAt,
            fishingEndAt = fishingEndAt,
            weight = weight,
            fishingMethod = fishingMethod,
            comment = comment,
            shore = shore,
            isPublic = isPublic,
            imageUrls = photos.map { it.storagePath },
            fishJson = json.encodeToString(fish),
            baitsJson = json.encodeToString(baits),
            authorName = author?.name,
            authorAvatar = author?.avatarUrl?.let(authRepository::resolveImageUrl),
            createdAt = createdAt
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
            fishingStartAt = parseInstant(fishingStartAt),
            isPublic = isPublic,
            isPaidWater = isPaidWater,
            fishNames = fishNames
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
            photos = imageUrls.map { url -> 
                // We don't have Photo IDs in details entity, but we can reconstruct them
                // or ideally we should store them in Room as well.
                // For now, use deterministic ID based on URL if needed, 
                // but domain FishingPhoto requires a UUID.
                // Actually, PhotoDto has an ID.
                FishingPhoto(id = UUID.nameUUIDFromBytes(url.toByteArray()), url = url)
            },
            fishingStartAt = parseInstant(fishingStartAt),
            fishingEndAt = parseInstant(fishingEndAt),
            weight = weight,
            fish = fish.map { Fish(id = it.id, name = it.name, count = it.count) },
            fishingMethod = (enumValueOf(fishingMethod ?: "", FishingMethod.entries.toTypedArray()) as? FishingMethod) ?: FishingMethod.NONE,
            bait = baits.map { (enumValueOf(it.baitCode, Bait.entries.toTypedArray()) as? Bait) ?: Bait.NONE },
            comment = comment ?: "",
            user = User(
                id = userId,
                name = authorName.orEmpty(),
                email = "",
                image = authorAvatar.orEmpty()
            ),
            fishingFromTheShore = shore,
            isPublic = isPublic,
            createdAt = createdAt?.let { parseDate(it) }
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
            fishingStartAt = formatInstant(fishingStartAt),
            fishingEndAt = formatInstant(fishingEndAt),
            weight = weight,
            fishingMethod = fishingMethod.name,
            comment = comment,
            shore = fishingFromTheShore,
            isPublic = isPublic,
            createdAt = createdAt?.let { formatDate(it) } ?: now,
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
