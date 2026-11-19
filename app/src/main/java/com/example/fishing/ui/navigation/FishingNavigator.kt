package com.example.fishing.ui.navigation

import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Small event-oriented wrapper around Navigation 3's back stack.
 *
 * The app has one stack: the tabs themselves remain owned by MainScreen, while
 * report flows and profile pages are pushed on top of it.
 */
internal class FishingNavigator(
    val backStack: NavBackStack<NavKey>
) {
    val currentKey: NavKey?
        get() = backStack.lastOrNull()

    val previousKey: NavKey?
        get() = backStack.getOrNull(backStack.lastIndex - 1)

    fun navigate(key: NavKey) {
        backStack.add(key)
    }

    fun popBackStack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeLastOrNull()
        return true
    }

    fun replaceStack(key: NavKey) {
        Snapshot.withMutableSnapshot {
            backStack.clear()
            backStack.add(key)
        }
    }

    fun replaceCurrent(key: NavKey) {
        if (backStack.isEmpty()) {
            backStack.add(key)
        } else {
            backStack[backStack.lastIndex] = key
        }
    }

    /** Replaces the current screen and its parent with [key]. */
    fun replaceCurrentAndPrevious(key: NavKey) {
        if (backStack.size > 1) backStack.removeLastOrNull()
        if (backStack.size > 1) backStack.removeLastOrNull()
        backStack.add(key)
    }
}
