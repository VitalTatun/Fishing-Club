package com.example.fishing.ui.screens.login

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.LoginViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Compose UI behaviour tests for [LoginScreen].
 *
 * They run on the JVM through Robolectric, so no emulator or real Supabase
 * connection is involved. Authentication is backed by [FakeAuthRepository].
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class LoginScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel
    private var registrationClicks = 0

    private val testUser = User(
        id = UUID.randomUUID(),
        name = "Test User",
        email = "test@example.com",
        image = ""
    )

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        viewModel = LoginViewModel(authRepository)
        registrationClicks = 0
    }

    private fun setLoginScreen() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegistration = { registrationClicks++ }
                )
            }
        }
    }

    /** Text fields are matched by their editable semantics in layout order. */
    private fun emailField() = composeRule.onAllNodes(hasSetTextAction())[0]

    private fun passwordField() = composeRule.onAllNodes(hasSetTextAction())[1]

    private fun typeCredentials(
        email: String = "test@example.com",
        password: String = "password123"
    ) {
        emailField().performTextInput(email)
        passwordField().performTextInput(password)
    }

    @Test
    fun `login form displays fields and actions`() {
        setLoginScreen()

        composeRule.onNodeWithText("Fishing Journal").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Пароль").assertIsDisplayed()
        composeRule.onNodeWithText("Войти").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Создать аккаунт").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun `successful login submits credentials`() {
        authRepository.loginResult = Result.success(testUser)
        setLoginScreen()

        typeCredentials(email = "  test@example.com  ", password = "password123")
        assertEquals("  test@example.com  ", viewModel.email)
        assertEquals("password123", viewModel.password)

        composeRule.onNodeWithText("Войти").performClick()
        composeRule.waitForIdle()

        assertEquals(1, authRepository.loginCalls)
        assertEquals(testUser, authRepository.sessionUser)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `failed login displays user friendly error`() {
        authRepository.loginResult =
            Result.failure(Exception("Invalid login credentials"))
        setLoginScreen()

        typeCredentials(password = "wrong-password")
        composeRule.onNodeWithText("Войти").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Неверный email или пароль").assertIsDisplayed()
        assertEquals("Неверный email или пароль", viewModel.error.value)
        assertTrue(authRepository.sessionUser == null)
    }

    @Test
    fun `blank submit displays validation error without calling repository`() {
        setLoginScreen()

        composeRule.onNodeWithText("Войти").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Введите email").assertIsDisplayed()
        assertEquals(0, authRepository.loginCalls)
    }

    @Test
    fun `loading state disables actions until the request completes`() {
        authRepository.loginGate = CompletableDeferred()
        authRepository.loginResult = Result.success(testUser)
        setLoginScreen()

        typeCredentials()
        composeRule.onNodeWithText("Войти").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.isLoading.value }

        // The login button renders a progress indicator instead of its label.
        composeRule.onNodeWithText("Войти").assertDoesNotExist()
        composeRule.onNodeWithText("Создать аккаунт").assertIsNotEnabled()

        authRepository.loginGate!!.complete(Unit)
        composeRule.waitUntil(timeoutMillis = 5_000) { !viewModel.isLoading.value }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Войти").assertIsDisplayed()
        assertNull(viewModel.error.value)
    }

    @Test
    fun `registration action navigates to registration`() {
        setLoginScreen()

        composeRule.onNodeWithText("Создать аккаунт").performClick()
        composeRule.waitForIdle()

        assertEquals(1, registrationClicks)
    }

    @Test
    fun `back navigation is not exposed by the login form`() {
        setLoginScreen()

        // Login is the root destination, so it must not render a back affordance.
        composeRule.onNodeWithContentDescription("Назад").assertDoesNotExist()
    }
}
