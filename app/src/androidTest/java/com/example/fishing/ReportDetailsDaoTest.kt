package com.example.fishing

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.dao.FavoriteReportDao
import com.example.fishing.data.local.dao.LikeDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.entity.FavoriteReportEntity
import com.example.fishing.data.local.entity.LikeEntity
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
    private lateinit var likeDao: LikeDao

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
        likeDao = database.likeDao()
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
    fun `getLikeStates combines isLiked and likesCount per user`() = runTest {
        val reportId = UUID.randomUUID()
        reportDetailsDao.insert(createReportEntity(id = reportId, userId = userBId).copy(likesCount = 10))
        likeDao.insertAll(listOf(LikeEntity(userId = userAId, reportId = reportId)))

        val forA = reportDetailsDao.getLikeStates(userAId).first().single()
        val forB = reportDetailsDao.getLikeStates(userBId).first().single()

        assertEquals(reportId, forA.id)
        assertTrue(forA.isLiked)
        assertEquals(10, forA.likesCount)
        assertFalse(forB.isLiked)
        assertEquals(10, forB.likesCount)
    }

    @Test
    fun `getLikeStates emits liked flag and count from one snapshot`() = runTest {
        val reportId = UUID.randomUUID()
        reportDetailsDao.insert(createReportEntity(id = reportId, userId = userBId).copy(likesCount = 3))

        database.withTransaction {
            likeDao.insertAll(listOf(LikeEntity(userId = userBId, reportId = reportId)))
            reportDetailsDao.insert(createReportEntity(id = reportId, userId = userBId).copy(likesCount = 4))
        }

        val row = reportDetailsDao.getLikeStates(userBId).first().single()
        assertTrue(row.isLiked)
        assertEquals(4, row.likesCount)
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

    @Test
    fun `updateLikesCount updates only the counter`() = runTest {
        val id = UUID.randomUUID()
        reportDetailsDao.insert(createReportEntity(id = id, userId = userAId).copy(likesCount = 3))

        reportDetailsDao.updateLikesCount(id, 42)

        val fetched = reportDetailsDao.getByIdOneShot(id)
        assertEquals(42, fetched?.likesCount)
        assertEquals("Test Report", fetched?.name)
    }

    @Test
    fun `getLikesCounts returns only requested reports`() = runTest {
        val idA = UUID.randomUUID()
        val idB = UUID.randomUUID()
        reportDetailsDao.insertAll(
            listOf(
                createReportEntity(id = idA, userId = userAId).copy(likesCount = 5),
                createReportEntity(id = idB, userId = userAId).copy(likesCount = 9)
            )
        )

        val counts = reportDetailsDao.getLikesCounts(listOf(idA))
            .associate { it.id to it.likesCount }

        assertEquals(mapOf(idA to 5), counts)
    }

    @Test
    fun `logout deleteAll clears all report details including foreign favorited ones`() = runTest {
        val reportOwn = createReportEntity(id = UUID.randomUUID(), userId = userAId)
        val reportForeign = createReportEntity(id = UUID.randomUUID(), userId = userBId)
        reportDetailsDao.insertAll(listOf(reportOwn, reportForeign))
        favoriteReportDao.insertAll(listOf(
            FavoriteReportEntity(userId = userAId, reportId = reportForeign.id),
            FavoriteReportEntity(userId = userBId, reportId = reportOwn.id)
        ))

        reportDetailsDao.deleteAll()
        favoriteReportDao.deleteAll()

        // No user data may survive logout, including foreign favorited reports.
        assertTrue(reportDetailsDao.getHomeReports(userAId).first().isEmpty())
        assertTrue(reportDetailsDao.getHomeReports(userBId).first().isEmpty())
        assertTrue(reportDetailsDao.getFavorites(userAId).first().isEmpty())
        assertTrue(reportDetailsDao.getFavorites(userBId).first().isEmpty())
    }
}
