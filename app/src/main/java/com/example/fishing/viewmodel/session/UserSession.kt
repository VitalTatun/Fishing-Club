package com.example.fishing.viewmodel.session

import com.example.fishing.data.AuthRepository
import java.util.UUID

/**
 * Single source of truth for the user whose data is currently displayed.
 *
 * Coordinators resolve the id through [resolve] and bind it once an operation
 * starts. Logout clears it synchronously, so late coroutine results can compare
 * their captured id with [currentUserId] and avoid restoring stale state.
 */
class UserSession(
    private val authRepository: AuthRepository
) {
    var currentUserId: UUID? = null
        private set

    fun resolve(): UUID? = currentUserId ?: authRepository.currentUser()?.id

    fun bind(): UUID? = resolve()?.also { currentUserId = it }

    fun set(userId: UUID?) {
        currentUserId = userId
    }

    fun clear() {
        currentUserId = null
    }
}
