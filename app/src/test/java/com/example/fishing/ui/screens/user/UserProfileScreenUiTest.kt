package com.example.fishing.ui.screens.user

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingPhoto
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.model.User
import com.example.fishing.model.Water
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.UserProfileUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/**
 * Compose behaviour tests for the public user screen. The screen is stateless,
 * so the states are fed in directly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class UserProfileScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var backClicks = 0
    private val sortOrders = mutableListOf<ReportSortOrder>()
    private var retries = 0
    private val openedReports = mutableListOf<UUID>()

    private companion object {
        const val AUTHOR_NAME = "Никита Белозерцев"
    }

    private fun report(
        id: UUID = UUID.randomUUID(),
        fishName: String = "Щука",
        waterName: String = "Озеро Нарочь"
    ): FishingReport = FishingReport(
        id = id,
        userId = UUID.randomUUID(),
        type = FishingType.FISHING_LOG,
        name = "Отчёт",
        water = Water(waterName = waterName, latitude = 55.0, longitude = 27.0),
        photos = listOf(FishingPhoto(id = UUID.randomUUID(), url = "https://example.com/photo.jpg")),
        fishingStartAt = Instant.parse("2026-08-12T09:00:00Z"),
        weight = 1.2,
        fish = listOf(Fish(id = UUID.randomUUID(), name = fishName, count = 2)),
        fishingMethod = FishingMethod.SPINNING,
        bait = emptyList(),
        comment = "",
        user = User(name = "Иван Рыбак", image = "", email = ""),
        fishingFromTheShore = true,
        isPublic = true
    )

    private fun setScreen(
        uiState: UserProfileUiState,
        sortOrder: ReportSortOrder = ReportSortOrder.BY_FISHING_TIME,
        canChangeSortOrder: Boolean = true
    ) {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                UserProfileScreen(
                    uiState = uiState,
                    sortOrder = sortOrder,
                    canChangeSortOrder = canChangeSortOrder,
                    onBackClick = { backClicks++ },
                    onSetSortOrder = { sortOrders += it },
                    onReportClick = { openedReports += it.id },
                    onRetry = { retries++ }
                )
            }
        }
    }

    private fun success(
        reports: List<FishingReport> = listOf(report())
    ) = UserProfileUiState.Success(
        user = User(name = "Никита Белозерцев", image = "", email = ""),
        reports = reports,
        caughtCount = 98,
        reportsCount = 25
    )

    @Test
    fun `header shows the author and aggregate stats`() {
        setScreen(success())

        composeRule.onNodeWithText("Никита Белозерцев").assertIsDisplayed()
        composeRule.onNodeWithText("Поймано").assertIsDisplayed()
        composeRule.onNodeWithText("98").assertIsDisplayed()
        composeRule.onNodeWithText("Всего отчетов").assertIsDisplayed()
        composeRule.onNodeWithText("25").assertIsDisplayed()
    }

    @Test
    fun `report card renders method, fish and water`() {
        setScreen(success())

        composeRule.onNodeWithText("Все отчеты").assertIsDisplayed()
        composeRule.onNodeWithText("Спиннинг • Щука").assertIsDisplayed()
        composeRule.onNodeWithText("Озеро Нарочь").assertIsDisplayed()
    }

    @Test
    fun `empty report list shows the placeholder`() {
        setScreen(success(reports = emptyList()))

        composeRule.onNodeWithText("Нет отчётов").assertIsDisplayed()
    }

    @Test
    fun `tapping a card opens that report`() {
        val report = report()
        setScreen(success(reports = listOf(report)))

        composeRule.onNodeWithText("Спиннинг • Щука").performClick()

        assertEquals(listOf(report.id), openedReports)
    }

    @Test
    fun `sort control opens a menu with both orders and reports the choice`() {
        setScreen(success())

        composeRule.onNodeWithText("По дате рыбалки").performClick()
        composeRule.onNodeWithText("По дате добавления").assertIsDisplayed()
        composeRule.onNodeWithText("По дате добавления").performClick()

        assertEquals(listOf(ReportSortOrder.BY_PUBLISH_DATE), sortOrders)
    }

    @Test
    fun `sort label follows the selected order`() {
        setScreen(success(), sortOrder = ReportSortOrder.BY_PUBLISH_DATE)

        composeRule.onNodeWithText("По дате добавления").assertIsDisplayed()
        composeRule.onNodeWithText("По дате рыбалки").assertDoesNotExist()
    }

    @Test
    fun `sort control does not open while photo urls are still signing`() {
        setScreen(success(), canChangeSortOrder = false)

        composeRule.onNodeWithText("По дате рыбалки").performClick()

        composeRule.onNodeWithText("По дате добавления").assertDoesNotExist()
        assertTrue(sortOrders.isEmpty())
    }

    @Test
    fun `collapsed app bar shows the author once the header scrolls away`() {
        setScreen(success(reports = List(10) { report() }))

        // At rest the name belongs to the scrolling page header.
        assertFalse(authorNameIsInAppBar())

        composeRule.onNode(hasScrollAction()).performScrollToIndex(2)
        composeRule.waitForIdle()

        // The page header is gone; the compact title in the app bar replaces it.
        composeRule.onNodeWithText(AUTHOR_NAME).assertExists()
        assertTrue(authorNameIsInAppBar())
    }

    @Test
    fun `scrolling back returns the name to the page header`() {
        setScreen(success(reports = List(10) { report() }))

        composeRule.onNode(hasScrollAction()).performScrollToIndex(2)
        composeRule.waitForIdle()
        assertTrue(authorNameIsInAppBar())

        composeRule.onNode(hasScrollAction()).performScrollToIndex(0)
        composeRule.waitForIdle()

        assertFalse(authorNameIsInAppBar())
    }

    /** The app bar name sits next to the back arrow, the page header one much lower. */
    private fun authorNameIsInAppBar(): Boolean {
        val nameBounds = composeRule.onNodeWithText(AUTHOR_NAME).fetchSemanticsNode().boundsInRoot
        val backBounds = composeRule
            .onNodeWithContentDescription("Назад")
            .fetchSemanticsNode()
            .boundsInRoot
        return nameBounds.center.y < backBounds.bottom
    }

    @Test
    fun `back arrow invokes its callback`() {
        setScreen(success())

        composeRule.onNodeWithContentDescription("Назад").performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun `error state offers a retry`() {
        setScreen(UserProfileUiState.Error(message = "Не удалось загрузить профиль"))

        composeRule.onNodeWithText("Не удалось загрузить профиль").assertIsDisplayed()
        composeRule.onNodeWithText("Повторить").performClick()

        assertEquals(1, retries)
    }
}
