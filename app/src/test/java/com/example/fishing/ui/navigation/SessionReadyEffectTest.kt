package com.example.fishing.ui.navigation

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Tests the session gate used by the loads inside the nav graph.
 *
 * BUG-01: a destination restored after process death is composed while the stored
 * session is still loading. Report details, the public profile and the favourites
 * list all need that session, and their own effect key never changes again, so a
 * load fired too early would never retry.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class SessionReadyEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val user = User(
        id = UUID.randomUUID(),
        name = "Test User",
        email = "test@example.com",
        image = ""
    )

    @Test
    fun `restored report details are not loaded while the session is loading`() {
        var loads = 0
        val authState = mutableStateOf<AuthState>(AuthState.Loading)
        composeRule.setContent {
            SessionReadyEffect(key = "report", authState = authState.value) { loads++ }
        }

        composeRule.waitForIdle()
        assertEquals(0, loads)

        composeRule.runOnIdle { authState.value = AuthState.Authenticated(user) }
        composeRule.waitForIdle()
        assertEquals(1, loads)

        // A later auth emission (for example a logout) must not load the page again.
        composeRule.runOnIdle { authState.value = AuthState.Unauthenticated }
        composeRule.waitForIdle()
        assertEquals(1, loads)
    }

    @Test
    fun `load runs immediately when the session is already resolved`() {
        var loads = 0
        composeRule.setContent {
            SessionReadyEffect(key = "report", authState = AuthState.Authenticated(user)) {
                loads++
            }
        }

        composeRule.waitForIdle()
        assertEquals(1, loads)
    }

    @Test
    fun `load restarts for a new key once the session is ready`() {
        var loads = 0
        val key = mutableStateOf<Any?>("report-1")
        composeRule.setContent {
            SessionReadyEffect(key = key.value, authState = AuthState.Unauthenticated) { loads++ }
        }

        composeRule.waitForIdle()
        assertEquals(1, loads)

        composeRule.runOnIdle { key.value = "report-2" }
        composeRule.waitForIdle()
        assertEquals(2, loads)
    }
}
