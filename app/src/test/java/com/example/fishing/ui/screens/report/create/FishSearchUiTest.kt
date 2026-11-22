package com.example.fishing.ui.screens.report.create

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishSuggestion
import com.example.fishing.model.FishSuggestionSource
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Assert.assertEquals
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
    qualifiers = "w411dp-h891dp"
)
class FishSearchUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pike = FishSpecies("esox_lucius", "Щука", listOf("щуки"), isPopular = true, sortOrder = 3)
    private val roach = FishSuggestion("Плотва", "rutilus_rutilus", FishSuggestionSource.HISTORY)

    @Test
    fun `collapsed search bar is clickable`() {
        var clicked = false
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                FishSearchBar(onClick = { clicked = true }, modifier = Modifier)
            }
        }

        composeRule.onNodeWithTag("fish_search_bar").performClick()

        assertTrue(clicked)
    }

    @Test
    fun `search view shows history and results and selects a species`() {
        var selectedName: String? = null
        var selectedId: String? = null

        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                FishSearchView(
                    query = "щу",
                    onQueryChange = {},
                    recent = listOf(roach),
                    popular = emptyList(),
                    results = listOf(pike),
                    isAlreadyAdded = { _, _ -> false },
                    onBack = {},
                    onSelect = { name, id ->
                        selectedName = name
                        selectedId = id
                    },
                    onAddCustom = {}
                )
            }
        }

        composeRule.onNodeWithText("Недавно добавляли").assertIsDisplayed()
        composeRule.onNodeWithText("Результат поиска").assertIsDisplayed()
        composeRule.onNodeWithText("Плотва").assertIsDisplayed()

        composeRule.onNodeWithTag("fish_suggestion_Щука").performClick()

        assertEquals("Щука", selectedName)
        assertEquals("esox_lucius", selectedId)
    }

    @Test
    fun `custom add is enabled only for a non blank query`() {
        var query by mutableStateOf("")
        var added = false

        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                FishSearchView(
                    query = query,
                    onQueryChange = { query = it },
                    recent = emptyList(),
                    popular = emptyList(),
                    results = emptyList(),
                    isAlreadyAdded = { _, _ -> false },
                    onBack = {},
                    onSelect = { _, _ -> },
                    onAddCustom = { added = true }
                )
            }
        }

        composeRule.onNodeWithTag("fish_search_add").assertIsNotEnabled()

        composeRule.onAllNodes(hasSetTextAction()).onFirst()
            .performTextInput("Щук")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("fish_search_add").assertIsEnabled().performClick()

        assertTrue(added)
    }

    @Test
    fun `already added species shows a subtitle and is not clickable`() {
        var selected = false

        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                FishSearchView(
                    query = "щу",
                    onQueryChange = {},
                    recent = emptyList(),
                    popular = emptyList(),
                    results = listOf(pike),
                    isAlreadyAdded = { name, _ -> name == "Щука" },
                    onBack = {},
                    onSelect = { _, _ -> selected = true },
                    onAddCustom = {}
                )
            }
        }

        composeRule.onNodeWithText("Уже есть в списке").assertIsDisplayed()
        composeRule.onNodeWithTag("fish_suggestion_Щука").assertIsNotEnabled()

        assertEquals(false, selected)
    }

    @Test
    fun `popular fallback replaces an empty history`() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                FishSearchView(
                    query = "",
                    onQueryChange = {},
                    recent = emptyList(),
                    popular = listOf(FishSuggestion("Плотва", "rutilus_rutilus", FishSuggestionSource.POPULAR)),
                    results = emptyList(),
                    isAlreadyAdded = { _, _ -> false },
                    onBack = {},
                    onSelect = { _, _ -> },
                    onAddCustom = {}
                )
            }
        }

        composeRule.onNodeWithText("Популярные виды").assertIsDisplayed()
        composeRule.onNodeWithText("Плотва").assertIsDisplayed()
    }
}
