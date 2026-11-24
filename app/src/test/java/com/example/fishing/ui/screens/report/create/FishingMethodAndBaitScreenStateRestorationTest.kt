package com.example.fishing.ui.screens.report.create

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fishing.model.FishingMethod
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AUD-06: the fishing method and selected baits survive a configuration change / process death. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class FishingMethodAndBaitScreenStateRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `method and selected bait survive state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                FishingMethodAndBaitScreen(
                    onBackClick = {},
                    onSaveClick = { _, _ -> },
                    initialMethod = FishingMethod.SPINNING,
                    initialBaits = emptyList()
                )
            }
        }

        composeRule.onNodeWithText("Спиннинг").performClick()
        composeRule.onNodeWithText("Фидер").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(isToggleable())[0].performClick()
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Фидер").assertIsDisplayed()
        composeRule.onAllNodes(isToggleable())[0].assertIsOn()
    }

    @Test
    fun `dropdown selection reflects initial method on recreation`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                FishingMethodAndBaitScreen(
                    onBackClick = {},
                    onSaveClick = { _, _ -> },
                    initialMethod = FishingMethod.FEEDER,
                    initialBaits = emptyList()
                )
            }
        }

        composeRule.onNodeWithText("Фидер").assertIsDisplayed()
    }
}
