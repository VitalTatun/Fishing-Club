package com.example.fishing.ui.screens.report.create

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.viewmodel.savedstate.baitsFromSaved
import com.example.fishing.viewmodel.savedstate.baitsToSaved
import com.example.fishing.viewmodel.savedstate.fishFromSaved
import com.example.fishing.viewmodel.savedstate.fishToSaved

/**
 * Saveable wrappers for the edit sub-screens. The values reuse the same codecs
 * as the create-report draft, so editing survives both configuration changes
 * and process death.
 */
val FishListSaver: Saver<SnapshotStateList<Fish>, Any> =
    listSaver<SnapshotStateList<Fish>, String>(
        save = { fishToSaved(it) },
        restore = { fishFromSaved(it).toMutableStateList() },
    )

val BaitListSaver: Saver<SnapshotStateList<Bait>, Any> =
    listSaver<SnapshotStateList<Bait>, String>(
        save = { baitsToSaved(it) },
        restore = { baitsFromSaved(it).toMutableStateList() },
    )

val FishingMethodSaver: Saver<FishingMethod, String> = Saver(
    save = { it.name },
    restore = { name -> FishingMethod.entries.firstOrNull { it.name == name } },
)
