package com.example.fishing.data

import android.content.Context
import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import com.example.fishing.utils.PhotoUtils
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.dao.FavoriteReportDao
import com.example.fishing.data.local.dao.LikeDao
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.cleanup.MarkerCacheCleaner
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import com.example.fishing.data.supabase.ProfileDto
import com.example.fishing.data.upload.ReportUploadQueue
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val supabase: SupabaseClient,
    private val reportDetailsDao: ReportDetailsDao,
    private val favoriteReportDao: FavoriteReportDao,
    private val likeDao: LikeDao,
    private val markerDao: MarkerDao,
    private val reportUploadQueue: ReportUploadQueue,
    private val markerCacheCleaner: MarkerCacheCleaner,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _sessionUser = MutableStateFlow<User?>(null)

    override val authState: StateFlow<AuthState> = supabase.auth.sessionStatus
        .combine(_sessionUser) { status, cachedUser ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = currentUser() ?: cachedUser
                    if (user != null) {
                        // Fire-and-forget: the profile upsert is best-effort and
                        // must not block emitting Authenticated (which gates the
                        // whole UI behind the loading spinner).
                        scope.launch { syncProfile(user) }
                        AuthState.Authenticated(user)
                    } else {
                        AuthState.Unauthenticated
                    }
                }
                SessionStatus.Initializing -> AuthState.Loading
                is SessionStatus.RefreshFailure -> {
                    val user = currentUser() ?: cachedUser
                    if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated
                }
                is SessionStatus.NotAuthenticated -> {
                    AuthState.Unauthenticated
                }
            }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    init {
        // AUD-02: marker rows are not user-scoped yet, so they may not outlive
        // the session that produced them. Keeping authState collected also
        // makes sure the drop happens even when no screen is observing it.
        markerCacheCleaner.clearWhenUnauthenticated(authState)
    }

    override suspend fun loadSession() {
        supabase.auth.awaitInitialization()
        val user = currentUser()
        _sessionUser.value = user
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = currentUser() ?: throw Exception("User not found")
            syncProfile(user)
            _sessionUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String, name: String): Result<User> {
        return try {
            val response = supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", name)
                }
            }
            val user = response?.let { currentUser() }
            if (user != null) {
                syncProfile(user)
                _sessionUser.value = user
                Result.success(user)
            } else {
                Result.success(
                    User(
                        id = UUID.randomUUID(),
                        name = name,
                        email = email,
                        image = ""
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        val userId = currentUser()?.id
        supabase.auth.signOut()
        _sessionUser.value = null
        if (userId != null) {
            clearUserScopedData()
        }
    }

    override suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            supabase.auth.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Password change failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            // SECURITY DEFINER function in supabase/migrations; removing the
            // auth.users row cascades to profiles and reports.
            supabase.postgrest.rpc("delete_account")
            runCatching { supabase.auth.signOut() }
            _sessionUser.value = null
            clearUserScopedData()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Account deletion failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun clearUserScopedData() {
        try {
            reportDetailsDao.deleteAll()
            favoriteReportDao.deleteAll()
            likeDao.deleteAll()
            markerDao.deleteAll()
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Failed to clear user cache: ${e.message}")
        }
        // Unsaved report photos belong to the session that just ended. The queue
        // is cleared first so its rows cannot keep files alive across sessions.
        withContext(Dispatchers.IO) {
            runCatching { reportUploadQueue.clearAll() }
            runCatching { PhotoUtils.clearSavedPhotos(appContext.filesDir) }
        }
    }

    override fun currentUser(): User? {
        return supabase.auth.currentUserOrNull()?.toDomainUser()
    }

    override suspend fun updateProfile(name: String, imageUri: String?): Result<User> {
        return try {
            val currentUser = currentUser() ?: throw Exception("Not logged in")
            var avatarUrl = currentUser.image

            if (imageUri != null && !imageUri.startsWith("http")) {
                val file = File(imageUri)
                if (file.exists()) {
                    try {
                        val ext = file.extension.ifEmpty { "jpg" }
                        val storagePath = "avatars/${currentUser.id}/${UUID.randomUUID()}.$ext"

                        // Reading the whole JPEG on Main is main-thread I/O
                        // (AUD-01): load the bytes on the IO dispatcher.
                        val bytes = withContext(ioDispatcher) { file.readBytes() }
                        supabase.storage.from("avatars").upload(storagePath, bytes) {
                            upsert = true
                        }
                        avatarUrl = storagePath
                    } catch (e: Exception) {
                        Log.e("SupabaseAuth", "Storage error: ${e.message}", e)
                        if (e.message?.contains("bucket", ignoreCase = true) == true &&
                            e.message?.contains("not found", ignoreCase = true) == true
                        ) {
                            throw Exception("Бакет 'avatars' не найден в Supabase Storage. Пожалуйста, создайте его в консоли Supabase.")
                        }
                        throw e
                    }
                }
            }

            supabase.auth.updateUser {
                data = buildJsonObject {
                    put("full_name", name)
                    put("avatar_url", avatarUrl)
                }
            }

            supabase.auth.refreshCurrentSession()
            val updatedUser = currentUser() ?: throw Exception("Failed to load updated user")
            syncProfile(updatedUser)
            _sessionUser.value = updatedUser
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Profile update failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun resolveImageUrl(path: String): String {
        if (path.isBlank() || path == "null") return ""
        if (path.startsWith("http") || path.startsWith("content://") || path.startsWith("/")) {
            return path
        }

        val storagePath = path.substringBefore('?')
        // Only the avatars bucket is public. Report photos live in the private
        // "fishing_photos" bucket and must be read through signed URLs.
        if (!storagePath.startsWith("avatars/")) return path

        return "${SupabaseConfig.URL.trimEnd('/')}/storage/v1/object/public/avatars/$storagePath"
    }

    private suspend fun syncProfile(user: User) {
        try {
            supabase.postgrest["profiles"].upsert(
                ProfileDto(
                    id = user.id,
                    name = user.name,
                    email = user.email,
                    avatarUrl = user.image
                )
            )
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Profile sync failed: ${e.message}", e)
        }
    }

    private fun UserInfo.toDomainUser(): User {
        val avatarPath = userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull ?: ""
        val fullName = userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull

        return User(
            id = UUID.fromString(id),
            name = fullName ?: email?.split("@")?.firstOrNull() ?: "User",
            email = email ?: "",
            image = if (avatarPath.isNotBlank()) resolveImageUrl(avatarPath) else ""
        )
    }
}
