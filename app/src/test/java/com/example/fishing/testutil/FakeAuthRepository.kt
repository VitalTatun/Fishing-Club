package com.example.fishing.testutil

import com.example.fishing.data.AuthRepository
import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FakeAuthRepository : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var loginResult: Result<User> = Result.success(
        User(id = UUID.randomUUID(), name = "Test", email = "test@example.com", image = "")
    )
    var registerResult: Result<User> = Result.success(
        User(id = UUID.randomUUID(), name = "Test", email = "test@example.com", image = "")
    )
    var updateProfileResult: Result<User> = Result.success(
        User(id = UUID.randomUUID(), name = "Updated", email = "test@example.com", image = "")
    )
    var changePasswordResult: Result<Unit> = Result.success(Unit)
    var deleteAccountResult: Result<Unit> = Result.success(Unit)
    var sessionUser: User? = null
        set(value) {
            field = value
            _authState.value = if (value != null) {
                AuthState.Authenticated(value)
            } else {
                AuthState.Unauthenticated
            }
        }

    var loginGate: CompletableDeferred<Unit>? = null
    var registerGate: CompletableDeferred<Unit>? = null
    var updateProfileGate: CompletableDeferred<Unit>? = null

    var registerSetsSession = true

    var loginCalls = 0
    var registerCalls = 0
    var logoutCalls = 0
    var updateProfileCalls = 0
    var lastUpdateProfileName: String? = null
    var lastUpdateProfileImage: String? = null
    var changePasswordCalls = 0
    var lastChangedPassword: String? = null
    var deleteAccountCalls = 0

    override suspend fun login(email: String, password: String): Result<User> {
        loginCalls++
        loginGate?.await()
        sessionUser = loginResult.getOrNull()
        return loginResult
    }

    override suspend fun register(email: String, password: String, name: String): Result<User> {
        registerCalls++
        registerGate?.await()
        if (registerSetsSession) {
            sessionUser = registerResult.getOrNull()
        }
        return registerResult
    }

    override suspend fun logout() {
        logoutCalls++
        sessionUser = null
    }

    override fun currentUser(): User? = sessionUser

    override suspend fun loadSession() {
        // No-op for tests
    }

    override suspend fun updateProfile(name: String, imageUri: String?): Result<User> {
        updateProfileCalls++
        lastUpdateProfileName = name
        lastUpdateProfileImage = imageUri
        updateProfileGate?.await()
        return updateProfileResult
    }

    override suspend fun changePassword(newPassword: String): Result<Unit> {
        changePasswordCalls++
        lastChangedPassword = newPassword
        return changePasswordResult
    }

    override suspend fun deleteAccount(): Result<Unit> {
        deleteAccountCalls++
        if (deleteAccountResult.isSuccess) {
            sessionUser = null
        }
        return deleteAccountResult
    }

    override fun resolveImageUrl(path: String): String = path

    fun setAuthState(state: AuthState) {
        _authState.value = state
    }
}