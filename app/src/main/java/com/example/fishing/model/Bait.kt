package com.example.fishing.model

import androidx.annotation.StringRes
import com.example.fishing.R

enum class Bait(@StringRes val labelRes: Int) {
    WORM(R.string.bait_worm),
    MAGGOT(R.string.bait_maggot),
    BLOODWORM(R.string.bait_bloodworm),
    BAITFISH(R.string.bait_baitfish),
    BARLEY(R.string.bait_barley),
    CORN(R.string.bait_corn),
    BREAD(R.string.bait_bread),
    POTATO(R.string.bait_potato),
    SEMOLINA(R.string.bait_semolina),
    SPOONBAIT(R.string.bait_spoonbait),
    WOBBLER(R.string.bait_wobbler),
    EDIBLE_RUBBER(R.string.bait_edible_rubber),
    FLY(R.string.bait_fly),
    GRASSHOPPER(R.string.bait_grasshopper),
    BUTTERFLY(R.string.bait_butterfly),
    NONE(R.string.bait_none)
}
