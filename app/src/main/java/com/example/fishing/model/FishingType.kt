package com.example.fishing.model

import androidx.annotation.StringRes
import com.example.fishing.R

enum class FishingType(@StringRes val labelRes: Int) {
    FISHING_LOG(R.string.type_report),
    HAUL(R.string.type_trophy)
}
