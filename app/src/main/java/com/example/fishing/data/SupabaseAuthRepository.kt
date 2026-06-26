package com.example.fishing.data

import com.example.fishing.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = currentUser() ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String): Result<User> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = currentUser() ?: throw Exception("Registration failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        supabase.auth.signOut()
    }

    override fun currentUser(): User? {
        return supabase.auth.currentUserOrNull()?.toDomainUser()
    }

    override fun isLoggedIn(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    private fun UserInfo.toDomainUser(): User {
        return User(
            id = UUID.fromString(id),
            name = userMetadata?.get("full_name")?.toString()
                ?: email?.split("@")?.firstOrNull()
                ?: "User",
            email = email ?: "",
            image = userMetadata?.get("avatar_url")?.toString() ?: ""
        )
    }
}
