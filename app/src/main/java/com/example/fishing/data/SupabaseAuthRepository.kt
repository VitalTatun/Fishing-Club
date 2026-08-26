package com.example.fishing.data

import com.example.fishing.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import com.example.fishing.data.supabase.ProfileDto
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository {

    private val _userStatus = MutableStateFlow<User?>(null)
    override val userStatus: Flow<User?> = _userStatus.asStateFlow()

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = currentUser() ?: throw Exception("User not found")
            syncProfile(user)
            _userStatus.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String, name: String): Result<User> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", name)
                }
            }
            val user = currentUser() ?: throw Exception("Registration failed")
            syncProfile(user)
            _userStatus.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        supabase.auth.signOut()
        _userStatus.value = null
    }

    override fun currentUser(): User? {
        val user = supabase.auth.currentUserOrNull()?.toDomainUser()
        _userStatus.value = user
        return user
    }

    override fun isLoggedIn(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    override suspend fun loadSession() {
        supabase.auth.awaitInitialization()
        val user = currentUser()
        if (user != null) syncProfile(user)
        _userStatus.value = user
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
                        // A new object path changes the image URL and avoids stale Coil/HTTP caches.
                        val storagePath = "avatars/${currentUser.id}/${UUID.randomUUID()}.$ext"
                        
                        Log.d("SupabaseAuth", "Attempting to upload to bucket 'avatars', path: $storagePath")
                        
                        supabase.storage.from("avatars").upload(storagePath, file.readBytes()) {
                            upsert = true
                        }
                        avatarUrl = storagePath
                        Log.d("SupabaseAuth", "Upload successful: $storagePath")
                    } catch (e: Exception) {
                        Log.e("SupabaseAuth", "Storage error: ${e.message}", e)
                        if (e.message?.contains("bucket", ignoreCase = true) == true && 
                            e.message?.contains("not found", ignoreCase = true) == true) {
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
            _userStatus.value = updatedUser
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
            // The auth operation has already succeeded; profile sync can be retried next time.
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
