package com.example.fishing.ui.screens.report.create

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fishing.model.Fish
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishSuggestion
import com.example.fishing.model.FishSuggestionSource
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.CatchSearchState
import java.util.UUID
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
class CatchEditScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pike = FishSpecies("esox_lucius", "Щука", listOf("щуки"), isPopular = true, sortOrder = 3)
    private val roach = FishSuggestion("Плотва", "rutilus_rutilus", FishSuggestionSource.HISTORY)

    private fun setContent(
        state: () -> CatchSearchState = { CatchSearchState() },
        fishList: List<Fish> = emptyList(),
        catalog: List<FishSpecies> = listOf(pike),
        initialWeight: Float = 0f,
        isTrophy: Boolean = false,
        onOpenSearch: () -> Unit = {},
        onCloseSearch: () -> Unit = {},
        onBackClick: () -> Unit = {},
        onSaveClick: (List<Fish>, Float) -> Unit = { _, _ -> }
    ) {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                CatchEditScreen(
                    fishList = fishList,
                    catalog = catalog,
                    searchState = state(),
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchQueryChange = {},
                    onBackClick = onBackClick,
                    onSaveClick = onSaveClick,
                    initialWeight = initialWeight,
                    isTrophy = isTrophy
                )
            }
        }
    }

    @Test
    fun `search bar opens the search`() {
        var opened = false
        setContent(onOpenSearch = { opened = true })

        composeRule.onNodeWithTag("fish_search_bar").performClick()

        assertTrue(opened)
    }

    @Test
    fun `open search replaces the normal content and back closes it`() {
        var closed = false
        setContent(
            state = { CatchSearchState(isOpen = true, query = "щу", results = listOf(pike)) },
            onCloseSearch = { closed = true }
        )

        composeRule.onNodeWithTag("fish_search_view").assertIsDisplayed()
        composeRule.onNodeWithTag("fish_search_input").assertIsDisplayed()
        composeRule.onNodeWithText("Рыба, которую вы уже указывали в отчетах").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Назад").performClick()

        assertTrue(closed)
    }

    @Test
    fun `selecting a result adds it to the local draft and closes the search`() {
        var open by mutableStateOf(true)
        var closes = 0
        setContent(
            state = { CatchSearchState(isOpen = open, query = "щу", results = listOf(pike)) },
            onCloseSearch = {
                open = false
                closes++
            }
        )

        composeRule.onNodeWithTag("fish_suggestion_Щука").performClick()
        composeRule.waitForIdle()

        assertEquals(1, closes)
        composeRule.onNodeWithTag("fish_search_view").assertDoesNotExist()
        composeRule.onNodeWithText("Щука").assertIsDisplayed()
        composeRule.onNodeWithText("1 шт.").assertIsDisplayed()
    }

    @Test
    fun `custom add creates a local fish and closes the search`() {
        var open by mutableStateOf(true)
        setContent(
            state = { CatchSearchState(isOpen = open, query = "Пескарик") },
            onCloseSearch = { open = false }
        )

        composeRule.onNodeWithTag("fish_search_add").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Пескарик").assertIsDisplayed()
        composeRule.onNodeWithText("1 шт.").assertIsDisplayed()
    }

    @Test
    fun `chips toggle the local draft`() {
        setContent(
            state = { CatchSearchState(chips = listOf(roach)) }
        )

        composeRule.onNodeWithTag("fish_chip_Плотва").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("fish_chip_Плотва").assertIsSelected()
        composeRule.onNodeWithText("1 шт.").assertIsDisplayed()

        composeRule.onNodeWithTag("fish_chip_Плотва").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("fish_chip_Плотва").assertIsNotSelected()
        composeRule.onNodeWithText("1 шт.").assertDoesNotExist()
    }

    @Test
    fun `steppers change counts and remove rows locally`() {
        val carp = Fish(UUID.randomUUID(), "Карась", count = 5, speciesId = "carassius_gibelio")
        val perch = Fish(UUID.randomUUID(), "Окунь", count = 1, speciesId = "perca_fluviatilis")
        setContent(fishList = listOf(carp, perch))

        composeRule.onAllNodesWithTag("quantity_increment")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("6 шт.").assertIsDisplayed()

        composeRule.onAllNodesWithTag("quantity_decrement")[1].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Окунь").assertDoesNotExist()
    }

    @Test
    fun `save passes the local draft and weight`() {
        val fish = listOf(Fish(UUID.randomUUID(), "Щука", count = 1, speciesId = "esox_lucius"))
        var savedFish: List<Fish>? = null
        var savedWeight: Float? = null
        setContent(
            fishList = fish,
            initialWeight = 2.5f,
            onSaveClick = { list, weight ->
                savedFish = list
                savedWeight = weight
            }
        )

        composeRule.onNodeWithContentDescription("Сохранить").assertIsEnabled().performClick()

        assertEquals(fish, savedFish)
        assertEquals(2.5f, savedWeight)
    }

    @Test
    fun `save includes a fish added from a chip`() {
        var savedFish: List<Fish>? = null
        setContent(
            state = { CatchSearchState(chips = listOf(roach)) },
            onSaveClick = { list, _ -> savedFish = list }
        )

        composeRule.onNodeWithTag("fish_chip_Плотва").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Сохранить").performClick()

        assertEquals("Плотва", savedFish?.single()?.name)
    }

    @Test
    fun `save is disabled without fish`() {
        setContent(fishList = emptyList())

        composeRule.onNodeWithContentDescription("Сохранить").assertIsNotEnabled()
    }

    @Test
    fun `back leaves the screen without saving`() {
        var backs = 0
        var saves = 0
        setContent(
            state = { CatchSearchState(chips = listOf(roach)) },
            onBackClick = { backs++ },
            onSaveClick = { _, _ -> saves++ }
        )

        composeRule.onNodeWithTag("fish_chip_Плотва").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        assertEquals(1, backs)
        assertEquals(0, saves)
    }

    @Test
    fun `trophy shows the hint and disables increment`() {
        val fish = listOf(Fish(UUID.randomUUID(), "Щука", count = 1, speciesId = "esox_lucius"))
        setContent(fishList = fish, isTrophy = true)

        composeRule.onNodeWithText("Вы выбрали тип рыбалки «Трофей»", substring = true)
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag("quantity_increment")[0].assertIsNotEnabled()
    }
}
