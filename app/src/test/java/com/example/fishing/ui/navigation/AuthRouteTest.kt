package com.example.fishing.ui.navigation

import com.example.fishing.model.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRouteTest {

    @Test
    fun `missing key is treated as splash`() {
        assertEquals(AuthRoute.Splash, AuthRoute.of(null))
    }

    @Test
    fun `auth keys are recognised`() {
        assertEquals(AuthRoute.Splash, AuthRoute.of(SplashRoute))
        assertEquals(AuthRoute.Login, AuthRoute.of(LoginRoute))
        assertEquals(AuthRoute.Registration, AuthRoute.of(RegistrationRoute))
    }

    @Test
    fun `app destinations are regular destinations`() {
        assertEquals(AuthRoute.Other, AuthRoute.of(MainRoute))
        assertEquals(AuthRoute.Other, AuthRoute.of(CreateReportRoute))
        assertEquals(AuthRoute.Other, AuthRoute.of(ReportDetailRoute("report-id")))
    }

    @Test
    fun `overlay is shown before the first key is displayed`() {
        assertTrue(isAuthOverlayVisible(AuthState.Unauthenticated, null))
    }

    @Test
    fun `overlay stays while the session is loading`() {
        assertTrue(isAuthOverlayVisible(AuthState.Loading, MainRoute))
    }

    @Test
    fun `overlay stays while the stack is on splash`() {
        assertTrue(isAuthOverlayVisible(AuthState.Unauthenticated, SplashRoute))
    }

    @Test
    fun `overlay is gone on a real destination once session is restored`() {
        assertFalse(isAuthOverlayVisible(AuthState.Unauthenticated, MainRoute))
    }
}
