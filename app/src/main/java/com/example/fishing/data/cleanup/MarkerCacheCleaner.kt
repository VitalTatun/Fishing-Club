package com.example.fishing.data.cleanup

import com.example.fishing.data.ApplicationScope
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.model.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AUD-02, minimal step: the marker cache is not partitioned by user yet, so it
 * must not survive a session. Whenever auth state falls back to
 * [AuthState.Unauthenticated] — explicit logout, revoked or expired refresh
 * token, failed session restore — the cached rows are dropped, and the next
 * signed-in session refills them from the server. Without this, the map could
 * briefly render markers of the previous account.
 *
 * The proper fix (a `userId` column on `MarkerEntity` plus filtering) is the
 * follow-up step of AUD-02; it also covers switching accounts without passing
 * through `Unauthenticated`.
 */
@Singleton
class MarkerCacheCleaner @Inject constructor(
    private val markerDao: MarkerDao,
    @param:ApplicationScope private val scope: CoroutineScope
) {

    /** Collects [authState] on the application scope and returns the observer job. */
    fun clearWhenUnauthenticated(authState: Flow<AuthState>): Job = scope.launch {
        authState.collect { state ->
            if (state is AuthState.Unauthenticated) {
                markerDao.deleteAll()
            }
        }
    }
}
