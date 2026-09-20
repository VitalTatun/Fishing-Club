package com.example.fishing.testutil

import com.example.fishing.data.UserPreferencesSource
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.model.ReportDisplayMode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeUserPreferencesRepository : UserPreferencesSource {

    private val sortOrders = ConcurrentHashMap<String, ReportSortOrder>()
    private val displayModes = ConcurrentHashMap<String, ReportDisplayMode>()

    override fun getSortOrder(userId: UUID?): ReportSortOrder {
        val key = userId?.toString() ?: "default"
        return sortOrders[key] ?: ReportSortOrder.BY_FISHING_TIME
    }

    override fun setSortOrder(userId: UUID, order: ReportSortOrder) {
        sortOrders[userId.toString()] = order
    }

    override fun getDisplayMode(userId: UUID?): ReportDisplayMode {
        val key = userId?.toString() ?: "default"
        return displayModes[key] ?: ReportDisplayMode.CARD
    }

    override fun setDisplayMode(userId: UUID, mode: ReportDisplayMode) {
        displayModes[userId.toString()] = mode
    }
}
