package com.example.fishing.ui.screens.report.create

import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.ReportUploadState
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.FakeReportUploadQueue
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.CreateReportSaveState
import com.example.fishing.viewmodel.CreateReportViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers

/**
 * Compose UI behaviour tests for [CreateReportScreen].
 *
 * The screen is driven by a real [CreateReportViewModel] wired to in-memory
 * fakes, so the form steps, validation and upload progress are exercised
 * without Supabase, Room or WorkManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class CreateReportScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var fishingRepository: FakeFishingRepository
    private lateinit var uploadQueue: FakeReportUploadQueue
    private lateinit var viewModel: CreateReportViewModel

    private var backClicks = 0
    private var saveCompleteCalls = 0
    private var catchEditClicks = 0
    private var methodEditClicks = 0
    private var commentEditClicks = 0
    private var waterEditClicks = 0
    private var waterNameEditClicks = 0

    private val testUser = User(
        id = UUID.randomUUID(),
        name = "Test User",
        email = "test@example.com",
        image = ""
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        authRepository = FakeAuthRepository().apply { sessionUser = testUser }
        fishingRepository = FakeFishingRepository()
        uploadQueue = FakeReportUploadQueue()
        viewModel = CreateReportViewModel(
            repository = fishingRepository,
            authRepository = authRepository,
            reportUploadQueue = uploadQueue,
            context = context,
            ioDispatcher = Dispatchers.Unconfined
        )

        backClicks = 0
        saveCompleteCalls = 0
        catchEditClicks = 0
        methodEditClicks = 0
        commentEditClicks = 0
        waterEditClicks = 0
        waterNameEditClicks = 0
    }

    private fun setCreateReportScreen() {
        composeRule.setContent {
            // The location preview uses an AndroidView map; inspection mode keeps
            // the test focused on the Compose form instead of osmdroid.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                FishingTheme(darkTheme = false, dynamicColor = false) {
                    CreateReportScreen(
                        viewModel = viewModel,
                        onBackClick = { backClicks++ },
                        onSaveComplete = { saveCompleteCalls++ },
                        onNavigateToCatchEdit = { catchEditClicks++ },
                        onNavigateToMethodAndBaitEdit = { methodEditClicks++ },
                        onNavigateToCommentEdit = { commentEditClicks++ },
                        onNavigateToWaterEdit = { waterEditClicks++ },
                        onNavigateToWaterNameEdit = { waterNameEditClicks++ }
                    )
                }
            }
        }
    }

    private fun validPastRange(): Pair<Instant, Instant> {
        val end = Instant.now().minusSeconds(3_600)
        return end.minusSeconds(3_600) to end
    }

    /** Fills the fields required by [CreateReportViewModel.isSaveEnabled]. */
    private fun fillRequiredFields() {
        val (start, end) = validPastRange()
        val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        viewModel.formLocation = GeoPoint(54.9, 26.7)
        viewModel.formStartDate = dateFormatter.format(Date.from(start))
        viewModel.formStartTime = timeFormatter.format(Date.from(start))
        viewModel.formEndDate = dateFormatter.format(Date.from(end))
        viewModel.formEndTime = timeFormatter.format(Date.from(end))
        viewModel.formSelectedMethod = FishingMethod.SPINNING
        viewModel.formSelectedBaits = listOf(Bait.WOBBLER)
        viewModel.formSelectedFish = listOf(
            Fish(id = UUID.randomUUID(), name = "Щука", count = 2)
        )

        composeRule.waitForIdle()
    }

    @Test
    fun `form displays main steps and save starts disabled`() {
        setCreateReportScreen()

        composeRule.onNodeWithText("Новый отчет").assertIsDisplayed()
        composeRule.onNodeWithText("Водоем", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Способ ловли и наживка", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Улов", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Комментарий").assertIsDisplayed()
        composeRule.onNodeWithText("Фотографии").assertIsDisplayed()
        composeRule.onNodeWithText("Сохранить").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun `saving becomes available once required fields are filled`() {
        setCreateReportScreen()

        composeRule.onNodeWithText("Сохранить").assertIsNotEnabled()

        fillRequiredFields()

        assertTrue(viewModel.isSaveEnabled)
        composeRule.onNodeWithText("Сохранить").assertIsEnabled()
    }

    @Test
    fun `missing dates display validation errors and disable save`() {
        setCreateReportScreen()

        viewModel.formStartDate = ""
        viewModel.formEndDate = ""
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Укажите дату и время начала рыбалки")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Сохранить").assertIsNotEnabled()
        assertFalse(viewModel.isSaveEnabled)

        // Restore the start boundary: the validator then reports the missing end.
        val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
        viewModel.formStartDate = dateFormatter.format(Date.from(validPastRange().first))
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Укажите дату и время окончания рыбалки")
            .assertExists()
        assertFalse(viewModel.isSaveEnabled)
    }

    @Test
    fun `main step rows invoke their navigation callbacks`() {
        setCreateReportScreen()

        composeRule.onNodeWithText("Водоем", substring = true).performClick()
        composeRule.onNodeWithText("Способ ловли и наживка", substring = true)
            .performClick()
        composeRule.onNodeWithText("Улов", substring = true).performClick()
        composeRule.onNodeWithText("Комментарий").performClick()
        composeRule.waitForIdle()

        assertEquals(1, waterEditClicks)
        assertEquals(1, methodEditClicks)
        assertEquals(1, catchEditClicks)
        assertEquals(1, commentEditClicks)
        assertEquals(0, waterNameEditClicks)
    }

    @Test
    fun `save shows upload progress and completes when the queue drains`() {
        setCreateReportScreen()
        fillRequiredFields()
        composeRule.onNodeWithText("Сохранить").assertIsEnabled()

        uploadQueue.onEnqueue = { report ->
            uploadQueue.setPendingRow(
                reportId = report.id,
                state = ReportUploadState.UPLOADING,
                uploadedPhotos = 1,
                totalPhotos = 2
            )
        }

        composeRule.onNodeWithText("Сохранить").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) { viewModel.isSaving }

        composeRule.onNodeWithText("Идёт сохранение").assertIsDisplayed()
        composeRule.onNodeWithText("Отменить сохранение").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.saveProgress?.uploadedPhotos == 1 &&
                viewModel.saveProgress?.totalPhotos == 2
        }
        assertEquals(0, saveCompleteCalls)

        // The worker removes the row only after the report is published.
        uploadQueue.pending.value = emptyList()
        composeRule.waitUntil(timeoutMillis = 10_000) { saveCompleteCalls == 1 }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Идёт сохранение").assertDoesNotExist()
        assertTrue(viewModel.formSelectedFish.isEmpty())
        assertEquals(CreateReportSaveState.Success, viewModel.saveState)
    }

    @Test
    fun `save failure displays an error and keeps the form data`() {
        setCreateReportScreen()
        fillRequiredFields()

        uploadQueue.onEnqueue = { report ->
            uploadQueue.setPendingRow(
                reportId = report.id,
                state = ReportUploadState.FAILED,
                lastError = "Не удалось загрузить фото"
            )
        }

        composeRule.onNodeWithText("Сохранить").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.saveState == CreateReportSaveState.Error
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Не удалось загрузить фото")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Не удалось загрузить фото").assertIsDisplayed()
        assertEquals(0, saveCompleteCalls)
        assertFalse(viewModel.formSelectedFish.isEmpty())
        assertTrue(viewModel.formLocation != null)
    }

    @Test
    fun `back with unsaved data asks for confirmation before leaving`() {
        setCreateReportScreen()
        viewModel.formWaterName = "Озеро Нарочь"
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Назад").performClick()

        composeRule.onNodeWithText("Введённые данные не сохранятся. Продолжить?")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Закрыть").performClick()
        composeRule.waitForIdle()

        assertEquals(1, backClicks)
    }
}
