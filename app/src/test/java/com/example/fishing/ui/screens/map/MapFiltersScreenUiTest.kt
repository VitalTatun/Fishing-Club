package com.example.fishing.ui.screens.map

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishingMethod
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.filter.ReportFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class MapFiltersScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val species = listOf(
        FishSpecies(
            id = "esox_lucius",
            canonicalName = "Щука",
            aliases = listOf("щуки"),
            isPopular = true,
            sortOrder = 1
        ),
        FishSpecies(
            id = "perca_fluviatilis",
            canonicalName = "Окунь",
            isPopular = true,
            sortOrder = 2
        )
    )

    @Test
    fun `apply stays disabled until the draft changes`() {
        setContent()

        composeRule.onNodeWithTag("map_filters_apply").assertIsNotEnabled()

        composeRule.onNodeWithText("Мои отчёты").performClick()

        composeRule.onNodeWithTag("map_filters_apply").assertIsEnabled()
    }

    @Test
    fun `apply sends own-only and a fishing method`() {
        var applied: ReportFilters? = null
        setContent(onApply = { applied = it })

        composeRule.onNodeWithText("Мои отчёты").performClick()
        composeRule.onNodeWithText("Спиннинг").performClick()
        composeRule.onNodeWithTag("map_filters_apply").performClick()

        assertEquals(
            ReportFilters(
                isOwnOnly = true,
                selectedMethod = FishingMethod.SPINNING
            ),
            applied
        )
    }

    @Test
    fun `back without applying discards the draft`() {
        var applied: ReportFilters? = null
        var backPressed = false
        setContent(onApply = { applied = it }, onBack = { backPressed = true })

        composeRule.onNodeWithText("Трофей").performClick()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        assertTrue(backPressed)
        assertEquals(null, applied)
    }

    @Test
    fun `reset clears every chip and disables apply`() {
        setContent()

        composeRule.onNodeWithText("Платный водоем").performClick()
        composeRule.onNodeWithText("Платный водоем").assertIsSelected()
        composeRule.onNodeWithTag("map_filters_apply").assertIsEnabled()

        composeRule.onNodeWithTag("map_filters_reset").performClick()

        composeRule.onNodeWithText("Платный водоем").assertIsNotSelected()
        composeRule.onNodeWithTag("map_filters_apply").assertIsNotEnabled()
    }

    @Test
    fun `fish picker stores both canonical name and species id`() {
        var applied: ReportFilters? = null
        setContent(onApply = { applied = it })

        composeRule.onNodeWithTag("map_filter_fish_field").performClick()
        composeRule.onNodeWithTag("map_filter_fish_query").performTextInput("щу")
        composeRule.onNodeWithText("Щука").performClick()
        composeRule.onNodeWithTag("map_filters_apply").performClick()

        assertEquals("Щука", applied?.selectedCatch)
        assertEquals("esox_lucius", applied?.selectedSpeciesId)
    }

    private fun setContent(
        initialFilters: ReportFilters = ReportFilters(),
        onApply: (ReportFilters) -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                MapFiltersContent(
                    initialFilters = initialFilters,
                    species = species,
                    onApply = onApply,
                    onBack = onBack
                )
            }
        }
    }
}
