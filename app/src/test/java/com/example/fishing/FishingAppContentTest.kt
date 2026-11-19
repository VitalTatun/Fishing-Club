package com.example.fishing

import android.app.Application
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.isHiddenFromAccessibility
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.fishing.model.AuthState
import com.example.fishing.ui.navigation.LoginRoute
import com.example.fishing.ui.navigation.MainRoute
import com.example.fishing.ui.navigation.SplashRoute
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose tests for [FishingAppContent], the root of the activity.
 *
 * BUG-01: the navigation host used to be removed from the composition while the
 * stored session was loading, so it was recreated afterwards and the back stack
 * restored after process death was lost. These tests pin the invariants that keep
 * the fix from regressing: the host stays composed, and the spinner is an input
 * blocking overlay on top of it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class FishingAppContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val authState = mutableStateOf<AuthState>(AuthState.Loading)

    private lateinit var backStack: NavBackStack<NavKey>

    @Before
    fun setUp() {
        // Most tests start on the destination a restored session lands on.
        backStack = newBackStack()
        backStack.add(MainRoute)
    }

    private fun newBackStack() = NavBackStack<NavKey>(SplashRoute)

    private fun setContent(
        stack: NavBackStack<NavKey> = backStack,
        contentLaunches: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                FishingAppContent(authState = authState.value, backStack = stack) {
                    if (contentLaunches != null) {
                        LaunchedEffect(Unit) { contentLaunches() }
                    }
                    Box(
                        modifier = if (onClick != null) {
                            Modifier.fillMaxSize().clickable { onClick() }
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) {
                        Text("nav-host")
                    }
                }
            }
        }
    }

    private fun assertOverlayVisible() {
        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    private fun assertOverlayHidden() {
        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }

    @Test
    fun `nav host stays composed while the session is loading`() {
        setContent()

        composeRule.onNodeWithText("nav-host").assertExists()
        assertOverlayVisible()
    }

    @Test
    fun `restored screen is hidden from accessibility while the spinner is up`() {
        setContent()

        // The screen below is still on the back stack, but TalkBack must not reach it
        // while the spinner covers it.
        composeRule.onNode(isHiddenFromAccessibility()).assertExists()

        composeRule.runOnIdle { authState.value = AuthState.Unauthenticated }
        composeRule.waitForIdle()

        composeRule.onNode(isHiddenFromAccessibility()).assertDoesNotExist()
    }

    @Test
    fun `nav host is kept, not recreated, when the session finishes loading`() {
        var contentEntries = 0
        setContent(contentLaunches = { contentEntries++ })

        composeRule.onNodeWithText("nav-host").assertExists()

        composeRule.runOnIdle { authState.value = AuthState.Unauthenticated }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("nav-host").assertExists()
        assertOverlayHidden()
        // A new LaunchedEffect would mean the content left the composition and the
        // nav host (with its restored back stack) was rebuilt from scratch.
        assertEquals(1, contentEntries)
    }

    @Test
    fun `spinner overlay swallows taps aimed at the screen below`() {
        var clicks = 0
        setContent(onClick = { clicks++ })

        // The click lands in the middle of the restored screen, but the overlay is
        // on top of it.
        composeRule.onRoot().performTouchInput { click(center) }
        composeRule.waitForIdle()
        assertEquals(0, clicks)

        // Once the session is restored the click reaches the screen again.
        composeRule.runOnIdle { authState.value = AuthState.Unauthenticated }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(center) }
        composeRule.waitForIdle()
        assertEquals(1, clicks)
    }

    @Test
    fun `spinner overlay consumes taps before a parent gesture handler sees them`() {
        // Hit testing alone already shields siblings of the overlay; consuming the
        // pointer events is what stops the handlers higher up in the same hit path
        // (a parent of the nav host) from acting on a tap aimed at the spinner.
        var parentClicks = 0
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                Box(modifier = Modifier.fillMaxSize().clickable { parentClicks++ }) {
                    FishingAppContent(
                        authState = authState.value,
                        backStack = backStack
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) { Text("nav-host") }
                    }
                }
            }
        }

        composeRule.onRoot().performTouchInput { click(center) }
        composeRule.waitForIdle()
        assertEquals(0, parentClicks)

        // Once the session is restored the tap reaches the parent again.
        composeRule.runOnIdle { authState.value = AuthState.Unauthenticated }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(center) }
        composeRule.waitForIdle()
        assertEquals(1, parentClicks)
    }

    @Test
    fun `back gesture does not reach the restored screen while the spinner is up`() {
        var contentBackPresses = 0
        var backDispatcher: OnBackPressedDispatcher? = null
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current)
                    .onBackPressedDispatcher
                FishingAppContent(authState = authState.value, backStack = backStack) {
                    // Stands in for the restored form's draft discard handler.
                    BackHandler { contentBackPresses++ }
                    Text("nav-host")
                }
            }
        }

        composeRule.runOnIdle { backDispatcher?.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(0, contentBackPresses)

        composeRule.runOnIdle { authState.value = AuthState.Unauthenticated }
        composeRule.waitForIdle()

        composeRule.runOnIdle { backDispatcher?.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(1, contentBackPresses)
    }

    @Test
    fun `overlay covers the empty splash destination after the session is restored`() {
        val splashBackStack = newBackStack()
        authState.value = AuthState.Unauthenticated
        setContent(stack = splashBackStack)

        // Dropping the spinner together with Loading would flash a blank frame before
        // the auth effect moves the back stack off the splash destination.
        assertOverlayVisible()

        composeRule.runOnIdle { splashBackStack.add(LoginRoute) }
        composeRule.waitForIdle()

        assertOverlayHidden()
    }
}
