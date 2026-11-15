package com.example.fishing.ui.screens.report.create

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AUD-06: catch input and the fish list survive a configuration change / process death. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class CatchEditScreenStateRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `typed input and selected fish survive state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                CatchEditScreen(
                    fishList = emptyList(),
                    initialWeight = 0f,
                    onBackClick = {},
                    onSaveClick = { _, _ -> }
                )
            }
        }

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Судак")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("4.5")
        composeRule.onNodeWithText("Щука").performClick()
        composeRule.waitForIdle()
        // The suggestion chip plus the quantity row.
        composeRule.onAllNodesWithText("Щука").assertCountEquals(2)

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Судак").assertIsDisplayed()
        composeRule.onNodeWithText("4.5").assertIsDisplayed()
        composeRule.onAllNodesWithText("Щука").assertCountEquals(2)
    }
}
