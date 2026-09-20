package com.example.fishing.data

import com.example.fishing.model.ReportSortOrder
import com.example.fishing.model.ReportDisplayMode
import java.util.UUID

interface UserPreferencesSource {
    fun getSortOrder(userId: UUID? = null): ReportSortOrder
    fun setSortOrder(userId: UUID, order: ReportSortOrder)
    fun getDisplayMode(userId: UUID? = null): ReportDisplayMode
    fun setDisplayMode(userId: UUID, mode: ReportDisplayMode)
}
