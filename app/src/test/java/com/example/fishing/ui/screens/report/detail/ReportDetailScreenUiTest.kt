package com.example.fishing.ui.screens.report.detail

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.ReportLikeState
import com.example.fishing.model.User
import com.example.fishing.model.Water
import com.example.fishing.ui.theme.FishingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Compose UI behaviour tests for [ReportDetailScreen] and its loading/error
 * variants. The screen is stateless, so no repository or Supabase is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class ReportDetailScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var backClicks = 0
    private var favoriteToggles = 0
    private var deleteCalls = 0
    private var likeToggles = 0
    private var userClicks = 0
    private var visibilityCalls = mutableListOf<Boolean>()

    private fun report(
        isPublic: Boolean = false,
        type: FishingType = FishingType.FISHING_LOG
    ): FishingReport {
        val authorId = UUID.randomUUID()
        return FishingReport(
            id = UUID.randomUUID(),
            userId = authorId,
            type = type,
            name = "Отчёт",
            water = Water(
                waterName = "Озеро Нарочь",
                latitude = 54.9,
                longitude = 26.7,
                isPaid = false
            ),
            photos = emptyList(),
            fishingStartAt = Instant.parse("2026-05-01T06:00:00Z"),
            fishingEndAt = Instant.parse("2026-05-01T09:00:00Z"),
            weight = 3.2,
            fish = listOf(Fish(id = UUID.randomUUID(), name = "Щука", count = 2)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Проверенный комментарий",
            user = User(
                id = authorId,
                name = "Иван Рыбак",
                email = "ivan@example.com",
                image = ""
            ),
            fishingFromTheShore = true,
            isPublic = isPublic,
            publishedAt = Date()
        )
    }

    private fun setDetailScreen(
        report: FishingReport = report(),
        isFavorite: Boolean = false,
        isOwnReport: Boolean = true,
        isDeleting: Boolean = false,
        isUpdatingVisibility: Boolean = false,
        visibilityResult: String? = null,
        likeState: ReportLikeState? = null,
        onUserClick: (() -> Unit)? = { userClicks++ }
    ) {
        composeRule.setContent {
            // Skip the osmdroid AndroidView map: the report data and actions are
            // what this test verifies.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                FishingTheme(darkTheme = false, dynamicColor = false) {
                    ReportDetailScreen(
                        report = report,
                        onBackClick = { backClicks++ },
                        isFavorite = isFavorite,
                        onToggleFavorite = { favoriteToggles++ },
                        isOwnReport = isOwnReport,
                        onDeleteReport = { deleteCalls++ },
                        isDeleting = isDeleting,
                        isUpdatingVisibility = isUpdatingVisibility,
                        onToggleVisibility = { visibilityCalls += it },
                        visibilityResult = visibilityResult,
                        likeState = likeState,
                        onToggleLike = { likeToggles++ },
                        onUserClick = onUserClick
                    )
                }
            }
        }
    }

    @Test
    fun `author row opens the user page when navigation is allowed`() {
        setDetailScreen()

        composeRule.onNodeWithText("Иван Рыбак").performClick()

        assertEquals(1, userClicks)
    }

    @Test
    fun `author row is not clickable when navigation is disabled`() {
        // Details opened from a user page pass null: profile → report → profile
        // must not be able to stack.
        setDetailScreen(onUserClick = null)

        composeRule.onNodeWithText("Иван Рыбак").performClick()

        assertEquals(0, userClicks)
    }

    @Test
    fun `report detail displays main data and draft state`() {
        setDetailScreen()

        composeRule.onNodeWithText("Щука • Спиннинг").assertIsDisplayed()
        composeRule.onNodeWithText("Иван Рыбак").assertIsDisplayed()
        composeRule.onNodeWithText("Озеро Нарочь").assertIsDisplayed()
        composeRule.onNodeWithText("Проверенный комментарий").assertIsDisplayed()
        composeRule.onNodeWithText("Не опубликован").assertIsDisplayed()
        composeRule.onNodeWithText("Отчет не опубликован").assertIsDisplayed()
        composeRule.onNodeWithText("3,2 кг.").assertExists()
        composeRule.onNodeWithText("Щука — 2 шт.").assertExists()
        composeRule.onNodeWithText("Ловля с берега").assertExists()
        composeRule.onNodeWithText("Дата").assertExists()
        composeRule.onNodeWithText("Время").assertExists()
        composeRule.onNodeWithText("Длительность").assertExists()
    }

    @Test
    fun `published report does not show draft banner`() {
        setDetailScreen(report = report(isPublic = true))

        composeRule.onNodeWithText("Не опубликован").assertDoesNotExist()
        composeRule.onNodeWithText("Отчет не опубликован").assertDoesNotExist()
    }

    @Test
    fun `toolbar and menu actions invoke their callbacks`() {
        setDetailScreen(isFavorite = false, isOwnReport = true)

        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.onNodeWithContentDescription("Добавить в избранное").performClick()
        composeRule.onNodeWithContentDescription("Меню").performClick()

        composeRule.onNodeWithText("Редактировать").assertDoesNotExist()
        composeRule.onNodeWithText("Сделать публичным").performClick()
        composeRule.waitForIdle()

        // The action is confirmed in a dialog before anything is sent.
        composeRule.onNodeWithText("Сделать отчёт публичным?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Отчёт станет доступен всем пользователям и появится на карте уловов."
        ).assertIsDisplayed()
        assertTrue(visibilityCalls.isEmpty())

        composeRule.onNodeWithText("Опубликовать").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(true), visibilityCalls)
        composeRule.onNodeWithText("Сделать отчёт публичным?").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Меню").performClick()
        composeRule.onNodeWithText("Удалить").performClick()
        composeRule.onNodeWithText("Удалить отчет").assertIsDisplayed()
        composeRule.onNodeWithText("Вы уверены, что хотите удалить этот отчет?")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Удалить").performClick()
        composeRule.waitForIdle()

        assertEquals(1, backClicks)
        assertEquals(1, favoriteToggles)
        assertEquals(1, deleteCalls)
    }

    @Test
    fun `favorite action removes an existing favorite`() {
        setDetailScreen(isFavorite = true)

        composeRule.onNodeWithContentDescription("Убрать из избранного").performClick()
        composeRule.waitForIdle()

        assertEquals(1, favoriteToggles)
    }

    @Test
    fun `non owner does not see private or delete actions`() {
        setDetailScreen(isOwnReport = false)

        composeRule.onNodeWithContentDescription("Меню").performClick()

        composeRule.onNodeWithText("Редактировать").assertDoesNotExist()
        composeRule.onNodeWithText("Сделать публичным").assertDoesNotExist()
        composeRule.onNodeWithText("Сделать приватным").assertDoesNotExist()
        composeRule.onNodeWithText("Удалить").assertDoesNotExist()
    }

    @Test
    fun `visibility action is disabled while the change is in flight`() {
        setDetailScreen(report = report(isPublic = true), isUpdatingVisibility = true)

        composeRule.onNodeWithContentDescription("Меню").performClick()

        composeRule.onNodeWithText("Сделать приватным").assertIsNotEnabled()
    }

    @Test
    fun `visibility action targets the opposite of the current state`() {
        setDetailScreen(report = report(isPublic = true))

        composeRule.onNodeWithContentDescription("Меню").performClick()
        composeRule.onNodeWithText("Сделать приватным").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Сделать отчёт приватным?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Отчёт станет виден только вам: он исчезнет из поиска и с карты у других пользователей."
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Скрыть").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(false), visibilityCalls)
    }

    @Test
    fun `cancelling the visibility dialog sends nothing`() {
        setDetailScreen(report = report(isPublic = false))

        composeRule.onNodeWithContentDescription("Меню").performClick()
        composeRule.onNodeWithText("Сделать публичным").performClick()
        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.waitForIdle()

        assertTrue(visibilityCalls.isEmpty())
        composeRule.onNodeWithText("Сделать отчёт публичным?").assertDoesNotExist()
    }

    @Test
    fun `visibility result is shown in a snackbar`() {
        setDetailScreen(visibilityResult = "Отчёт опубликован")

        composeRule.onNodeWithText("Отчёт опубликован").assertIsDisplayed()
    }

    @Test
    fun `like action invokes callback`() {
        val report = report(isPublic = true)
        setDetailScreen(
            report = report,
            likeState = ReportLikeState(
                reportId = report.id,
                isLiked = false,
                likesCount = 3
            )
        )

        composeRule.onNodeWithText("3").performClick()
        composeRule.waitForIdle()

        assertEquals(1, likeToggles)
    }

    @Test
    fun `deleting state hides the menu action`() {
        setDetailScreen(isDeleting = true)

        composeRule.onNodeWithContentDescription("Меню").assertDoesNotExist()
    }

    @Test
    fun `loading screen displays progress and handles back`() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                ReportDetailLoadingScreen(onBackClick = { backClicks++ })
            }
        }

        composeRule.onNodeWithText("Загрузка").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitForIdle()

        assertEquals(1, backClicks)
    }

    @Test
    fun `unavailable screen displays the deletion message`() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                ReportDetailUnavailableScreen(onBackClick = { backClicks++ })
            }
        }

        composeRule.onAllNodesWithText("Этот отчёт был удалён или стал приватным")
            .assertCountEquals(2)
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.waitForIdle()

        assertEquals(1, backClicks)
    }

    @Test
    fun `error screen displays the message and retries`() {
        var retries = 0
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                ReportDetailErrorScreen(
                    message = "Нет соединения с интернетом",
                    onBackClick = { backClicks++ },
                    onRetry = { retries++ }
                )
            }
        }

        composeRule.onNodeWithText("Не удалось загрузить отчёт").assertIsDisplayed()
        composeRule.onNodeWithText("Нет соединения с интернетом").assertIsDisplayed()
        composeRule.onNodeWithText("Повторить").performClick()
        composeRule.waitForIdle()

        assertEquals(1, retries)
        assertEquals(0, backClicks)
    }
}
