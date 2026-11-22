package com.example.fishing.ui.screens.report.create

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.example.fishing.model.Bait
import com.example.fishing.model.FishingMethod
import com.example.fishing.viewmodel.savedstate.baitsFromSaved
import com.example.fishing.viewmodel.savedstate.baitsToSaved

/**
 * Saveable wrappers for the edit sub-screens. The values reuse the same codecs
 * as the create-report draft, so editing survives both configuration changes
 * and process death.
 */
val BaitListSaver: Saver<SnapshotStateList<Bait>, Any> =
    listSaver<SnapshotStateList<Bait>, String>(
        save = { baitsToSaved(it) },
        restore = { baitsFromSaved(it).toMutableStateList() },
    )

val FishingMethodSaver: Saver<FishingMethod, String> = Saver(
    save = { it.name },
    restore = { name -> FishingMethod.entries.firstOrNull { it.name == name } },
)
