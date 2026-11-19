package com.example.fishing.ui.navigation

import android.app.Application
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class NavigationBackStackTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `typed back stack and route arguments survive saved state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        val observedKeys = mutableStateOf<List<NavKey>>(emptyList())
        val stackReference = mutableStateOf<androidx.navigation3.runtime.NavBackStack<NavKey>?>(null)
        val expectedKeys = listOf(
            SplashRoute,
            MainRoute,
            CreateReportRoute,
            ReportDetailRoute(
                reportId = "report-123",
                allowAuthorNavigation = false
            ),
            FishingLocationRoute(
                searchLatitude = 45.5,
                searchLongitude = -73.6
            ),
            LocationSearchRoute(fromWaterEdit = true)
        )

        restorationTester.setContent {
            val backStack = rememberNavBackStack(SplashRoute)
            val keys = backStack.toList()
            SideEffect {
                stackReference.value = backStack
                observedKeys.value = keys
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            expectedKeys.drop(1).forEach { key -> stackReference.value!!.add(key) }
        }
        composeRule.waitForIdle()
        assertEquals(expectedKeys, observedKeys.value)

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals(expectedKeys, observedKeys.value)
    }
}
