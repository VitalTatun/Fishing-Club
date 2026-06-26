package com.example.fishing.data

import com.example.fishing.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String): Result<User>
    suspend fun logout()
    fun currentUser(): User?
    fun isLoggedIn(): Boolean
}
