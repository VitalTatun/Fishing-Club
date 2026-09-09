package com.example.fishing

import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.MainDispatcherRule
import com.example.fishing.viewmodel.RegistrationResult
import com.example.fishing.viewmodel.RegistrationViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val user = User(id = UUID.randomUUID(), name = "Test", email = "new@example.com", image = "")

    private fun fillValidForm(viewModel: RegistrationViewModel) {
        viewModel.name = "Иван"
        viewModel.email = "new@example.com"
        viewModel.password = "password123"
        viewModel.confirmPassword = "password123"
    }

    @Test
    fun `register with valid data succeeds when session is created`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            repository.registerResult = Result.success(user)
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)

            viewModel.register()
            advanceUntilIdle()

            assertEquals(RegistrationResult.Success, viewModel.result.value)
            assertEquals(1, repository.registerCalls)
        }

    @Test
    fun `register with valid data reports email confirmation when no session`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            repository.registerResult = Result.success(user)
            repository.registerSetsSession = false
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)

            viewModel.register()
            advanceUntilIdle()

            assertEquals(RegistrationResult.NeedsEmailConfirmation, viewModel.result.value)
            assertEquals(1, repository.registerCalls)
        }

    @Test
    fun `register with invalid email shows validation error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)
            viewModel.email = "not-an-email"

            viewModel.register()
            advanceUntilIdle()

            assertEquals(
                RegistrationResult.Error("Некорректный формат email"),
                viewModel.result.value
            )
            assertEquals(0, repository.registerCalls)
        }

    @Test
    fun `register with empty email shows validation error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)
            viewModel.email = ""

            viewModel.register()
            advanceUntilIdle()

            assertEquals(RegistrationResult.Error("Введите email"), viewModel.result.value)
            assertEquals(0, repository.registerCalls)
        }

    @Test
    fun `register with empty name shows validation error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)
            viewModel.name = ""

            viewModel.register()
            advanceUntilIdle()

            assertEquals(RegistrationResult.Error("Введите имя"), viewModel.result.value)
            assertEquals(0, repository.registerCalls)
        }

    @Test
    fun `register with short password shows validation error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)
            viewModel.password = "12345"
            viewModel.confirmPassword = "12345"

            viewModel.register()
            advanceUntilIdle()

            assertEquals(
                RegistrationResult.Error("Пароль должен быть минимум 6 символов"),
                viewModel.result.value
            )
            assertEquals(0, repository.registerCalls)
        }

    @Test
    fun `register with password mismatch shows validation error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)
            viewModel.confirmPassword = "different"

            viewModel.register()
            advanceUntilIdle()

            assertEquals(RegistrationResult.Error("Пароли не совпадают"), viewModel.result.value)
            assertEquals(0, repository.registerCalls)
        }

    @Test
    fun `register with duplicate email shows server error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            repository.registerResult = Result.failure(Exception("User already registered"))
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)

            viewModel.register()
            advanceUntilIdle()

            assertEquals(
                RegistrationResult.Error("Этот email уже зарегистрирован"),
                viewModel.result.value
            )
            assertEquals(1, repository.registerCalls)
        }

    @Test
    fun `register with network error shows connection message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            repository.registerResult = Result.failure(Exception("timeout"))
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)

            viewModel.register()
            advanceUntilIdle()

            assertEquals(
                RegistrationResult.Error("Нет соединения с интернетом"),
                viewModel.result.value
            )
        }

    @Test
    fun `register sets loading while in flight and clears it after completion`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            repository.registerGate = CompletableDeferred()
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)

            viewModel.register()
            runCurrent()

            assertTrue(viewModel.isLoading)

            repository.registerGate!!.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.isLoading)
            assertEquals(RegistrationResult.Success, viewModel.result.value)
        }

    @Test
    fun `error is cleared and form stays usable after failed register`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            repository.registerResult = Result.failure(Exception("User already registered"))
            val viewModel = RegistrationViewModel(repository)
            fillValidForm(viewModel)

            viewModel.register()
            advanceUntilIdle()
            assertTrue(viewModel.result.value is RegistrationResult.Error)

            repository.registerResult = Result.success(user)
            viewModel.register()
            advanceUntilIdle()

            assertEquals(RegistrationResult.Success, viewModel.result.value)
        }
}