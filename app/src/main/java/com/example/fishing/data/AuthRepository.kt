package com.example.fishing.data

import com.example.fishing.model.User

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, name: String): Result<User>
    suspend fun logout()
    fun currentUser(): User?
    fun isLoggedIn(): Boolean
    suspend fun loadSession()
    suspend fun updateProfile(name: String, imageUri: String?): Result<User>
    val userStatus: Flow<User?>
    fun resolveImageUrl(path: String): String
}
