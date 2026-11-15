package com.example.fishing.data

import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, name: String): Result<User>
    suspend fun logout()
    fun currentUser(): User?
    suspend fun loadSession()
    suspend fun updateProfile(name: String, imageUri: String?): Result<User>

    /**
     * Updates the password of the signed-in account.
     *
     * The Supabase project requires the current password before setting a new
     * one, so it is verified on the server as part of the same request.
     */
    suspend fun changePassword(newPassword: String, currentPassword: String): Result<Unit>

    /**
     * Deletes the signed-in account on the server and clears its local data.
     * The server call must run through a privileged RPC; the client cannot
     * delete rows from `auth.users` directly.
     */
    suspend fun deleteAccount(): Result<Unit>
    fun resolveImageUrl(path: String): String
}
