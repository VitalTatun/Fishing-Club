package com.example.fishing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.FavoriteReportDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.entity.FavoriteReportEntity
import com.example.fishing.data.local.entity.ReportDetailsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReportDetailsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var reportDetailsDao: ReportDetailsDao
    private lateinit var favoriteReportDao: FavoriteReportDao

    private val userAId = UUID.randomUUID()
    private val userBId = UUID.randomUUID()

    private fun createReportEntity(id: UUID, userId: UUID): ReportDetailsEntity {
        return ReportDetailsEntity(
            id = id,
            userId = userId,
            publishedAt = null,
            type = "FISHING_LOG",
            name = "Test Report",
            waterName = "Test Lake",
            waterLat = 55.0,
            waterLng = 37.0,
            waterPaid = false,
            spotLat = null,
            spotLng = null,
            fishingStartAt = "2024-01-01T10:00:00Z",
            fishingEndAt = null,
            weight = 1.0,
            fishingMethod = "SPINNING",
            comment = "Test",
            shore = true,
            isPublic = true,
            imageUrls = emptyList(),
            fishJson = "[]",
            baitsJson = "[]",
            authorName = "Test User",
            authorAvatar = null,
            createdAt = null
        )
    }

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        reportDetailsDao = database.reportDetailsDao()
        favoriteReportDao = database.favoriteDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `user A gets own reports via getHomeReports`() = runTest {
        val reportA = createReportEntity(id = UUID.randomUUID(), userId = userAId)
        reportDetailsDao.insertAll(listOf(reportA))

        val reports = reportDetailsDao.getHomeReports(userAId).first()
        assertEquals(1, reports.size)
        assertEquals(reportA.id, reports[0].id)
    }

    @Test
    fun `user A gets own favorites via getHomeReports`() = runTest {
        val reportB = createReportEntity(id = UUID.randomUUID(), userId = userBId)
        reportDetailsDao.insertAll(listOf(reportB))
        favoriteReportDao.insertAll(listOf(FavoriteReportEntity(userId = userAId, reportId = reportB.id)))

        val reports = reportDetailsDao.getHomeReports(userAId).first()
        assertEquals(1, reports.size)
        assertEquals(reportB.id, reports[0].id)
    }

    @Test
    fun `user B does not get user A favorites`() = runTest {
        val reportA = createReportEntity(id = UUID.randomUUID(), userId = userAId)
        reportDetailsDao.insertAll(listOf(reportA))
        // Only userA favorited reportA — userB should NOT see it in their home
        // unless it's userB's own report or userB also favorited it

        val reportsB = reportDetailsDao.getHomeReports(userBId).first()
        assertTrue(reportsB.isEmpty())
    }

    @Test
    fun `deleteByUserId A removes only A data`() = runTest {
        val reportA1 = createReportEntity(id = UUID.randomUUID(), userId = userAId)
        val reportA2 = createReportEntity(id = UUID.randomUUID(), userId = userAId)
        val reportB = createReportEntity(id = UUID.randomUUID(), userId = userBId)
        reportDetailsDao.insertAll(listOf(reportA1, reportA2, reportB))

        reportDetailsDao.deleteByUserId(userAId)

        val remaining = reportDetailsDao.getHomeReports(userBId).first()
        assertEquals(1, remaining.size)
        assertEquals(reportB.id, remaining[0].id)
    }

    @Test
    fun `deleteAllForUser A favorites does not affect B`() = runTest {
        val reportA = createReportEntity(id = UUID.randomUUID(), userId = userAId)
        val reportB = createReportEntity(id = UUID.randomUUID(), userId = userBId)
        reportDetailsDao.insertAll(listOf(reportA, reportB))

        favoriteReportDao.insertAll(listOf(
            FavoriteReportEntity(userId = userAId, reportId = reportB.id),
            FavoriteReportEntity(userId = userBId, reportId = reportA.id)
        ))

        favoriteReportDao.deleteAllForUser(userAId)

        // User A's favorites should be empty
        val favsA = reportDetailsDao.getFavorites(userAId).first()
        assertTrue(favsA.isEmpty())

        // User B's favorites should still exist
        val favsB = reportDetailsDao.getFavorites(userBId).first()
        assertEquals(1, favsB.size)
        assertEquals(reportA.id, favsB[0].id)
    }

    @Test
    fun `user A getFavorites returns only A favorites`() = runTest {
        val reportA = createReportEntity(id = UUID.randomUUID(), userId = userAId)
        val reportB = createReportEntity(id = UUID.randomUUID(), userId = userBId)
        reportDetailsDao.insertAll(listOf(reportA, reportB))

        favoriteReportDao.insertAll(listOf(
            FavoriteReportEntity(userId = userAId, reportId = reportB.id)
        ))

        val favsA = reportDetailsDao.getFavorites(userAId).first()
        assertEquals(1, favsA.size)
        assertEquals(reportB.id, favsA[0].id)
    }

    @Test
    fun `getByIdOneShot returns report if exists`() = runTest {
        val id = UUID.randomUUID()
        val report = createReportEntity(id = id, userId = userAId)
        reportDetailsDao.insert(report)

        val fetched = reportDetailsDao.getByIdOneShot(id)
        assertNotNull(fetched)
        assertEquals(id, fetched?.id)
    }
}
