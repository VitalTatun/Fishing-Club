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
    fun resolveImageUrl(path: String): String
}
