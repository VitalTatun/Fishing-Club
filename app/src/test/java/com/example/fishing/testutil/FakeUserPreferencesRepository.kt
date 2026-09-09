package com.example.fishing.testutil

import com.example.fishing.data.UserPreferencesSource
import com.example.fishing.model.ReportSortOrder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeUserPreferencesRepository : UserPreferencesSource {

    private val sortOrders = ConcurrentHashMap<String, ReportSortOrder>()

    override fun getSortOrder(userId: UUID?): ReportSortOrder {
        val key = userId?.toString() ?: "default"
        return sortOrders[key] ?: ReportSortOrder.BY_FISHING_TIME
    }

    override fun setSortOrder(userId: UUID, order: ReportSortOrder) {
        sortOrders[userId.toString()] = order
    }
}
