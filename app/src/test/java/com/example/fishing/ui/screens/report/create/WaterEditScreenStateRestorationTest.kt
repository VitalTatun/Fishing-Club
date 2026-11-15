package com.example.fishing.ui.screens.report.create

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.FakeReportUploadQueue
import com.example.fishing.testutil.FakeUserPreferencesRepository
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.CreateReportViewModel
import com.example.fishing.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * AUD-06: [WaterEditScreen] keeps the name and the two switches in
 * `rememberSaveable` local state (they are only copied into the draft
 * ViewModel when the user taps save), so they must survive a configuration
 * change / process death on their own.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class WaterEditScreenStateRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var viewModel: CreateReportViewModel
    private lateinit var reportsViewModel: MainViewModel

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        val authRepository = FakeAuthRepository().apply {
            sessionUser = User(
                id = UUID.randomUUID(),
                name = "Test User",
                email = "test@example.com",
                image = ""
            )
        }
        val fishingRepository = FakeFishingRepository()
        val uploadQueue = FakeReportUploadQueue()
        viewModel = CreateReportViewModel(
            repository = fishingRepository,
            authRepository = authRepository,
            reportUploadQueue = uploadQueue,
            context = context,
            ioDispatcher = Dispatchers.Unconfined
        )
        reportsViewModel = MainViewModel(
            repository = fishingRepository,
            authRepository = authRepository,
            userPreferencesRepository = FakeUserPreferencesRepository(),
            reportUploadQueue = uploadQueue
        )
    }

    @Test
    fun `water name and switches survive state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                WaterEditScreen(
                    viewModel = viewModel,
                    reportsViewModel = reportsViewModel,
                    onBackClick = {},
                    onSaveClick = {}
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("Новое озеро")
        // First switch is "Платный водоем", which starts off.
        composeRule.onAllNodes(isToggleable())[0].performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Новое озеро").assertIsDisplayed()
        composeRule.onAllNodes(isToggleable())[0].assertIsOn()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Новое озеро").assertIsDisplayed()
        composeRule.onAllNodes(isToggleable())[0].assertIsOn()
        // The draft itself must stay untouched until the screen is saved.
        assertEquals("", viewModel.formWaterName)
        assertFalse(viewModel.formIsPaidWater)
    }
}
