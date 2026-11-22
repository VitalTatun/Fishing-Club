package com.example.fishing.ui.screens.report.create

import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishCatalogRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.FakeReportUploadQueue
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.CreateReportViewModel
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * AUD-06: [CreateReportScreen]'s local UI state must survive recreation.
 * Dialogs are intentionally transient, but the expanded "water details" block
 * is a pure view preference and is saved.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w411dp-h891dp"
)
class CreateReportScreenStateRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var viewModel: CreateReportViewModel

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
        viewModel = CreateReportViewModel(
            repository = FakeFishingRepository(),
            fishCatalogRepository = FakeFishCatalogRepository(),
            authRepository = authRepository,
            reportUploadQueue = FakeReportUploadQueue(),
            context = context,
            ioDispatcher = Dispatchers.Unconfined
        )
        // The details header only exists once the water has a name and a point.
        viewModel.formWaterName = "Озеро Нарочь"
        viewModel.formLocation = GeoPoint(54.9, 26.7)
    }

    @Test
    fun `expanded water details survive state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                FishingTheme(darkTheme = false, dynamicColor = false) {
                    CreateReportScreen(
                        viewModel = viewModel,
                        onBackClick = {},
                        onSaveComplete = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Детали").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Платный водоем").performScrollTo().assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Платный водоем").performScrollTo().assertIsDisplayed()
    }
}
