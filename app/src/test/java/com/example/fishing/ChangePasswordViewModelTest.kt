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

    @Test
    fun `short password is rejected without calling the repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = ChangePasswordViewModel(repository)

        viewModel.changePassword("123", "123")

        assertEquals(0, repository.changePasswordCalls)
        assertEquals("Пароль должен быть минимум 6 символов", viewModel.error.value)
        assertFalse(viewModel.success.value)
    }

    @Test
    fun `password mismatch is rejected without calling the repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = ChangePasswordViewModel(repository)

        viewModel.changePassword("secret1", "secret2")

        assertEquals(0, repository.changePasswordCalls)
        assertEquals("Пароли не совпадают", viewModel.error.value)
    }

    @Test
    fun `successful password change reports success`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = ChangePasswordViewModel(repository)

        viewModel.changePassword("secret1", "secret1")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.changePasswordCalls)
        assertEquals("secret1", repository.lastChangedPassword)
        assertTrue(viewModel.success.value)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `password change failure surfaces an error`() = runTest {
        val repository = FakeAuthRepository().apply {
            changePasswordResult = Result.failure(Exception("Unable to resolve host"))
        }
        val viewModel = ChangePasswordViewModel(repository)

        viewModel.changePassword("secret1", "secret1")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Нет соединения с интернетом", viewModel.error.value)
        assertFalse(viewModel.success.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `consumeSuccess resets the event`() = runTest {
        val viewModel = ChangePasswordViewModel(FakeAuthRepository())

        viewModel.changePassword("secret1", "secret1")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.success.value)

        viewModel.consumeSuccess()

        assertFalse(viewModel.success.value)
    }
}
