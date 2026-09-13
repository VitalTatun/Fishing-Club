package com.example.fishing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.LikeDao
import com.example.fishing.data.local.entity.LikeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LikeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var likeDao: LikeDao

    private val userAId = UUID.randomUUID()
    private val userBId = UUID.randomUUID()
    private val report1Id = UUID.randomUUID()
    private val report2Id = UUID.randomUUID()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        likeDao = database.likeDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and getLikedIds returns liked report ids`() = runTest {
        likeDao.insertAll(
            listOf(
                LikeEntity(userAId, report1Id),
                LikeEntity(userAId, report2Id)
            )
        )

        assertEquals(setOf(report1Id, report2Id), likeDao.getLikedIds(userAId).first().toSet())
    }

    @Test
    fun `duplicate insert is ignored`() = runTest {
        likeDao.insertAll(listOf(LikeEntity(userAId, report1Id)))
        likeDao.insertAll(listOf(LikeEntity(userAId, report1Id)))

        assertEquals(listOf(report1Id), likeDao.getLikedIds(userAId).first())
    }

    @Test
    fun `delete removes own like`() = runTest {
        likeDao.insertAll(listOf(LikeEntity(userAId, report1Id), LikeEntity(userAId, report2Id)))

        likeDao.delete(userAId, report1Id)

        assertEquals(listOf(report2Id), likeDao.getLikedIds(userAId).first())
    }

    @Test
    fun `deleteByReportId removes likes of all users for the report`() = runTest {
        likeDao.insertAll(
            listOf(
                LikeEntity(userAId, report1Id),
                LikeEntity(userBId, report1Id),
                LikeEntity(userAId, report2Id)
            )
        )

        likeDao.deleteByReportId(report1Id)

        assertTrue(likeDao.getLikedIds(userAId).first().contains(report2Id))
        assertFalse(likeDao.getLikedIds(userAId).first().contains(report1Id))
        assertTrue(likeDao.getLikedIds(userBId).first().isEmpty())
    }

    @Test
    fun `deleteAllForUser removes only that user likes`() = runTest {
        likeDao.insertAll(
            listOf(
                LikeEntity(userAId, report1Id),
                LikeEntity(userBId, report1Id)
            )
        )

        likeDao.deleteAllForUser(userAId)

        assertTrue(likeDao.getLikedIds(userAId).first().isEmpty())
        assertEquals(listOf(report1Id), likeDao.getLikedIds(userBId).first())
    }

    @Test
    fun `deleteAll clears everything`() = runTest {
        likeDao.insertAll(
            listOf(
                LikeEntity(userAId, report1Id),
                LikeEntity(userBId, report2Id)
            )
        )

        likeDao.deleteAll()

        assertTrue(likeDao.getLikedIds(userAId).first().isEmpty())
        assertTrue(likeDao.getLikedIds(userBId).first().isEmpty())
    }

    @Test
    fun `getLikedIds is empty for unknown user`() = runTest {
        likeDao.insertAll(listOf(LikeEntity(userAId, report1Id)))

        assertTrue(likeDao.getLikedIds(userBId).first().isEmpty())
    }
}
