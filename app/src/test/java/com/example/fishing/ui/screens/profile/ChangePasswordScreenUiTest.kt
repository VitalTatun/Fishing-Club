package com.example.fishing.ui.screens.profile

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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
    private val currentPassword = mutableStateOf("")
    private val newPassword = mutableStateOf("")
    private val confirmPassword = mutableStateOf("")
    private val currentPasswordVisible = mutableStateOf(false)
    private val newPasswordVisible = mutableStateOf(false)
    private val confirmPasswordVisible = mutableStateOf(false)

    private fun setContent(
        isLoading: Boolean = false,
        error: String? = null
    ) {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                ChangePasswordContent(
                    currentPassword = currentPassword.value,
                    newPassword = newPassword.value,
                    confirmPassword = confirmPassword.value,
                    isCurrentPasswordVisible = currentPasswordVisible.value,
                    isNewPasswordVisible = newPasswordVisible.value,
                    isConfirmPasswordVisible = confirmPasswordVisible.value,
                    isLoading = isLoading,
                    error = error,
                    onCurrentPasswordChange = { currentPassword.value = it },
                    onNewPasswordChange = { newPassword.value = it },
                    onConfirmPasswordChange = { confirmPassword.value = it },
                    onToggleCurrentPasswordVisibility = {
                        currentPasswordVisible.value = !currentPasswordVisible.value
                    },
                    onToggleNewPasswordVisibility = {
                        newPasswordVisible.value = !newPasswordVisible.value
                    },
                    onToggleConfirmPasswordVisibility = {
                        confirmPasswordVisible.value = !confirmPasswordVisible.value
                    },
                    onBackClick = { backClicks++ },
                    onSubmit = { submits++ }
                )
            }
        }
    }

    private fun field(index: Int) = composeRule.onAllNodes(hasSetTextAction())[index]

    private fun fillValidForm() {
        field(0).performTextInput("Qwerty1234")
        field(1).performTextInput("Abcdefgh12")
        field(2).performTextInput("Abcdefgh12")
    }

    @Test
    fun `screen displays the password fields and the password rules`() {
        setContent()

        composeRule.onNodeWithText("Текущий пароль").assertIsDisplayed()
        composeRule.onNodeWithText("Новый пароль").assertIsDisplayed()
        composeRule.onNodeWithText("Повторите пароль").assertIsDisplayed()
        composeRule.onNodeWithText("Пароль должен содержать:").assertIsDisplayed()
        composeRule.onNodeWithText("Минимум 10 символов").assertIsDisplayed()
        composeRule.onNodeWithText("Сохранить").assertIsNotEnabled()
    }

    @Test
    fun `save stays disabled until the password meets the policy`() {
        setContent()

        field(0).performTextInput("Qwerty1234")
        field(1).performTextInput("abcdefghij")
        field(2).performTextInput("abcdefghij")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Сохранить").assertIsNotEnabled()

        field(1).performTextReplacement("Abcdefgh12")
        field(2).performTextReplacement("Abcdefgh12")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Сохранить").assertIsEnabled()
    }

    @Test
    fun `rules reflect the typed password`() {
        setContent()

        // Ten lowercase letters: length and lowercase pass, uppercase and digit do not.
        field(1).performTextInput("abcdefghij")
        composeRule.waitForIdle()

        composeRule.onAllNodesWithContentDescription("Выполнено").assertCountEquals(2)
        composeRule.onAllNodesWithContentDescription("Не выполнено").assertCountEquals(2)
    }

    @Test
    fun `save submits all three passwords`() {
        setContent()

        fillValidForm()
        composeRule.onNodeWithText("Сохранить").performClick()
        composeRule.waitForIdle()

        assertEquals("Qwerty1234", currentPassword.value)
        assertEquals("Abcdefgh12", newPassword.value)
        assertEquals("Abcdefgh12", confirmPassword.value)
        assertEquals(1, submits)
    }

    @Test
    fun `password visibility can be toggled`() {
        setContent()

        composeRule.onAllNodesWithContentDescription("Показать пароль")[0].performClick()
        composeRule.waitForIdle()

        assertEquals(true, currentPasswordVisible.value)
        composeRule.onAllNodesWithContentDescription("Скрыть пароль").assertCountEquals(1)
    }

    @Test
    fun `validation error is displayed`() {
        setContent(error = "Неверный текущий пароль")

        composeRule.onNodeWithText("Неверный текущий пароль").assertIsDisplayed()
    }

    @Test
    fun `back is disabled while the request is in flight`() {
        setContent(isLoading = true)

        composeRule.onNodeWithContentDescription("Назад").assertIsNotEnabled()
        assertEquals(0, backClicks)
    }
}
