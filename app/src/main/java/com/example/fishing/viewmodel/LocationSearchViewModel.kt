package com.example.fishing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.network.NominatimResponse
import com.example.fishing.data.network.NominatimSearchResult
import com.example.fishing.data.network.NominatimService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationSearchUiState(
    val query: String = "",
    val results: List<NominatimResponse> = emptyList(),
    val isSearching: Boolean = false,
    val hasError: Boolean = false,
)

@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val nominatimService: NominatimService,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val restoredQuery = savedStateHandle[QUERY_KEY] ?: ""
    private val _uiState = MutableStateFlow(LocationSearchUiState(query = restoredQuery))
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        if (restoredQuery.trim().length >= MIN_QUERY_LENGTH) {
            scheduleSearch(restoredQuery)
        }
    }

    fun onQueryChanged(query: String) {
        savedStateHandle[QUERY_KEY] = query
        _uiState.update {
            it.copy(
                query = query,
                results = emptyList(),
                isSearching = false,
                hasError = false,
            )
        }
        searchJob?.cancel()

        if (query.trim().length < MIN_QUERY_LENGTH) return

        scheduleSearch(query)
    }

    fun retry() {
        val query = _uiState.value.query
        if (query.trim().length < MIN_QUERY_LENGTH) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            search(query)
        }
    }

    private suspend fun search(query: String) {
        _uiState.update { it.copy(isSearching = true, hasError = false) }
        val result = try {
            nominatimService.search(query)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NominatimSearchResult.Failure
        }

        if (_uiState.value.query != query) return
        _uiState.update { state ->
            when (result) {
                is NominatimSearchResult.Success -> state.copy(
                    results = result.results,
                    isSearching = false,
                    hasError = false,
                )
                NominatimSearchResult.Failure -> state.copy(
                    results = emptyList(),
                    isSearching = false,
                    hasError = true,
                )
            }
        }
    }

    private fun scheduleSearch(query: String) {
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            search(query)
        }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 3
        const val SEARCH_DEBOUNCE_MILLIS = 1_000L
        const val QUERY_KEY = "location_search_query"
    }
}
