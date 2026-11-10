package com.example.fishing.ui.screens.profile

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AUD-03: the account actions on the profile settings screen must be real
 * actions, not no-op buttons.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class EditProfileScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var passwordClicks = 0
    private var deleteCalls = 0

    private fun setScreen(isDeletingAccount: Boolean = false) {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                EditProfileScreen(
                    initialName = "Иван Рыбак",
                    email = "ivan@example.com",
                    avatarUrl = null,
                    isDeletingAccount = isDeletingAccount,
                    onBackClick = {},
                    onSaveClick = { _, _ -> },
                    onChangePasswordClick = { passwordClicks++ },
                    onDeleteAccount = { deleteCalls++ }
                )
            }
        }
    }

    @Test
    fun `password action opens the dedicated screen`() {
        setScreen()

        composeRule.onNodeWithText("Изменить пароль").performClick()
        composeRule.waitForIdle()

        assertEquals(1, passwordClicks)
    }

    @Test
    fun `delete account opens a confirmation dialog`() {
        setScreen()

        composeRule.onNodeWithText("Удалить аккаунт").performClick()

        composeRule.onNodeWithText(
            "Аккаунт и все отчёты будут удалены безвозвратно. " +
                "Это действие нельзя отменить."
        ).assertIsDisplayed()
    }

    @Test
    fun `delete confirmation invokes account deletion`() {
        setScreen()

        composeRule.onNodeWithText("Удалить аккаунт").performClick()
        composeRule.onNodeWithText("Удалить").performClick()
        composeRule.waitForIdle()

        assertEquals(1, deleteCalls)
    }

    @Test
    fun `delete confirmation is disabled while deletion is in flight`() {
        setScreen(isDeletingAccount = true)

        composeRule.onNodeWithText("Удалить аккаунт").performClick()
        composeRule.onNodeWithText("Удалить").assertIsNotEnabled()
    }
}
