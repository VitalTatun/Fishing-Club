package com.example.fishing.data.report

import android.app.Application
import androidx.room.Room
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.local.entity.MarkerEntity
import com.example.fishing.model.AuthState
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
 * AUD-02 (full variant): the map follows the signed-in viewer, so switching
 * accounts must drop the previous user's private markers without waiting for a
 * network refresh.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class MarkerStoreTest {

    private lateinit var database: AppDatabase
    private lateinit var markerDao: MarkerDao
    private lateinit var auth: FakeAuthRepository
    private lateinit var store: RoomMarkerStore

    private val userA = User(UUID.randomUUID(), "A", "a@example.com", "")
    private val userB = User(UUID.randomUUID(), "B", "b@example.com", "")

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        markerDao = database.markerDao()
        auth = FakeAuthRepository()
        store = RoomMarkerStore(
            reportRemoteQueries = mockk(relaxed = true),
            markerDao = markerDao,
            database = database,
            authRepository = auth
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun marker(author: UUID, isPublic: Boolean): MarkerEntity = MarkerEntity(
        id = UUID.randomUUID(),
        userId = author,
        name = "Test Lake",
        waterName = "Test Lake",
        waterLat = 55.0,
        waterLng = 37.0,
        type = "FISHING_LOG",
        fishingMethod = "SPINNING",
        fishingStartAt = null,
        isPublic = isPublic
    )

    @Test
    fun `map follows the viewer and hides another users private markers`() = runBlocking {
        val publicByA = marker(userA.id, isPublic = true)
        val privateByA = marker(userA.id, isPublic = false)
        val publicByB = marker(userB.id, isPublic = true)
        val privateByB = marker(userB.id, isPublic = false)
        markerDao.insertAll(listOf(publicByA, privateByA, publicByB, privateByB))
        auth.setAuthState(AuthState.Authenticated(userA))

        val visibleToA = withTimeout(5_000) { store.getMapMarkers().first { it.isNotEmpty() } }

        assertEquals(
            setOf(publicByA.id, privateByA.id, publicByB.id),
            visibleToA.map { it.id }.toSet()
        )

        auth.setAuthState(AuthState.Unauthenticated)
        assertTrue(
            "signed-out map must not expose cached markers",
            withTimeout(5_000) { store.getMapMarkers().first() }.isEmpty()
        )

        auth.setAuthState(AuthState.Authenticated(userB))
        val visibleToB = withTimeout(5_000) { store.getMapMarkers().first { it.isNotEmpty() } }

        assertEquals(
            setOf(publicByA.id, publicByB.id, privateByB.id),
            visibleToB.map { it.id }.toSet()
        )
    }
}
