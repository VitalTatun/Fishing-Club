package com.example.fishing.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishing.data.FishCatalogRepository
import com.example.fishing.model.FishSpecies
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Supplies the fish catalog to the filters screen. Applied filters live in
 * [com.example.fishing.viewmodel.MainViewModel]; only the draft and the picker
 * search are screen-local.
 */
@HiltViewModel
class MapFiltersViewModel @Inject constructor(
    private val fishCatalogRepository: FishCatalogRepository
) : ViewModel() {

    val species: StateFlow<List<FishSpecies>> = fishCatalogRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        // Offline-first: the bundled seed is already visible, the refresh only
        // brings new species when the network is available.
        viewModelScope.launch { fishCatalogRepository.refresh() }
    }
}
