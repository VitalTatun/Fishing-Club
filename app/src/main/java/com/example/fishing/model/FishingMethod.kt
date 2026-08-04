package com.example.fishing.model

import androidx.annotation.StringRes
import com.example.fishing.R

enum class FishingMethod(@StringRes val labelRes: Int) {
    NONE(R.string.method_none),
    BOBBER(R.string.method_bobber),
    SPINNING(R.string.method_spinning),
    FEEDER(R.string.method_feeder),
    FLY_FISHING(R.string.method_fly);

    companion object {
        val methodsAndBaits: Map<FishingMethod, List<Bait>> = mapOf(
            BOBBER to listOf(
                Bait.WORM, Bait.MAGGOT, Bait.BLOODWORM, Bait.BARLEY,
                Bait.CORN, Bait.BREAD, Bait.POTATO, Bait.SEMOLINA
            ),
            FEEDER to listOf(
                Bait.WORM, Bait.MAGGOT, Bait.BLOODWORM, Bait.BARLEY,
                Bait.CORN, Bait.BREAD, Bait.POTATO, Bait.SEMOLINA
            ),
            SPINNING to listOf(
                Bait.SPOONBAIT, Bait.WOBBLER, Bait.EDIBLE_RUBBER
            ),
            FLY_FISHING to listOf(
                Bait.FLY, Bait.GRASSHOPPER, Bait.BUTTERFLY
            )
        )
    }
}
