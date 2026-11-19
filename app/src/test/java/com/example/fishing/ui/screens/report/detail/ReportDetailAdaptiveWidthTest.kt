package com.example.fishing.ui.screens.report.detail

import android.app.Application
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.fishing.testutil.testReport
import com.example.fishing.ui.theme.FishingTheme
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
class ReportDetailAdaptiveWidthTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `report detail body is centered and capped on a wide window`() {
        composeRule.setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                ReportDetailScreen(
                    report = testReport(comment = "A detailed report comment."),
                    onBackClick = {}
                )
            }
        }

        val contentBounds = composeRule.onNode(hasScrollAction())
            .fetchSemanticsNode()
            .boundsInRoot
        val rootWidth = composeRule.onRoot()
            .fetchSemanticsNode()
            .boundsInRoot
            .width
        val maxContentWidth = with(composeRule.density) { 640.dp.toPx() }

        assertTrue(contentBounds.width <= maxContentWidth)
        assertTrue(abs(contentBounds.center.x - rootWidth / 2f) < 2f)
    }
}
