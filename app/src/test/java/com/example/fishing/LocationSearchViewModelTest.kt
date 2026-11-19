package com.example.fishing

import androidx.lifecycle.SavedStateHandle
import com.example.fishing.data.network.NominatimResponse
import com.example.fishing.data.network.NominatimSearchResult
import com.example.fishing.data.network.NominatimService
import com.example.fishing.testutil.MainDispatcherRule
import com.example.fishing.viewmodel.LocationSearchViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val nominatimService = mockk<NominatimService>()

    @Test
    fun `search failure is visible and retry can succeed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val result = response()
            coEvery { nominatimService.search("Минск") } returnsMany listOf(
                NominatimSearchResult.Failure,
                NominatimSearchResult.Success(listOf(result)),
            )
            val viewModel = LocationSearchViewModel(nominatimService, SavedStateHandle())

            viewModel.onQueryChanged("Минск")
            advanceTimeBy(1_000)
            runCurrent()

            assertTrue(viewModel.uiState.value.hasError)
            assertFalse(viewModel.uiState.value.isSearching)

            viewModel.retry()
            runCurrent()

            assertFalse(viewModel.uiState.value.hasError)
            assertEquals(listOf(result), viewModel.uiState.value.results)
            coVerify(exactly = 2) { nominatimService.search("Минск") }
        }

    @Test
    fun `short queries do not call Nominatim`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = LocationSearchViewModel(nominatimService, SavedStateHandle())

        viewModel.onQueryChanged("Аб")
        advanceTimeBy(2_000)
        runCurrent()

        assertEquals("Аб", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.isSearching)
        coVerify(exactly = 0) { nominatimService.search(any()) }
    }

    @Test
    fun `restored query is saved and searched again`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("location_search_query" to "Минск"))
        coEvery { nominatimService.search("Минск") } returns
            NominatimSearchResult.Success(emptyList())
        val viewModel = LocationSearchViewModel(nominatimService, savedStateHandle)

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals("Минск", viewModel.uiState.value.query)
        coVerify(exactly = 1) { nominatimService.search("Минск") }
    }

    private fun response() = NominatimResponse(
        placeId = 1,
        licence = "",
        osmType = "relation",
        osmId = 1,
        lat = "53.9",
        lon = "27.56",
        displayName = "Минск, Беларусь",
        category = "place",
        type = "city",
        importance = 0.8,
    )
}
