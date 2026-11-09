package com.example.fishing.data.local

import android.app.Application
import androidx.room.Room
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.local.entity.MarkerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * AUD-02: the marker cache is shared by all accounts, so visibility is decided
 * per row — public reports for everyone, private reports only for their author.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class MarkerDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var markerDao: MarkerDao

    private val userA = UUID.randomUUID()
    private val userB = UUID.randomUUID()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        markerDao = database.markerDao()
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
    fun `viewer sees public markers of others and own private markers only`() = runTest {
        val publicByA = marker(userA, isPublic = true)
        val privateByA = marker(userA, isPublic = false)
        val publicByB = marker(userB, isPublic = true)
        val privateByB = marker(userB, isPublic = false)
        markerDao.insertAll(listOf(publicByA, privateByA, publicByB, privateByB))

        val visibleToB = markerDao.getVisibleForUser(userB).first()

        assertEquals(
            setOf(publicByA.id, publicByB.id, privateByB.id),
            visibleToB.map { it.id }.toSet()
        )
        assertFalse("private marker of A leaked to B", visibleToB.any { it.id == privateByA.id })
    }

    @Test
    fun `author keeps their own private markers`() = runTest {
        val privateByA = marker(userA, isPublic = false)
        markerDao.insertAll(listOf(privateByA))

        val visibleToA = markerDao.getVisibleForUser(userA).first()

        assertEquals(listOf(privateByA.id), visibleToA.map { it.id })
        assertTrue(markerDao.getVisibleForUser(userB).first().isEmpty())
    }
}
