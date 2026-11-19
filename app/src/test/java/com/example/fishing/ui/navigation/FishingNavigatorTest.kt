package com.example.fishing.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FishingNavigatorTest {

    @Test
    fun `back removes one route but keeps the root route`() {
        val stack = NavBackStack<NavKey>(MainRoute)
        val navigator = FishingNavigator(stack)
        navigator.navigate(CreateReportRoute)

        assertTrue(navigator.popBackStack())
        assertEquals(listOf(MainRoute), stack.toList())
        assertFalse(navigator.popBackStack())
        assertEquals(listOf(MainRoute), stack.toList())
    }

    @Test
    fun `auth redirect replaces the whole stack`() {
        val stack = NavBackStack<NavKey>(MainRoute)
        stack.add(CreateReportRoute)
        val navigator = FishingNavigator(stack)

        navigator.replaceStack(LoginRoute)

        assertEquals(listOf(LoginRoute), stack.toList())
    }

    @Test
    fun `map marker replaces full map and preserves previous detail`() {
        val firstDetail = ReportDetailRoute("first")
        val secondDetail = ReportDetailRoute("second")
        val stack = NavBackStack<NavKey>(MainRoute)
        stack.add(firstDetail)
        stack.add(FullMapRoute(firstDetail.reportId))
        val navigator = FishingNavigator(stack)

        navigator.replaceCurrent(secondDetail)

        assertEquals(listOf(MainRoute, firstDetail, secondDetail), stack.toList())
    }

    @Test
    fun `location result replaces search and location editor routes`() {
        val stack = NavBackStack<NavKey>(MainRoute)
        stack.add(CreateReportRoute)
        stack.add(FishingLocationRoute())
        stack.add(LocationSearchRoute(fromWaterEdit = true))
        val navigator = FishingNavigator(stack)

        val result = FishingLocationRoute(searchLatitude = 44.0, searchLongitude = -71.0)
        navigator.replaceCurrentAndPrevious(result)

        assertEquals(
            listOf(MainRoute, CreateReportRoute, result),
            stack.toList()
        )
    }
}
