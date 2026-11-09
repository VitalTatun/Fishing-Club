package com.example.fishing.viewmodel.search

import com.example.fishing.model.FishingMethod
import com.example.fishing.viewmodel.filter.ReportFilters
import com.example.fishing.viewmodel.filter.ReportSearchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCoordinatorTest {

    @Test
    fun `state starts empty`() {
        val coordinator = SearchCoordinator()

        assertEquals(ReportSearchState(), coordinator.state.value)
        assertTrue(coordinator.state.value.isEmpty)
    }

    @Test
    fun `state updates preserve unrelated fields`() {
        val coordinator = SearchCoordinator()
        val filters = ReportFilters(
            isFavoritesSelected = true,
            selectedMethod = FishingMethod.SPINNING
        )

        coordinator.setState(
            ReportSearchState(
                query = "pike",
                selectedDateMillis = 123L,
                filters = filters
            )
        )
        coordinator.update { it.copy(filters = it.filters.copy(selectedCatch = "Pike")) }

        val state = coordinator.state.value
        assertEquals("pike", state.query)
        assertEquals(123L, state.selectedDateMillis)
        assertEquals("Pike", state.filters.selectedCatch)
        assertEquals(FishingMethod.SPINNING, state.filters.selectedMethod)
        assertTrue(state.filters.isFavoritesSelected)
    }

    @Test
    fun `clear resets the whole search form`() {
        val coordinator = SearchCoordinator()
        coordinator.setState(
            ReportSearchState(
                query = "pike",
                selectedDateMillis = 123L,
                filters = ReportFilters(isTrophySelected = true)
            )
        )

        coordinator.clear()

        assertEquals(ReportSearchState(), coordinator.state.value)
    }
}
