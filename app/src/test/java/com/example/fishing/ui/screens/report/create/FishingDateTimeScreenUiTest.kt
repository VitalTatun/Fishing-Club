package com.example.fishing.ui.screens.report.create

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "ru-rRU-w411dp-h891dp",
)
class FishingDateTimeScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingDateAndTimeFieldsOpensTheirPickers() {
        composeRule.setContent {
            FishingTheme(dynamicColor = false) {
                FishingDateTimeScreen()
            }
        }

        val startDateField = composeRule.onAllNodesWithText("Не указан").get(0)
        startDateField.assertIsEnabled()
        startDateField.performTouchInput { click() }
        composeRule.onNodeWithText("OK").assertIsDisplayed()
        composeRule.onNodeWithText("Отмена").performClick()

        composeRule.onAllNodesWithText("Начало")
            .get(1)
            .performTouchInput { click() }
        composeRule.onNodeWithText("OK").assertIsDisplayed()
    }
}
