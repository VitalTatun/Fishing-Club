package com.example.fishing.ui.screens.report.create

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
        state: CatchSearchState = CatchSearchState(),
        fishList: List<Fish> = emptyList(),
        initialWeight: Float = 0f,
        isTrophy: Boolean = false,
        isAlreadyAdded: (name: String, speciesId: String?) -> Boolean = { _, _ -> false },
        onOpenSearch: () -> Unit = {},
        onCloseSearch: () -> Unit = {},
        onAddFish: (name: String, speciesId: String?) -> Unit = { _, _ -> },
        onAddCustomFish: (name: String) -> Unit = {},
        onToggleSuggestion: (name: String, speciesId: String?) -> Unit = { _, _ -> },
        onIncrementFish: (UUID) -> Unit = {},
        onDecrementFish: (UUID) -> Unit = {},
        onRemoveFish: (UUID) -> Unit = {},
        onBackClick: () -> Unit = {},
        onSaveClick: (List<Fish>, Float) -> Unit = { _, _ -> }
    ) {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                CatchEditScreen(
                    fishList = fishList,
                    searchState = state,
                    isAlreadyAdded = isAlreadyAdded,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchQueryChange = {},
                    onAddFish = onAddFish,
                    onAddCustomFish = onAddCustomFish,
                    onToggleSuggestion = onToggleSuggestion,
                    onIncrementFish = onIncrementFish,
                    onDecrementFish = onDecrementFish,
                    onRemoveFish = onRemoveFish,
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
            state = CatchSearchState(isOpen = true, query = "щу", results = listOf(pike)),
            onCloseSearch = { closed = true }
        )

        composeRule.onNodeWithTag("fish_search_view").assertIsDisplayed()
        composeRule.onNodeWithTag("fish_search_input").assertIsDisplayed()
        composeRule.onNodeWithText("Рыба, которую вы уже указывали в отчетах").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Назад").performClick()

        assertTrue(closed)
    }

    @Test
    fun `selecting a result forwards the name and species id`() {
        var selectedName: String? = null
        var selectedId: String? = null
        setContent(
            state = CatchSearchState(isOpen = true, query = "щу", results = listOf(pike)),
            onAddFish = { name, id ->
                selectedName = name
                selectedId = id
            }
        )

        composeRule.onNodeWithTag("fish_suggestion_Щука").performClick()

        assertEquals("Щука", selectedName)
        assertEquals("esox_lucius", selectedId)
    }

    @Test
    fun `custom add is available for a non blank query`() {
        var added: String? = null
        setContent(
            state = CatchSearchState(isOpen = true, query = "Пескарик"),
            onAddCustomFish = { added = it }
        )

        composeRule.onNodeWithTag("fish_search_add").assertIsEnabled().performClick()

        assertEquals("Пескарик", added)
    }

    @Test
    fun `chips forward toggles and reflect the already added state`() {
        var toggled: Pair<String, String?>? = null
        var added by mutableStateOf(false)
        setContent(
            state = CatchSearchState(chips = listOf(roach)),
            isAlreadyAdded = { _, _ -> added },
            onToggleSuggestion = { name, id -> toggled = name to id }
        )

        composeRule.onNodeWithTag("fish_chip_Плотва").performClick()

        assertEquals("Плотва" to "rutilus_rutilus", toggled)

        added = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fish_chip_Плотва").assertIsSelected()
    }

    @Test
    fun `added fish show the count and forward stepper actions`() {
        val carp = Fish(UUID.randomUUID(), "Карась", count = 5, speciesId = "carassius_gibelio")
        val perch = Fish(UUID.randomUUID(), "Окунь", count = 1, speciesId = "perca_fluviatilis")
        val incremented = mutableListOf<UUID>()
        val decremented = mutableListOf<UUID>()
        val removed = mutableListOf<UUID>()

        setContent(
            fishList = listOf(carp, perch),
            onIncrementFish = { incremented += it },
            onDecrementFish = { decremented += it },
            onRemoveFish = { removed += it }
        )

        composeRule.onNodeWithText("5 шт.").assertIsDisplayed()
        composeRule.onAllNodesWithTag("quantity_increment")[0].performClick()
        composeRule.onAllNodesWithTag("quantity_decrement")[1].performClick()
        composeRule.onAllNodesWithTag("quantity_remove")[0].performClick()

        assertEquals(listOf(carp.id), incremented)
        assertEquals(listOf(perch.id), decremented)
        assertEquals(listOf(carp.id), removed)
    }

    @Test
    fun `save is disabled without fish`() {
        setContent(fishList = emptyList())

        composeRule.onNodeWithContentDescription("Сохранить").assertIsNotEnabled()
    }

    @Test
    fun `save passes the list and weight`() {
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
    fun `trophy shows the hint and disables increment`() {
        val fish = listOf(Fish(UUID.randomUUID(), "Щука", count = 1, speciesId = "esox_lucius"))
        setContent(fishList = fish, isTrophy = true)

        composeRule.onNodeWithText("Вы выбрали тип рыбалки «Трофей»", substring = true)
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag("quantity_increment")[0].assertIsNotEnabled()
    }

    @Test
    fun `normal app bar back invokes the callback`() {
        var backs = 0
        setContent(onBackClick = { backs++ })

        composeRule.onNodeWithContentDescription("Назад").performClick()

        assertEquals(1, backs)
    }
}
