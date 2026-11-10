package com.example.fishing.ui.screens.profile

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.mutableStateOf
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AUD-03: the dedicated change-password screen. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class ChangePasswordScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var submits = 0
    private var backClicks = 0
    private val newPassword = mutableStateOf("")
    private val confirmPassword = mutableStateOf("")

    private fun setContent(
        isLoading: Boolean = false,
        error: String? = null
    ) {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                ChangePasswordContent(
                    newPassword = newPassword.value,
                    confirmPassword = confirmPassword.value,
                    isLoading = isLoading,
                    error = error,
                    onNewPasswordChange = { newPassword.value = it },
                    onConfirmPasswordChange = { confirmPassword.value = it },
                    onBackClick = { backClicks++ },
                    onSubmit = { submits++ }
                )
            }
        }
    }

    private fun field(index: Int) = composeRule.onAllNodes(hasSetTextAction())[index]

    @Test
    fun `screen displays both password fields`() {
        setContent()

        composeRule.onNodeWithText("Новый пароль").assertIsDisplayed()
        composeRule.onNodeWithText("Повторите пароль").assertIsDisplayed()
        composeRule.onNodeWithText("Сохранить").assertIsNotEnabled()
    }

    @Test
    fun `save submits the typed passwords`() {
        setContent()

        field(0).performTextInput("secret1")
        field(1).performTextInput("secret1")
        composeRule.onNodeWithText("Сохранить").performClick()
        composeRule.waitForIdle()

        assertEquals("secret1", newPassword.value)
        assertEquals("secret1", confirmPassword.value)
        assertEquals(1, submits)
    }

    @Test
    fun `validation error is displayed`() {
        setContent(error = "Пароли не совпадают")

        composeRule.onNodeWithText("Пароли не совпадают").assertIsDisplayed()
    }

    @Test
    fun `back is disabled while the request is in flight`() {
        setContent(isLoading = true)

        composeRule.onNodeWithContentDescription("Назад").assertIsNotEnabled()
        assertEquals(0, backClicks)
    }
}
