package com.example.fishing.ui.navigation

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class AuthNavigationEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val authenticated = AuthState.Authenticated(
        User(
            id = UUID.randomUUID(),
            name = "Test User",
            email = "test@example.com",
            image = ""
        )
    )

    private lateinit var backStack: NavBackStack<NavKey>

    @Before
    fun setUp() {
        backStack = NavBackStack<NavKey>(SplashRoute)
    }

    private fun setEffect(initialState: AuthState): MutableState<AuthState> {
        val authState = mutableStateOf(initialState)
        composeRule.setContent {
            AuthNavigationEffect(authState = authState.value, backStack = backStack)
        }
        composeRule.waitForIdle()
        return authState
    }

    private fun emit(authState: MutableState<AuthState>, state: AuthState) {
        composeRule.runOnIdle { authState.value = state }
        composeRule.waitForIdle()
    }

    @Test
    fun `restored report form is not reset by session restore`() {
        backStack.add(MainRoute)
        backStack.add(CreateReportRoute)
        val authState = setEffect(AuthState.Loading)
        val stackBefore = backStack.toList()

        emit(authState, authenticated)

        assertEquals(CreateReportRoute, backStack.last())
        assertEquals(stackBefore, backStack.toList())
    }

    @Test
    fun `successful login replaces the login stack with main`() {
        backStack.add(LoginRoute)
        val authState = setEffect(AuthState.Unauthenticated)

        emit(authState, authenticated)

        assertEquals(listOf(MainRoute), backStack.toList())
    }

    @Test
    fun `logout from report detail clears the stack and opens login`() {
        backStack.add(MainRoute)
        backStack.add(ReportDetailRoute(reportId = UUID.randomUUID().toString()))
        val authState = setEffect(authenticated)

        emit(authState, AuthState.Unauthenticated)

        assertEquals(listOf(LoginRoute), backStack.toList())
    }

    @Test
    fun `loading does not touch the current key`() {
        backStack.add(LoginRoute)
        val authState = setEffect(AuthState.Unauthenticated)
        val stackBefore = backStack.toList()

        emit(authState, AuthState.Loading)

        assertEquals(stackBefore, backStack.toList())
    }

    @Test
    fun `cold start of signed in user lands on main`() {
        val authState = setEffect(AuthState.Loading)

        emit(authState, authenticated)

        assertEquals(listOf(MainRoute), backStack.toList())
    }

    @Test
    fun `cold start without session opens login`() {
        val authState = setEffect(AuthState.Loading)

        emit(authState, AuthState.Unauthenticated)

        assertTrue(backStack.size == 1)
        assertEquals(LoginRoute, backStack.last())
    }
}
