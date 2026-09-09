package com.example.fishing.data

import android.content.Context
import com.example.fishing.model.ReportSortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesSource {

    private val prefs by lazy { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }

    companion object {
        private const val KEY_SORT_ORDER = "report_sort_order"
    }

    override fun getSortOrder(userId: UUID?): ReportSortOrder {
        val key = if (userId != null) "${KEY_SORT_ORDER}_$userId" else KEY_SORT_ORDER
        val name = prefs.getString(key, ReportSortOrder.BY_FISHING_TIME.name)
        return try {
            ReportSortOrder.valueOf(name ?: ReportSortOrder.BY_FISHING_TIME.name)
        } catch (e: Exception) {
            ReportSortOrder.BY_FISHING_TIME
        }
    }

    override fun setSortOrder(userId: UUID, order: ReportSortOrder) {
        val key = "${KEY_SORT_ORDER}_$userId"
        prefs.edit().putString(key, order.name).apply()
    }
}
