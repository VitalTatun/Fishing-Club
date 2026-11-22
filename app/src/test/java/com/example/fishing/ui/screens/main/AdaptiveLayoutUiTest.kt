package com.example.fishing.ui.screens.main

import android.app.Application
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.fishing.model.Fish
import com.example.fishing.testutil.testReport
import com.example.fishing.ui.screens.login.LoginContent
import com.example.fishing.ui.screens.report.create.CatchEditScreen
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.CatchSearchState
import com.example.fishing.viewmodel.HomeUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class CompactAdaptiveLayoutUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `compact window uses horizontal navigation bar`() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                MainShell(homeUiState = HomeUiState.Loading)
            }
        }

        val home = navigationItemBounds("Главная")
        val map = navigationItemBounds("Карта")
        val profile = navigationItemBounds("Профиль")

        assertTrue(abs(home.center.y - map.center.y) < 24f)
        assertTrue(abs(home.center.y - profile.center.y) < 24f)
        assertTrue(abs(home.center.x - map.center.x) > abs(home.center.y - map.center.y))
    }

    @Test
    fun `adaptive home grid keeps one column in compact width`() {
        setHomeFeed()

        val first = composeRule.onNodeWithText("Adaptive lake 1").fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithText("Adaptive lake 2").fetchSemanticsNode().boundsInRoot

        assertTrue(abs(first.center.x - second.center.x) < 2f)
        assertTrue(second.center.y > first.center.y)
    }

    private fun navigationItemBounds(label: String): Rect {
        val node = composeRule.onNodeWithContentDescription(
            label,
            useUnmergedTree = true
        )
        node.assertIsDisplayed()
        return node.fetchSemanticsNode().boundsInRoot
    }

    private fun setHomeFeed() {
        val ownerId = UUID.randomUUID()
        val reports = (1..4).map { index ->
            testReport(
                userId = ownerId,
                name = "Adaptive report $index",
                waterName = "Adaptive lake $index",
                fish = listOf(
                    Fish(
                        id = UUID.randomUUID(),
                        name = "Adaptive fish $index",
                        count = 1
                    )
                )
            )
        }
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                MainShell(
                    homeUiState = HomeUiState.Success(reports),
                    currentUserId = ownerId,
                )
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w840dp-h900dp"
)
class ExpandedAdaptiveLayoutUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `expanded window uses vertical navigation rail`() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                MainShell(homeUiState = HomeUiState.Loading)
            }
        }

        val home = navigationItemBounds("Главная")
        val map = navigationItemBounds("Карта")
        val profile = navigationItemBounds("Профиль")

        assertTrue(abs(home.center.x - map.center.x) < 24f)
        assertTrue(abs(home.center.x - profile.center.x) < 24f)
        assertTrue(abs(home.center.y - map.center.y) > abs(home.center.x - map.center.x))
    }

    @Test
    fun `adaptive home grid shows two columns in expanded width`() {
        val ownerId = UUID.randomUUID()
        val reports = (1..4).map { index ->
            testReport(
                userId = ownerId,
                name = "Adaptive report $index",
                waterName = "Adaptive lake $index",
                fish = listOf(
                    Fish(
                        id = UUID.randomUUID(),
                        name = "Adaptive fish $index",
                        count = 1
                    )
                )
            )
        }

        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                MainShell(
                    homeUiState = HomeUiState.Success(reports),
                    currentUserId = ownerId
                )
            }
        }

        val first = composeRule.onNodeWithText("Adaptive lake 1").fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithText("Adaptive lake 2").fetchSemanticsNode().boundsInRoot

        assertTrue(abs(first.center.y - second.center.y) < 24f)
        assertTrue(second.center.x > first.center.x)
    }

    @Test
    fun `authentication content is bounded and centered`() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                LoginContent(
                    email = "",
                    password = "",
                    isLoading = false,
                    error = null,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onLoginClick = {},
                    onRegisterClick = {}
                )
            }
        }

        val email = composeRule.onAllNodes(hasSetTextAction())[0]
            .fetchSemanticsNode().boundsInRoot
        val rootWidth = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.width
        val authMaxWidth = with(composeRule.density) { 480.dp.toPx() }

        assertTrue(email.width <= authMaxWidth)
        assertTrue(abs(email.center.x - rootWidth / 2f) < 2f)
    }

    @Test
    fun `report form content is bounded in expanded width`() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                CatchEditScreen(
                    fishList = emptyList(),
                    catalog = emptyList(),
                    searchState = CatchSearchState(),
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchQueryChange = {},
                    onBackClick = {},
                    onSaveClick = { _, _ -> }
                )
            }
        }

        val fishNameField = composeRule.onAllNodes(hasSetTextAction())[0]
            .fetchSemanticsNode().boundsInRoot
        val formMaxWidth = with(composeRule.density) { 640.dp.toPx() }
        assertTrue(fishNameField.width <= formMaxWidth)
    }

    private fun navigationItemBounds(label: String): Rect {
        val node = composeRule.onNodeWithContentDescription(
            label,
            useUnmergedTree = true
        )
        node.assertIsDisplayed()
        return node.fetchSemanticsNode().boundsInRoot
    }
}
