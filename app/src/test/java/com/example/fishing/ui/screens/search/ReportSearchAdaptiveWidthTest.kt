package com.example.fishing.ui.screens.search

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.fishing.testutil.testReport
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.filter.ReportSearchState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = Application::class,
    qualifiers = "w1280dp-h800dp"
)
class ReportSearchAdaptiveWidthTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `report search results stay centered and bounded on a wide window`() {
        val report = testReport(
            name = "Adaptive search report",
            waterName = "Adaptive search lake",
            comment = "Adaptive search result"
        )
        val searchState = ReportSearchState(query = "Adaptive search")

        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                ReportSearchScreen(
                    reports = listOf(report),
                    state = searchState,
                    onStateChange = { update -> update(searchState) },
                    onReportClick = {},
                    onBack = {}
                )
            }
        }

        val results = composeRule.onNodeWithTag("report_search_results")
        results.assertIsDisplayed()
        val resultBounds = results.fetchSemanticsNode().boundsInRoot
        val rootWidth = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.width
        val maxContentWidth = with(composeRule.density) { 640.dp.toPx() }

        assertTrue(resultBounds.width <= maxContentWidth)
        assertTrue(abs(resultBounds.center.x - rootWidth / 2f) < 2f)
    }
}
