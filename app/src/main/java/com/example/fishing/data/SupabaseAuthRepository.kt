package com.example.fishing.data

import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.dao.FavoriteReportDao
import com.example.fishing.data.local.dao.MarkerDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import com.example.fishing.data.supabase.ProfileDto
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.*
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val reportDetailsDao: ReportDetailsDao,
    private val favoriteReportDao: FavoriteReportDao,
    private val markerDao: MarkerDao
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _sessionUser = MutableStateFlow<User?>(null)

    override val authState: StateFlow<AuthState> = supabase.auth.sessionStatus
        .combine(_sessionUser) { status, cachedUser ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = currentUser() ?: cachedUser
                    if (user != null) {
                        syncProfile(user)
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
            try {
                reportDetailsDao.deleteAll()
                favoriteReportDao.deleteAll()
                markerDao.deleteAll()
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Failed to clear user cache on logout: ${e.message}")
            }
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

                        supabase.storage.from("avatars").upload(storagePath, file.readBytes()) {
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
        val bucket = if (storagePath.startsWith("avatars/")) "avatars" else "fishing_photos"

        return "${SupabaseConfig.URL.trimEnd('/')}/storage/v1/object/public/$bucket/$storagePath"
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
