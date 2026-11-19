package com.example.fishing.ui.screens.profile

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w891dp-h411dp"
)
class ProfileScreenLandscapeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `logout button remains readable and scrolls into view in landscape`() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.fillMaxWidth().height(64.dp))
                    ProfileScreen(
                        userName = "Test User",
                        userEmail = "test@example.com",
                        avatarUrl = null,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.fillMaxWidth().height(72.dp))
                }
            }
        }

        val logoutButton = composeRule.onNode(hasText("Выйти") and hasClickAction())
        logoutButton.performScrollTo()
        logoutButton.assertIsDisplayed()

        val buttonBounds = logoutButton.fetchSemanticsNode().boundsInRoot
        val rootWidth = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.width
        val minReadableWidth = with(composeRule.density) { 240.dp.toPx() }
        assertTrue(buttonBounds.width >= minReadableWidth)
        assertTrue(buttonBounds.left >= 0f && buttonBounds.right <= rootWidth)
    }
}
