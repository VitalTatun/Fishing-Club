package com.example.fishing.ui.navigation

import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for the auth driven part of the navigation graph.
 *
 * BUG-01: the nav host is no longer recreated when the auth state changes, so these
 * rules decide whether the current back stack is replaced. A stack restored after
 * process death (a report form) must never be reset by the session restore.
 */
class AuthNavigationTest {

    private val user = User(
        id = UUID.randomUUID(),
        name = "Test User",
        email = "test@example.com",
        image = ""
    )

    private val authenticated = AuthState.Authenticated(user)

    // --- Loading ---

    @Test
    fun `loading leaves the splash destination alone`() {
        assertNull(resolveAuthNavigation(AuthState.Loading, AuthRoute.Splash))
    }

    @Test
    fun `loading leaves a restored destination alone`() {
        assertNull(resolveAuthNavigation(AuthState.Loading, AuthRoute.Other))
    }

    // --- Authenticated ---

    @Test
    fun `signed in user moves from splash to the main screen`() {
        assertEquals(MainRoute, resolveAuthNavigation(authenticated, AuthRoute.Splash))
    }

    @Test
    fun `successful login replaces the login screen with the main screen`() {
        assertEquals(MainRoute, resolveAuthNavigation(authenticated, AuthRoute.Login))
    }

    @Test
    fun `registration that establishes a session lands on the main screen`() {
        assertEquals(MainRoute, resolveAuthNavigation(authenticated, AuthRoute.Registration))
    }

    @Test
    fun `restored report form is not reset by the session restore`() {
        // The form is gone from the auth point of view: it is a regular destination.
        assertNull(resolveAuthNavigation(authenticated, AuthRoute.Other))
    }

    // --- Unauthenticated ---

    @Test
    fun `cold start without a session moves from splash to login`() {
        assertEquals(LoginRoute, resolveAuthNavigation(AuthState.Unauthenticated, AuthRoute.Splash))
    }

    @Test
    fun `login screen stays when there is no session`() {
        assertNull(resolveAuthNavigation(AuthState.Unauthenticated, AuthRoute.Login))
    }

    @Test
    fun `registration screen stays when there is no session`() {
        assertNull(resolveAuthNavigation(AuthState.Unauthenticated, AuthRoute.Registration))
    }

    @Test
    fun `logout from a regular destination replaces the stack with login`() {
        // The main screen and report details are both regular destinations, the whole
        // stack is replaced by the login screen either way.
        assertEquals(LoginRoute, resolveAuthNavigation(AuthState.Unauthenticated, AuthRoute.Other))
    }
}
