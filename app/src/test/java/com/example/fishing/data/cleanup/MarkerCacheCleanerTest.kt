package com.example.fishing.data.cleanup

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.local.entity.MarkerEntity
import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * AUD-02 (minimal step): cached map markers must not survive a session, so a
 * different account can never see them before the first network refresh.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MarkerCacheCleanerTest {

    private lateinit var database: AppDatabase
    private lateinit var markerDao: MarkerDao

    private val user = User(
        id = UUID.randomUUID(),
        name = "Test",
        email = "test@example.com",
        image = ""
    )

    @Before
    fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        markerDao = database.markerDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun marker(): MarkerEntity = MarkerEntity(
        id = UUID.randomUUID(),
        userId = user.id,
        name = "Test Lake",
        waterName = "Test Lake",
        waterLat = 55.0,
        waterLng = 37.0,
        type = "FISHING_LOG",
        fishingMethod = "SPINNING",
        fishingStartAt = null,
        isPublic = false
    )

    @Test
    fun `cached markers are dropped when the session ends`() = runTest {
        markerDao.insertAll(listOf(marker(), marker()))
        assertEquals(2, markerDao.getVisibleForUser(user.id).first().size)

        val authState = MutableStateFlow<AuthState>(AuthState.Authenticated(user))
        MarkerCacheCleaner(markerDao, backgroundScope).clearWhenUnauthenticated(authState)
        advanceUntilIdle()
        // While a session is active the cache stays usable (offline map).
        assertEquals(2, markerDao.getVisibleForUser(user.id).first().size)

        authState.value = AuthState.Unauthenticated
        advanceUntilIdle()

        assertTrue(markerDao.getVisibleForUser(user.id).first().isEmpty())
    }

    @Test
    fun `stale markers are dropped on a cold start without a session`() = runTest {
        markerDao.insertAll(listOf(marker()))

        val authState = MutableStateFlow<AuthState>(AuthState.Loading)
        MarkerCacheCleaner(markerDao, backgroundScope).clearWhenUnauthenticated(authState)
        advanceUntilIdle()
        // Session restore in progress: do not wipe a cache that may belong to
        // the user who is about to be restored.
        assertEquals(1, markerDao.getVisibleForUser(user.id).first().size)

        authState.value = AuthState.Unauthenticated
        advanceUntilIdle()

        assertTrue(markerDao.getVisibleForUser(user.id).first().isEmpty())
    }
}
