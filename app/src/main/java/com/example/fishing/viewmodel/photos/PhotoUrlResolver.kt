package com.example.fishing.viewmodel.photos

import com.example.fishing.data.FishingRepository
import com.example.fishing.model.FishingPhoto
import com.example.fishing.model.FishingReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Resolves Storage paths into signed URLs and keeps a short-lived cache so
 * repeated Room emissions do not make the UI flash raw storage paths.
 *
 * The cache is shared by all report scenarios; callers own their own
 * "latest request" versions to avoid publishing out-of-order results.
 */
class PhotoUrlResolver(
    private val repository: FishingRepository,
    private val now: () -> Long = System::currentTimeMillis
) {
    private companion object {
        const val SIGNED_URL_TTL_MILLIS = 24L * 60 * 60 * 1000
        const val SIGNED_URL_REFRESH_BEFORE_MILLIS = 5L * 60 * 1000
    }

    private data class CachedSignedUrl(val url: String, val expiresAtMillis: Long)

    private val signedPhotoUrlCache = mutableMapOf<String, CachedSignedUrl>()
    private val signedPhotoUrlMutex = Mutex()

    /**
     * Reuses already known signed URLs so repeated Room emissions (likes, cache
     * updates) do not flash raw storage paths before the async resolve finishes.
     */
    fun applyCachedPhotoUrls(reports: List<FishingReport>): List<FishingReport> {
        val currentTime = now()
        val snapshot = synchronized(signedPhotoUrlCache) { signedPhotoUrlCache.toMap() }
        if (snapshot.isEmpty()) return reports
        return reports.map { report ->
            report.copy(
                photos = report.photos.map { photo ->
                    val cached = snapshot[photo.url]
                    if (cached != null &&
                        currentTime < cached.expiresAtMillis - SIGNED_URL_REFRESH_BEFORE_MILLIS
                    ) {
                        photo.copy(url = cached.url)
                    } else {
                        photo
                    }
                }
            )
        }
    }

    suspend fun resolvePhotoUrls(photos: List<FishingPhoto>): List<FishingPhoto> {
        val storagePaths = photos.map { it.url }
            .filter { repository.isStoragePath(it) }
            .distinct()
        if (storagePaths.isEmpty()) return photos

        val urls = resolveSignedUrls(storagePaths)
        if (urls.isEmpty()) return photos
        return photos.map { photo ->
            urls[photo.url]?.let { photo.copy(url = it) } ?: photo
        }
    }

    suspend fun resolveReportsPhotoUrls(reports: List<FishingReport>): List<FishingReport> {
        val storagePaths = reports.flatMap { it.photos }
            .map { it.url }
            .filter { repository.isStoragePath(it) }
            .distinct()
        if (storagePaths.isEmpty()) return reports

        val urls = resolveSignedUrls(storagePaths)
        if (urls.isEmpty()) return reports
        return reports.map { report ->
            report.copy(
                photos = report.photos.map { photo ->
                    urls[photo.url]?.let { photo.copy(url = it) } ?: photo
                }
            )
        }
    }

    /**
     * Resolves signed URLs for [storagePaths] using the cache where possible and
     * one batched repository call for the rest. A failed lookup is not cached, so
     * the next render tries again.
     */
    private suspend fun resolveSignedUrls(storagePaths: List<String>): Map<String, String> {
        val currentTime = now()
        val resolved = mutableMapOf<String, String>()
        val missing = mutableListOf<String>()

        synchronized(signedPhotoUrlCache) {
            signedPhotoUrlCache.entries.removeAll { it.value.expiresAtMillis <= currentTime }
            storagePaths.forEach { path ->
                val cached = signedPhotoUrlCache[path]
                if (cached != null &&
                    currentTime < cached.expiresAtMillis - SIGNED_URL_REFRESH_BEFORE_MILLIS
                ) {
                    resolved[path] = cached.url
                } else {
                    missing += path
                }
            }
        }
        if (missing.isEmpty()) return resolved

        return signedPhotoUrlMutex.withLock {
            val toFetch = mutableListOf<String>()
            val lockedNow = now()
            synchronized(signedPhotoUrlCache) {
                missing.forEach { path ->
                    val cached = signedPhotoUrlCache[path]
                    if (cached != null &&
                        lockedNow < cached.expiresAtMillis - SIGNED_URL_REFRESH_BEFORE_MILLIS
                    ) {
                        resolved[path] = cached.url
                    } else {
                        toFetch += path
                    }
                }
            }

            if (toFetch.isNotEmpty()) {
                val fetched = try {
                    repository.getPhotoSignedUrls(toFetch)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyMap()
                }
                if (fetched.isNotEmpty()) {
                    val expiresAt = lockedNow + SIGNED_URL_TTL_MILLIS
                    synchronized(signedPhotoUrlCache) {
                        fetched.forEach { (path, url) ->
                            signedPhotoUrlCache[path] = CachedSignedUrl(url, expiresAt)
                            resolved[path] = url
                        }
                    }
                }
            }
            resolved
        }
    }

    fun clear() {
        synchronized(signedPhotoUrlCache) { signedPhotoUrlCache.clear() }
    }
}
