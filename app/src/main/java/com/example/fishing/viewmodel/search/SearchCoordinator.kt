package com.example.fishing.viewmodel.search

import com.example.fishing.viewmodel.filter.ReportSearchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the search form state that survives navigation between the list search
 * and the main screen. Filtering itself is a pure operation in
 * [com.example.fishing.viewmodel.filter.ReportSearchState].
 */
class SearchCoordinator {
    private val _state = MutableStateFlow(ReportSearchState())
    val state: StateFlow<ReportSearchState> = _state.asStateFlow()

    fun setState(state: ReportSearchState) {
        _state.value = state
    }

    fun update(transform: (ReportSearchState) -> ReportSearchState) {
        _state.update(transform)
    }

    fun clear() {
        _state.value = ReportSearchState()
    }
}
