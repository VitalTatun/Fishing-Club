package com.example.fishing.viewmodel

import com.example.fishing.model.FishSpecies
import com.example.fishing.model.FishSuggestion

/**
 * State of the catch search: the mode, the query and the three suggestion
 * sources. `chips` powers the normal screen (history, otherwise popular),
 * `recent` is the shorter list shown inside the full-screen search.
 */
data class CatchSearchState(
    val isOpen: Boolean = false,
    val query: String = "",
    val recent: List<FishSuggestion> = emptyList(),
    val popular: List<FishSuggestion> = emptyList(),
    val chips: List<FishSuggestion> = emptyList(),
    val results: List<FishSpecies> = emptyList()
)
