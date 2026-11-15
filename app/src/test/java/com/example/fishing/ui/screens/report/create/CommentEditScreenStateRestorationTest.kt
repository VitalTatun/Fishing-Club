package com.example.fishing.ui.screens.report.create

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AUD-06: the edit sub-screens must keep in-progress input across a
 * configuration change / process death. [StateRestorationTester] drives the
 * same `rememberSaveable` machinery the real save/restore cycle uses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class CommentEditScreenStateRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `typed comment survives state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                CommentEditScreen(
                    initialComment = "",
                    onBackClick = {},
                    onSaveClick = {}
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("Клёв был отличный")
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Клёв был отличный").assertIsDisplayed()
    }
}
