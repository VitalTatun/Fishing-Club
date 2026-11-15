package com.example.fishing.ui.screens.profile

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AUD-06, name synchronisation only.
 *
 * This test does NOT cover `rememberSaveable` restoration of `name` /
 * `selectedPhotoUri` or the `nameEdited` guard: driving the edit-name dialog
 * (which contains an `OutlinedTextField`) under Robolectric makes
 * `ShadowLineBreaker` allocate without bound and the test OOMs on dismiss, even
 * without typing. Those paths need a device/instrumentation test. The guard
 * logic itself is covered by the pure-function tests below.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class EditProfileNameSyncTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `initialName does not overwrite an edited name`() {
        assertNull(resolveNameSync(initialName = "Новое", currentName = "Старое", nameEdited = true))
    }

    @Test
    fun `initialName is applied while the user has not edited it`() {
        assertEquals(
            "Новое",
            resolveNameSync(initialName = "Новое", currentName = "Старое", nameEdited = false)
        )
    }

    @Test
    fun `unchanged initialName does not rewrite the field`() {
        assertNull(
            resolveNameSync(initialName = "Иван", currentName = "Иван", nameEdited = false)
        )
    }

    @Test
    fun `late initialName is shown in the screen until the name is edited`() {
        var initialName by mutableStateOf("Иван Рыбак")
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                EditProfileScreen(
                    initialName = initialName,
                    email = "ivan@example.com",
                    avatarUrl = null,
                    onBackClick = {},
                    onSaveClick = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Иван Рыбак").assertIsDisplayed()

        initialName = "Пётр Другой"
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Пётр Другой").assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Пётр Другой").assertIsDisplayed()
    }
}
