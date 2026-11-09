package com.example.fishing.data

import android.app.Application
import androidx.room.Room
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.entity.MarkerEntity
import com.example.fishing.data.report.RemoteReport
import com.example.fishing.data.report.RemoteReportPage
import com.example.fishing.data.report.ReportRemoteQueries
import com.example.fishing.data.report.SupabasePhotoUrlProvider
import com.example.fishing.data.supabase.FishingDto
import com.example.fishing.data.supabase.ProfileDto
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.MainDispatcherRule
import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class SupabaseFishingRepositoryReadTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var auth: FakeAuthRepository
    private lateinit var queries: FakeQueries
    private lateinit var repository: SupabaseFishingRepository

    private val user = User(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        name = "Test User",
        email = "test@example.com",
        image = ""
    )

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        auth = FakeAuthRepository().apply { sessionUser = user }
        queries = FakeQueries()
        repository = SupabaseFishingRepository(
            supabase = mockk<SupabaseClient>(relaxed = true),
            authRepository = auth,
            reportRemoteDataSource = mockk(relaxed = true),
            reportRemoteQueries = queries,
            markerDao = db.markerDao(),
            reportDetailsDao = db.reportDetailsDao(),
            favoriteReportDao = db.favoriteDao(),
            likeDao = db.likeDao(),
            database = db,
            photoUrlProvider = SupabasePhotoUrlProvider(mockk(relaxed = true)),
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `refreshHomeReports caches the first page and reports pagination state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val first = UUID.randomUUID()
            val second = UUID.randomUUID()
            queries.page = RemoteReportPage(
                reports = listOf(remoteReport(first), remoteReport(second)),
                hasMore = true
            )

            val result = repository.refreshHomeReports(user.id)

            assertTrue(result.hasMore)
            assertEquals(2, result.loadedCount)
            assertEquals(listOf(0 to 20), queries.pageRequests)
            assertEquals(
                setOf(first, second),
                repository.getHomeReports(user.id).first().map { it.id }.toSet()
            )
        }

    @Test
    fun `loadMoreHomeReports requests the next page and appends it`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val first = UUID.randomUUID()
            val second = UUID.randomUUID()
            queries.page = RemoteReportPage(listOf(remoteReport(first)), hasMore = true)
            repository.refreshHomeReports(user.id)

            queries.page = RemoteReportPage(listOf(remoteReport(second)), hasMore = false)
            val hasMore = repository.loadMoreHomeReports(user.id)

            assertFalse(hasMore)
            assertEquals(listOf(0 to 20, 1 to 20), queries.pageRequests)
            assertEquals(
                listOf(first, second).toSet(),
                repository.getHomeReports(user.id).first().map { it.id }.toSet()
            )
        }

    @Test
    fun `loadMoreHomeReports does nothing when the first page was the last one`() =
        runTest(mainDispatcherRule.testDispatcher) {
            queries.page = RemoteReportPage(listOf(remoteReport(UUID.randomUUID())), hasMore = false)
            repository.refreshHomeReports(user.id)

            assertFalse(repository.loadMoreHomeReports(user.id))

            assertEquals(listOf(0 to 20), queries.pageRequests)
        }

    @Test
    fun `refreshReportDetails caches an existing report`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val id = UUID.randomUUID()
            queries.reportsById[id] = remoteReport(id)

            assertTrue(repository.refreshReportDetails(id))

            assertEquals(id, db.reportDetailsDao().getByIdOneShot(id)?.id)
        }

    @Test
    fun `refreshReportDetails reports a missing report`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val id = UUID.randomUUID()

            assertFalse(repository.refreshReportDetails(id))

            assertEquals(null, db.reportDetailsDao().getByIdOneShot(id))
        }

    @Test
    fun `refreshLikes rewrites the local like cache from the server snapshot`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val liked = UUID.randomUUID()
            queries.reportsById[liked] = remoteReport(liked)
            repository.refreshReportDetails(liked)
            queries.likes = listOf(liked)

            repository.refreshLikes(user.id)

            val states = repository.getLikeStates(user.id).first()
            assertTrue(states.getValue(liked).isLiked)
        }

    @Test
    fun `refreshMapMarkers replaces the cached markers`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val marker = marker(UUID.randomUUID())
            queries.markers = listOf(marker)

            val result = repository.refreshMapMarkers()

            assertTrue(result.isSuccess)
            assertEquals(listOf(marker.id), repository.getMapMarkers().first().map { it.id })
        }

    private fun remoteReport(id: UUID, likesCount: Int = 0): RemoteReport = RemoteReport(
        fishing = FishingDto(
            id = id,
            userId = user.id,
            type = "FISHING_LOG",
            name = "Спиннинг • Щука",
            fishingStartAt = "2026-11-07T16:03:49Z",
            likesCount = likesCount
        ),
        fish = emptyList(),
        baits = emptyList(),
        photos = emptyList(),
        author = ProfileDto(id = user.id, name = "Test User")
    )

    private fun marker(id: UUID): MarkerEntity = MarkerEntity(
        id = id,
        name = "Спиннинг",
        waterName = "Озеро",
        waterLat = 54.0,
        waterLng = 26.0,
        type = "FISHING_LOG",
        fishingMethod = "SPINNING",
        fishingStartAt = "2026-11-07T16:03:49Z",
        isPublic = true,
        isPaidWater = false,
        fishNames = listOf("Щука")
    )

    private class FakeQueries : ReportRemoteQueries {
        var page: RemoteReportPage = RemoteReportPage(emptyList(), hasMore = false)
        val pageRequests = mutableListOf<Pair<Int, Int>>()
        val reportsById = mutableMapOf<UUID, RemoteReport>()
        var favorites: List<UUID> = emptyList()
        var likes: List<UUID> = emptyList()
        var markers: List<MarkerEntity> = emptyList()

        override suspend fun fetchOwnReportPage(
            userId: UUID,
            offset: Int,
            limit: Int
        ): RemoteReportPage {
            pageRequests += offset to limit
            return page
        }

        override suspend fun fetchReportsByIds(ids: List<UUID>): List<RemoteReport> =
            ids.mapNotNull { reportsById[it] }

        override suspend fun fetchReport(id: UUID): RemoteReport? = reportsById[id]

        override suspend fun fetchFavoriteReportIds(userId: UUID): List<UUID> = favorites

        override suspend fun fetchLikedReportIds(userId: UUID): List<UUID> = likes

        override suspend fun fetchMapMarkers(userId: UUID): List<MarkerEntity> = markers
    }
}
