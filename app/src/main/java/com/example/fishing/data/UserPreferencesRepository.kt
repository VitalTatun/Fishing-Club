package com.example.fishing.data

import android.content.Context
import com.example.fishing.model.ReportSortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SORT_ORDER = "report_sort_order"
    }

    fun getSortOrder(): ReportSortOrder {
        val name = prefs.getString(KEY_SORT_ORDER, ReportSortOrder.BY_FISHING_TIME.name)
        return try {
            ReportSortOrder.valueOf(name ?: ReportSortOrder.BY_FISHING_TIME.name)
        } catch (e: Exception) {
            ReportSortOrder.BY_FISHING_TIME
        }
    }

    fun setSortOrder(order: ReportSortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
    }
}
