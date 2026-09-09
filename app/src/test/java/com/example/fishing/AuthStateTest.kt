package com.example.fishing

import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.viewmodel.AuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AuthStateTest {

    private val user = User(id = UUID.randomUUID(), name = "Test", email = "t@example.com", image = "")

    @Test
    fun `authState starts in Loading before session restore`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        assertEquals(AuthState.Loading, viewModel.authState.value)
    }

    @Test
    fun `authState transitions Loading to Authenticated on session restore`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)
        val (emissions, job) = collectStates(viewModel)

        repository.setAuthState(AuthState.Authenticated(user))
        advanceUntilIdle()

        assertEquals(listOf(AuthState.Loading, AuthState.Authenticated(user)), emissions)
        assertEquals(user, (viewModel.authState.value as AuthState.Authenticated).user)
        job.cancel()
    }

    @Test
    fun `authState transitions Loading to Unauthenticated on session restore`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)
        val (emissions, job) = collectStates(viewModel)

        repository.setAuthState(AuthState.Unauthenticated)
        advanceUntilIdle()

        assertEquals(listOf(AuthState.Loading, AuthState.Unauthenticated), emissions)
        assertTrue(viewModel.authState.value is AuthState.Unauthenticated)
        job.cancel()
    }

    @Test
    fun `logout transitions Authenticated to Unauthenticated`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)
        val (emissions, job) = collectStates(viewModel)

        repository.setAuthState(AuthState.Authenticated(user))
        advanceUntilIdle()
        assertTrue(viewModel.authState.value is AuthState.Authenticated)

        repository.logout()
        advanceUntilIdle()

        assertEquals(1, repository.logoutCalls)
        assertTrue(viewModel.authState.value is AuthState.Unauthenticated)
        assertEquals(AuthState.Unauthenticated, emissions.last())
        job.cancel()
    }

    private fun TestScope.collectStates(
        viewModel: AuthViewModel
    ): Pair<MutableList<AuthState>, Job> {
        val collected = mutableListOf<AuthState>()
        val job = launch {
            viewModel.authState.collect { collected.add(it) }
        }
        advanceUntilIdle()
        return collected to job
    }
}