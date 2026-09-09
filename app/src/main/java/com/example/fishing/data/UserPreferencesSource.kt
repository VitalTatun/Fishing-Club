package com.example.fishing.data

import com.example.fishing.model.ReportSortOrder
import java.util.UUID

interface UserPreferencesSource {
    fun getSortOrder(userId: UUID? = null): ReportSortOrder
    fun setSortOrder(userId: UUID, order: ReportSortOrder)
}
