package com.example.fishing.ui.screens.search

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.LocationSearchUiState
import org.junit.Assert.assertEquals
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
class LocationSearchScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `search error offers retry`() {
        var retryCount = 0
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                LocationSearchScreen(
                    state = LocationSearchUiState(query = "Минск", hasError = true),
                    onQueryChange = {},
                    onRetry = { retryCount++ },
                    onLocationSelected = { _, _, _ -> },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText(
            "Не удалось выполнить поиск. Проверьте подключение и попробуйте ещё раз."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Повторить").performClick()

        assertEquals(1, retryCount)
    }
}
