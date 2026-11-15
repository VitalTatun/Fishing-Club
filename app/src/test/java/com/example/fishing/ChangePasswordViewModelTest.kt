package com.example.fishing

import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.MainDispatcherRule
import com.example.fishing.viewmodel.ChangePasswordViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** AUD-03: password change validation and repository outcome handling. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val currentPassword = "Qwerty1234"
    private val newPassword = "Abcdefgh12"

    private fun viewModel(repository: FakeAuthRepository = FakeAuthRepository()) =
        ChangePasswordViewModel(repository)

    @Test
    fun `blank current password is rejected without calling the repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.changePassword("", newPassword, newPassword)

        assertEquals(0, repository.changePasswordCalls)
        assertEquals("Введите текущий пароль", viewModel.error.value)
    }

    @Test
    fun `short password is rejected without calling the repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.changePassword(currentPassword, "123", "123")

        assertEquals(0, repository.changePasswordCalls)
        assertEquals("Пароль должен быть минимум 10 символов", viewModel.error.value)
        assertFalse(viewModel.success.value)
    }

    @Test
    fun `password without a digit is rejected`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.changePassword(currentPassword, "Abcdefghij", "Abcdefghij")

        assertEquals(0, repository.changePasswordCalls)
        assertEquals("Пароль должен содержать цифру", viewModel.error.value)
    }

    @Test
    fun `password without an uppercase letter is rejected`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.changePassword(currentPassword, "abcdefghij1", "abcdefghij1")

        assertEquals(0, repository.changePasswordCalls)
        assertEquals("Пароль должен содержать заглавную букву", viewModel.error.value)
    }

    @Test
    fun `password without a lowercase letter is rejected`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.changePassword(currentPassword, "ABCDEFGHIJ1", "ABCDEFGHIJ1")

        assertEquals(0, repository.changePasswordCalls)
        assertEquals("Пароль должен содержать строчную букву", viewModel.error.value)
    }

    @Test
    fun `password mismatch is rejected without calling the repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.changePassword(currentPassword, "Abcdefgh12", "Abcdefgh34")

        assertEquals(0, repository.changePasswordCalls)
        assertEquals("Пароли не совпадают", viewModel.error.value)
    }

    @Test
    fun `new password equal to the current one is rejected`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.changePassword(currentPassword, currentPassword, currentPassword)

        assertEquals(0, repository.changePasswordCalls)
        assertEquals("Новый пароль должен отличаться от текущего", viewModel.error.value)
    }

    @Test
    fun `successful password change passes both passwords to the repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.changePassword(currentPassword, newPassword, newPassword)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.changePasswordCalls)
        assertEquals(newPassword, repository.lastChangedPassword)
        assertEquals(currentPassword, repository.lastCurrentPassword)
        assertTrue(viewModel.success.value)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `wrong current password surfaces the server error`() = runTest {
        val repository = FakeAuthRepository().apply {
            changePasswordResult = Result.failure(
                Exception(
                    "current_password_mismatch\n" +
                        "Current password required when setting new password.: current_password_mismatch"
                )
            )
        }
        val viewModel = viewModel(repository)

        viewModel.changePassword(currentPassword, newPassword, newPassword)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Неверный текущий пароль", viewModel.error.value)
        assertFalse(viewModel.success.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `password change failure surfaces an error`() = runTest {
        val repository = FakeAuthRepository().apply {
            changePasswordResult = Result.failure(Exception("Unable to resolve host"))
        }
        val viewModel = viewModel(repository)

        viewModel.changePassword(currentPassword, newPassword, newPassword)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Нет соединения с интернетом", viewModel.error.value)
        assertFalse(viewModel.success.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `consumeSuccess resets the event`() = runTest {
        val viewModel = viewModel()

        viewModel.changePassword(currentPassword, newPassword, newPassword)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.success.value)

        viewModel.consumeSuccess()

        assertFalse(viewModel.success.value)
    }
}
