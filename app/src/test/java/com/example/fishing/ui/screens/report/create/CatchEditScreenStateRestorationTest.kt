package com.example.fishing.ui.screens.report.create

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.example.fishing.model.Fish
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.CatchSearchState
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AUD-06: the catch form keeps its weight and the caller-provided fish list. */
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
    fun `weight and provided fish survive state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                CatchEditScreen(
                    fishList = listOf(
                        Fish(
                            id = UUID.randomUUID(),
                            name = "Щука",
                            count = 1,
                            speciesId = "esox_lucius"
                        )
                    ),
                    searchState = CatchSearchState(),
                    isAlreadyAdded = { _, _ -> false },
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchQueryChange = {},
                    onAddFish = { _, _ -> },
                    onAddCustomFish = {},
                    onToggleSuggestion = { _, _ -> },
                    onIncrementFish = {},
                    onDecrementFish = {},
                    onRemoveFish = {},
                    onWeightChange = {},
                    onBackClick = {},
                    onSaveClick = { _, _ -> }
                )
            }
        }

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("4.5")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Щука").assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("4.5").assertIsDisplayed()
        composeRule.onNodeWithText("Щука").assertIsDisplayed()
    }
}
