package com.example.fishing.data.report

import com.example.fishing.data.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

/**
 * Turns paths stored in the `fishing_photos` bucket into URLs the UI can load.
 * Paths that are already absolute are returned untouched, which lets callers
 * handle freshly picked local files and already-resolved URLs uniformly.
 */
interface PhotoUrlProvider {
    /** `true` when [path] is an object path in the private bucket. */
    fun isStoragePath(path: String): Boolean

    suspend fun getSignedUrl(storagePath: String): String?

    suspend fun getSignedUrls(storagePaths: List<String>): Map<String, String>
}

@Singleton
class SupabasePhotoUrlProvider @Inject constructor(
    private val supabase: SupabaseClient
) : PhotoUrlProvider {

    override fun isStoragePath(path: String): Boolean =
        !path.startsWith("http") && !path.startsWith("/")

    override suspend fun getSignedUrl(storagePath: String): String? {
        return try {
            if (!isStoragePath(storagePath)) {
                return storagePath
            }
            val signedUrl = supabase.storage.from(BUCKET).createSignedUrl(
                path = storagePath,
                expiresIn = SIGNED_URL_TTL
            )
            // createSignedUrl may return a relative path — prepend project URL.
            if (signedUrl.startsWith("http")) signedUrl else absoluteUrl(signedUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getSignedUrls(storagePaths: List<String>): Map<String, String> {
        val paths = storagePaths.filter { isStoragePath(it) }.distinct()
        if (paths.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, String>()
        paths.chunked(SIGNED_URL_BATCH_SIZE).forEach { chunk ->
            val signedUrls = supabase.storage.from(BUCKET)
                .createSignedUrls(SIGNED_URL_TTL, chunk)
            signedUrls.forEach { signed ->
                if (signed.error == null) {
                    result[signed.path] = if (signed.signedURL.startsWith("http")) {
                        signed.signedURL
                    } else {
                        absoluteUrl(signed.signedURL)
                    }
                }
            }
        }
        return result
    }

    private fun absoluteUrl(path: String): String = SupabaseConfig.URL.trimEnd('/') + path

    private companion object {
        const val BUCKET = "fishing_photos"

        // Signed URLs are short-lived; the UI re-resolves them on refresh.
        val SIGNED_URL_TTL = 24.hours

        // Supabase Storage accepts a limited number of paths per signed-url call.
        const val SIGNED_URL_BATCH_SIZE = 50
    }
}
