package com.example.fishing.ui.navigation

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Tests [AuthNavigationEffect] against a real type-safe navigation graph.
 *
 * BUG-01: the effect is the only thing that reacts to an auth change now that the
 * nav host is not recreated anymore, so it must never reset a stack that was restored
 * after process death.
 */
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

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(RuntimeEnvironment.getApplication())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.graph = navController.createGraph(startDestination = SplashRoute) {
            composable<SplashRoute> { }
            composable<LoginRoute> { }
            composable<MainRoute> { }
            composable<CreateReportRoute> { }
            composable<ReportDetailRoute> { }
        }
    }

    private fun setEffect(initialState: AuthState): MutableState<AuthState> {
        val authState = mutableStateOf(initialState)
        composeRule.setContent {
            AuthNavigationEffect(authState = authState.value, navController = navController)
        }
        composeRule.waitForIdle()
        return authState
    }

    private fun emit(authState: MutableState<AuthState>, state: AuthState) {
        composeRule.runOnIdle { authState.value = state }
        composeRule.waitForIdle()
    }

    private fun isOnCreateReportForm(): Boolean =
        navController.currentDestination?.hasRoute<CreateReportRoute>() == true

    private fun isOnLogin(): Boolean =
        navController.currentDestination?.hasRoute<LoginRoute>() == true

    private fun isOnMain(): Boolean =
        navController.currentDestination?.hasRoute<MainRoute>() == true

    /** Number of real screens on the stack, the root graph entry is not a screen. */
    private fun screenCount(): Int =
        navController.currentBackStack.value.count { it.destination !is NavGraph }

    @Test
    fun `restored report form is not reset by the session restore`() {
        navController.navigate(MainRoute)
        navController.navigate(CreateReportRoute)
        val authState = setEffect(AuthState.Loading)
        val screensBefore = screenCount()

        emit(authState, authenticated)

        assertTrue(isOnCreateReportForm())
        assertEquals(screensBefore, screenCount())
    }

    @Test
    fun `successful login replaces the login screen with the main screen`() {
        navController.navigate(LoginRoute)
        val authState = setEffect(AuthState.Unauthenticated)

        emit(authState, authenticated)

        assertTrue(isOnMain())
        assertEquals(1, screenCount())
        assertNull(navController.previousBackStackEntry)
    }

    @Test
    fun `logout from a report detail screen clears the stack and opens login`() {
        navController.navigate(MainRoute)
        navController.navigate(ReportDetailRoute(reportId = UUID.randomUUID().toString()))
        val authState = setEffect(authenticated)

        emit(authState, AuthState.Unauthenticated)

        assertTrue(isOnLogin())
        assertEquals(1, screenCount())
        assertNull(navController.previousBackStackEntry)
    }

    @Test
    fun `loading does not touch the destination`() {
        navController.navigate(LoginRoute)
        val authState = setEffect(AuthState.Unauthenticated)
        val screensBefore = screenCount()

        assertTrue(isOnLogin())

        emit(authState, AuthState.Loading)

        assertTrue(isOnLogin())
        assertEquals(screensBefore, screenCount())
    }

    @Test
    fun `cold start of a signed in user lands on the main screen`() {
        val authState = setEffect(AuthState.Loading)

        emit(authState, authenticated)

        assertTrue(isOnMain())
        assertNull(navController.previousBackStackEntry)
    }

    @Test
    fun `cold start without a session opens login`() {
        val authState = setEffect(AuthState.Loading)

        emit(authState, AuthState.Unauthenticated)

        assertTrue(isOnLogin())
        assertEquals(1, screenCount())
    }
}
