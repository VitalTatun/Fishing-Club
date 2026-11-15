package com.example.fishing.ui.navigation

import android.app.Application
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import com.example.fishing.model.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests the glue between the live [androidx.navigation.NavDestination] and [AuthRoute].
 *
 * The destinations come from a real, type-safe navigation graph, so `hasRoute<T>()`
 * is exercised exactly as it is at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class AuthRouteTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(RuntimeEnvironment.getApplication())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.graph = navController.createGraph(startDestination = SplashRoute) {
            composable<SplashRoute> { }
            composable<LoginRoute> { }
            composable<RegistrationRoute> { }
            composable<MainRoute> { }
            composable<CreateReportRoute> { }
        }
    }

    @Test
    fun `missing destination is treated as the splash destination`() {
        assertEquals(AuthRoute.Splash, AuthRoute.of(null))
    }

    @Test
    fun `splash destination is recognised`() {
        assertEquals(AuthRoute.Splash, AuthRoute.of(navController.graph.findNode<SplashRoute>()))
    }

    @Test
    fun `login destination is recognised`() {
        assertEquals(AuthRoute.Login, AuthRoute.of(navController.graph.findNode<LoginRoute>()))
    }

    @Test
    fun `registration destination is recognised`() {
        assertEquals(
            AuthRoute.Registration,
            AuthRoute.of(navController.graph.findNode<RegistrationRoute>())
        )
    }

    @Test
    fun `main destination is a regular destination`() {
        assertEquals(AuthRoute.Other, AuthRoute.of(navController.graph.findNode<MainRoute>()))
    }

    @Test
    fun `restored report form is a regular destination`() {
        // The form restored after process death must be treated as a regular
        // destination: the session restore is not allowed to reset it (BUG-01).
        assertEquals(
            AuthRoute.Other,
            AuthRoute.of(navController.graph.findNode<CreateReportRoute>())
        )
    }

    @Test
    fun `overlay is shown before the graph is installed`() {
        assertTrue(isAuthOverlayVisible(AuthState.Unauthenticated, null))
    }

    @Test
    fun `overlay stays while the session is loading`() {
        assertTrue(
            isAuthOverlayVisible(
                AuthState.Loading,
                navController.graph.findNode<MainRoute>()
            )
        )
    }

    @Test
    fun `overlay stays while the host is still on the splash destination`() {
        // The session is already restored, but the auth effect has not moved off the
        // empty splash destination yet.
        assertTrue(
            isAuthOverlayVisible(
                AuthState.Unauthenticated,
                navController.graph.findNode<SplashRoute>()
            )
        )
    }

    @Test
    fun `overlay is gone on a real destination once the session is restored`() {
        assertFalse(
            isAuthOverlayVisible(
                AuthState.Unauthenticated,
                navController.graph.findNode<MainRoute>()
            )
        )
    }
}
