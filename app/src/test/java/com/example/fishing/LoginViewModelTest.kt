package com.example.fishing

import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.MainDispatcherRule
import com.example.fishing.viewmodel.LoginViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val user = User(id = UUID.randomUUID(), name = "Test", email = "test@example.com", image = "")

    @Test
    fun `login with valid credentials succeeds`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        repository.loginResult = Result.success(user)
        val viewModel = LoginViewModel(repository)

        viewModel.email = "test@example.com"
        viewModel.password = "password123"
        viewModel.login()
        advanceUntilIdle()

        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals(1, repository.loginCalls)
        assertTrue(repository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `login with invalid credentials shows error`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        repository.loginResult = Result.failure(Exception("Invalid login credentials"))
        val viewModel = LoginViewModel(repository)

        viewModel.email = "test@example.com"
        viewModel.password = "wrong-password"
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Неверный email или пароль", viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
        assertTrue(repository.authState.value is AuthState.Unauthenticated)
    }

    @Test
    fun `login with network error shows connection message`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        repository.loginResult = Result.failure(Exception("Unable to resolve host"))
        val viewModel = LoginViewModel(repository)

        viewModel.email = "test@example.com"
        viewModel.password = "password123"
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Нет соединения с интернетом", viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `login with blank email shows validation error and skips request`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        viewModel.email = ""
        viewModel.password = "password123"
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Введите email", viewModel.error.value)
        assertEquals(0, repository.loginCalls)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `login with invalid email format shows validation error`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        viewModel.email = "not-an-email"
        viewModel.password = "password123"
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Некорректный формат email", viewModel.error.value)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `login with blank password shows validation error`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        viewModel.email = "test@example.com"
        viewModel.password = ""
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Введите пароль", viewModel.error.value)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `login sets loading while in flight and clears it after completion`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            repository.loginGate = CompletableDeferred()
            val viewModel = LoginViewModel(repository)

            viewModel.email = "test@example.com"
            viewModel.password = "password123"
            viewModel.login()
            runCurrent()

            assertTrue(viewModel.isLoading.value)

            repository.loginGate!!.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.isLoading.value)
            assertNull(viewModel.error.value)
        }

    @Test
    fun `error is cleared on next successful login`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        repository.loginResult = Result.failure(Exception("Invalid login credentials"))
        val viewModel = LoginViewModel(repository)

        viewModel.email = "test@example.com"
        viewModel.password = "bad"
        viewModel.login()
        advanceUntilIdle()
        assertEquals("Неверный email или пароль", viewModel.error.value)

        repository.loginResult = Result.success(user)
        viewModel.login()
        advanceUntilIdle()

        assertNull(viewModel.error.value)
    }
}