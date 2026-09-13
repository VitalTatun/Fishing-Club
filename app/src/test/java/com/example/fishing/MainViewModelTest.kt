package com.example.fishing

import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.MarkerDomain
import com.example.fishing.model.ReportLikeState
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.model.User
import com.example.fishing.model.Water
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.Bait
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.FakeUserPreferencesRepository
import com.example.fishing.testutil.MainDispatcherRule
import com.example.fishing.viewmodel.MainViewModel
import com.example.fishing.viewmodel.ReportDetailUiState
import com.example.fishing.viewmodel.HomeUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeFishingRepository: FakeFishingRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    private val testUserId = UUID.randomUUID()
    private val testUser = User(
        id = testUserId,
        name = "Test User",
        email = "test@example.com",
        image = ""
    )

    private fun createReport(id: UUID = UUID.randomUUID(), userId: UUID = testUserId): FishingReport {
        return FishingReport(
            id = id,
            userId = userId,
            type = FishingType.FISHING_LOG,
            name = "Test Report",
            water = Water(waterName = "Test Lake", latitude = 55.0, longitude = 37.0),
            photos = emptyList(),
            fishingStartAt = Instant.now().minusSeconds(3600),
            weight = 1.0,
            fish = listOf(Fish(id = UUID.randomUUID(), name = "Pike", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Test comment",
            user = testUser,
            fishingFromTheShore = true,
            isPublic = true
        )
    }

    private fun marker(id: UUID = UUID.randomUUID()): MarkerDomain {
        return MarkerDomain(
            id = id,
            name = "Test Report",
            waterName = "Test Lake",
            waterLat = 55.0,
            waterLng = 37.0,
            type = FishingType.FISHING_LOG,
            fishingMethod = FishingMethod.SPINNING,
            fishingStartAt = null,
            isPublic = true
        )
    }

    @Before
    fun setup() {
        fakeAuthRepository = FakeAuthRepository()
        fakeFishingRepository = FakeFishingRepository()
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        fakeAuthRepository.sessionUser = testUser
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(
            repository = fakeFishingRepository,
            authRepository = fakeAuthRepository,
            userPreferencesRepository = fakeUserPreferencesRepository
        )
    }

    @Test
    fun `authenticated user triggers home loading`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.homeReportsValue = listOf(report)

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.refreshHomeReportsCallCount)
        assertEquals(listOf(report), vm.reports.value)
    }

    @Test
    fun `logout clears all user content`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.homeReportsValue = listOf(report)

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.reports.value.size)

        fakeAuthRepository.sessionUser = null
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.reports.value.isEmpty())
        assertTrue(vm.favoriteReports.value.isEmpty())
        assertTrue(vm.mapMarkers.value.isEmpty())
        assertEquals(ReportDetailUiState.Loading, vm.reportDetailUiState.value)
        assertNull(vm.error.value)
        assertTrue(vm.isInitialLoading.value)
        assertFalse(vm.isRefreshing.value)
        assertEquals(0, vm.selectedTab.value)
    }

    @Test
    fun `report detail - opening a report starts from Loading synchronously`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.reportDetailsValue = report

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ReportDetailUiState.Loading, vm.reportDetailUiState.value)

        vm.loadReportDetails(report.id)
        assertEquals(ReportDetailUiState.Loading, vm.reportDetailUiState.value)

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.reportDetailUiState.value
        assertTrue(state is ReportDetailUiState.Success)
        assertEquals(report.id, (state as ReportDetailUiState.Success).report.id)
    }

    @Test
    fun `report detail - absent report resolves to Empty only after load completes`() = runTest {
        fakeAuthRepository.sessionUser = testUser

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.loadReportDetails(UUID.randomUUID())
        assertEquals(ReportDetailUiState.Loading, vm.reportDetailUiState.value)

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.reportDetailUiState.value is ReportDetailUiState.Empty)
    }

    @Test
    fun `report detail - opening another report resets stale Empty to Loading`() = runTest {
        fakeAuthRepository.sessionUser = testUser

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val absentId = UUID.randomUUID()
        vm.loadReportDetails(absentId)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.reportDetailUiState.value is ReportDetailUiState.Empty)

        val report = createReport()
        fakeFishingRepository.reportDetailsValue = report
        vm.loadReportDetails(report.id)
        assertEquals(ReportDetailUiState.Loading, vm.reportDetailUiState.value)

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.reportDetailUiState.value is ReportDetailUiState.Success)
    }

    @Test
    fun `refresh sets isRefreshing then clears it`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.homeReportsValue = listOf(report)

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.isInitialLoading.value)
        assertEquals(1, vm.reports.value.size)

        // Now set the gate BEFORE triggering refresh
        val refreshGate = CompletableDeferred<Unit>()
        fakeFishingRepository.refreshHomeReportsGate = refreshGate

        // Trigger refresh — the gate prevents it from completing
        vm.refresh()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.isRefreshing.value)

        // Release the gate
        refreshGate.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.isRefreshing.value)
    }

    @Test
    fun `initial loading is false after load completes`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.isInitialLoading.value)
    }

    @Test
    fun `error state shows error message`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        fakeFishingRepository.refreshHomeReportsException = RuntimeException("Network error")

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.error.value)
        assertTrue(vm.error.value!!.contains("Network error"))
    }

    @Test
    fun `delete own report removes it`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.homeReportsValue = listOf(report)

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.deleteReport(report.id)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.deleteReportCallCount)
        assertFalse(vm.isDeletingReport.value)
        assertEquals(report.id, vm.deletedReportId.value)
        assertEquals(0, vm.reports.value.size)
    }

    @Test
    fun `delete failure publishes error and allows retry`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.homeReportsValue = listOf(report)
        fakeFishingRepository.deleteReportException = RuntimeException("Network error")

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.deleteReport(report.id)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.deleteReportCallCount)
        assertNotNull(vm.deleteReportError.value)
        assertNull(vm.deletedReportId.value)
        assertFalse(vm.isDeletingReport.value)
        // No false success — the report is still present.
        assertEquals(1, vm.reports.value.size)

        // Retry after clearing the failure must succeed.
        fakeFishingRepository.deleteReportException = null
        vm.deleteReport(report.id)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeFishingRepository.deleteReportCallCount)
        assertNull(vm.deleteReportError.value)
        assertEquals(report.id, vm.deletedReportId.value)
        assertEquals(0, vm.reports.value.size)
    }

    @Test
    fun `duplicate delete while in-flight is ignored`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.homeReportsValue = listOf(report)

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val deleteGate = CompletableDeferred<Unit>()
        fakeFishingRepository.deleteReportGate = deleteGate

        vm.deleteReport(report.id)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.isDeletingReport.value)
        assertEquals(1, fakeFishingRepository.deleteReportCallCount)

        // Second tap while deletion is in flight must not start another operation.
        vm.deleteReport(report.id)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeFishingRepository.deleteReportCallCount)

        // Release the gate — the first deletion completes.
        deleteGate.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.isDeletingReport.value)
        assertEquals(report.id, vm.deletedReportId.value)
    }

    @Test
    fun `add favorite updates favorite list`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val report = createReport()
        vm.toggleFavorite(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.addFavoriteCallCount)
        assertEquals(1, vm.favoriteReports.value.size)
        assertEquals(report.id, vm.favoriteReports.value[0].id)
    }

    @Test
    fun `remove favorite updates favorite list`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.favoriteReportsValue = listOf(report)

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleFavorite(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.removeFavoriteCallCount)
        assertTrue(vm.favoriteReports.value.isEmpty())
    }

    @Test
    fun `duplicate favorite toggle while in-flight is ignored`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val addGate = CompletableDeferred<Unit>()
        fakeFishingRepository.addFavoriteGate = addGate

        vm.toggleFavorite(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.addFavoriteCallCount)

        // Second tap while the first add is still in flight must not start another operation.
        vm.toggleFavorite(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeFishingRepository.addFavoriteCallCount)
        assertTrue(vm.favoriteReports.value.isEmpty())

        // Release the gate — the first add completes and the guard is released.
        addGate.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.favoriteReports.value.size)
        assertEquals(report.id, vm.favoriteReports.value[0].id)

        // A new tap after completion starts a fresh operation.
        vm.toggleFavorite(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeFishingRepository.removeFavoriteCallCount)
        assertTrue(vm.favoriteReports.value.isEmpty())
    }

    @Test
    fun `concurrent favorite toggles on different reports are independent`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val reportA = createReport()
        val reportB = createReport(userId = UUID.randomUUID())

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val addGate = CompletableDeferred<Unit>()
        fakeFishingRepository.addFavoriteGate = addGate

        vm.toggleFavorite(reportA)
        vm.toggleFavorite(reportB)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        addGate.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeFishingRepository.addFavoriteCallCount)
        assertEquals(2, vm.favoriteReports.value.size)
    }

    @Test
    fun `favorite add failure publishes error and allows retry`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.addFavoriteException = RuntimeException("Network error")

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleFavorite(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.addFavoriteCallCount)
        assertNotNull(vm.favoriteError.value)
        assertTrue(vm.favoriteError.value!!.contains("Network error"))
        // No false success — the report must not appear as favorited.
        assertTrue(vm.favoriteReports.value.isEmpty())

        // Retry after clearing the failure must succeed.
        fakeFishingRepository.addFavoriteException = null
        vm.toggleFavorite(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeFishingRepository.addFavoriteCallCount)
        assertNull(vm.favoriteError.value)
        assertEquals(report.id, vm.favoriteReports.value[0].id)
    }

    @Test
    fun `favorite remove failure publishes error and allows retry`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.favoriteReportsValue = listOf(report)
        fakeFishingRepository.removeFavoriteException = RuntimeException("Network error")

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleFavorite(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.removeFavoriteCallCount)
        assertNotNull(vm.favoriteError.value)
        assertTrue(vm.favoriteError.value!!.contains("Network error"))
        // No false success — the report must remain favorited.
        assertEquals(1, vm.favoriteReports.value.size)

        // Retry after clearing the failure must succeed.
        fakeFishingRepository.removeFavoriteException = null
        vm.toggleFavorite(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeFishingRepository.removeFavoriteCallCount)
        assertNull(vm.favoriteError.value)
        assertTrue(vm.favoriteReports.value.isEmpty())
    }

    @Test
    fun `sort order changes when set`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ReportSortOrder.BY_FISHING_TIME, vm.reportSortOrder.value)

        vm.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)
        assertEquals(ReportSortOrder.BY_PUBLISH_DATE, vm.reportSortOrder.value)
    }

    @Test
    fun `user A sort order isolated from user B`() = runTest {
        val userA = User(id = UUID.randomUUID(), name = "A", email = "a@test.com", image = "")
        val userB = User(id = UUID.randomUUID(), name = "B", email = "b@test.com", image = "")

        fakeAuthRepository.sessionUser = userA
        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)
        assertEquals(ReportSortOrder.BY_PUBLISH_DATE, fakeUserPreferencesRepository.getSortOrder(userA.id))

        fakeAuthRepository.sessionUser = null
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        fakeAuthRepository.sessionUser = userB
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ReportSortOrder.BY_FISHING_TIME, vm.reportSortOrder.value)
        assertEquals(ReportSortOrder.BY_FISHING_TIME, fakeUserPreferencesRepository.getSortOrder(userB.id))
    }

    @Test
    fun `logout then login as different user gets fresh state`() = runTest {
        val userA = User(id = UUID.randomUUID(), name = "User A", email = "a@test.com", image = "")
        fakeAuthRepository.sessionUser = userA

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val reportA = createReport(userId = userA.id)
        fakeFishingRepository.homeReportsValue = listOf(reportA)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.reports.value.size)

        // User A logs out — clear the fake's data too (simulates Room cache clear)
        fakeAuthRepository.sessionUser = null
        fakeFishingRepository.homeReportsValue = emptyList()
        fakeFishingRepository.favoriteReportsValue = emptyList()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.reports.value.isEmpty())

        // User B logs in with their own reports
        val userB = User(id = UUID.randomUUID(), name = "User B", email = "b@test.com", image = "")
        val reportB = createReport(userId = userB.id)
        fakeAuthRepository.sessionUser = userB
        fakeFishingRepository.homeReportsValue = listOf(reportB)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.reports.value.size)
        assertEquals(userB.id, vm.reports.value[0].userId)
        assertTrue(vm.reports.value.none { it.userId == userA.id })
    }

    // --- Map states (P0.7) ---

    @Test
    fun `map case A - no cache stays loading while refresh is in progress`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val gate = CompletableDeferred<Unit>()
        fakeFishingRepository.refreshMapMarkersGate = gate

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.mapIsLoading.value)
        assertTrue(vm.mapMarkers.value.isEmpty())
        assertNull(vm.mapRefreshError.value)

        gate.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.mapIsLoading.value)
    }

    @Test
    fun `map case B - successful refresh with markers is content`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val m = marker()
        fakeFishingRepository.mapMarkersValue = listOf(m)

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.mapIsLoading.value)
        assertEquals(listOf(m), vm.mapMarkers.value)
        assertNull(vm.mapRefreshError.value)
        assertEquals(1, fakeFishingRepository.refreshMapMarkersCallCount)
    }

    @Test
    fun `map case C - successful refresh with zero markers is empty`() = runTest {
        fakeAuthRepository.sessionUser = testUser

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.mapIsLoading.value)
        assertTrue(vm.mapMarkers.value.isEmpty())
        assertNull(vm.mapRefreshError.value)
    }

    @Test
    fun `map case D - cached markers stay when refresh fails`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val m = marker()
        fakeFishingRepository.mapMarkersValue = listOf(m)
        fakeFishingRepository.refreshMapMarkersException = RuntimeException("Network error")

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(m), vm.mapMarkers.value)
        assertFalse(vm.mapIsLoading.value)
        assertNotNull(vm.mapRefreshError.value)
    }

    @Test
    fun `map case E - no cache and refresh failure is error`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        fakeFishingRepository.mapMarkersValue = emptyList()
        fakeFishingRepository.refreshMapMarkersException = RuntimeException("Network error")

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.mapMarkers.value.isEmpty())
        assertFalse(vm.mapIsLoading.value)
        assertNotNull(vm.mapRefreshError.value)
    }

    @Test
    fun `map refresh - retry clears error after a successful reload`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        fakeFishingRepository.mapMarkersValue = emptyList()
        fakeFishingRepository.refreshMapMarkersException = RuntimeException("Network error")

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.mapRefreshError.value)

        fakeFishingRepository.refreshMapMarkersException = null
        fakeFishingRepository.mapMarkersValue = listOf(marker())

        vm.loadMapMarkers(force = true)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.mapRefreshError.value)
        assertFalse(vm.mapIsLoading.value)
        assertTrue(vm.mapMarkers.value.isNotEmpty())
    }

    @Test
    fun `map refresh - logout resets map state`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        fakeFishingRepository.mapMarkersValue = listOf(marker())
        fakeFishingRepository.refreshMapMarkersException = RuntimeException("Network error")

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.mapMarkers.value.isNotEmpty())
        assertNotNull(vm.mapRefreshError.value)

        fakeAuthRepository.sessionUser = null
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.mapMarkers.value.isEmpty())
        assertNull(vm.mapRefreshError.value)
        assertFalse(vm.mapIsRefreshing.value)
        assertTrue(vm.mapIsLoading.value)
    }

    // --- Map tab selection (P1.1: no duplicate refresh) ---

    @Test
    fun `selecting map tab does not duplicate refresh when markers are already loaded`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val m = marker()
        fakeFishingRepository.mapMarkersValue = listOf(m)

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.refreshMapMarkersCallCount)

        vm.selectTab(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.refreshMapMarkersCallCount)
        assertEquals(listOf(m), vm.mapMarkers.value)
    }

    @Test
    fun `selecting map tab while initial refresh is in flight does not start a second refresh`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val gate = CompletableDeferred<Unit>()
        fakeFishingRepository.refreshMapMarkersGate = gate

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.mapIsLoading.value)
        assertEquals(1, fakeFishingRepository.refreshMapMarkersCallCount)

        vm.selectTab(1)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.refreshMapMarkersCallCount)

        gate.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.refreshMapMarkersCallCount)
        assertFalse(vm.mapIsLoading.value)
    }

    // --- HomeUiState tests ---

    @Test
    fun `empty cache and successful refresh with zero reports emits Empty`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        fakeFishingRepository.homeReportsValue = emptyList()

        val vm = createViewModel()
        val job = launch { vm.homeUiState.collect {} }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(HomeUiState.Empty, vm.homeUiState.value)
        job.cancel()
    }

    @Test
    fun `empty cache and network error emits Error`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        fakeFishingRepository.homeReportsValue = emptyList()
        fakeFishingRepository.refreshHomeReportsException = RuntimeException("Network Error")

        val vm = createViewModel()
        val job = launch { vm.homeUiState.collect {} }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.homeUiState.value is HomeUiState.Error)
        assertEquals("Ошибка загрузки: Network Error", (vm.homeUiState.value as HomeUiState.Error).message)
        job.cancel()
    }

    @Test
    fun `non empty cache and network error stays Success`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.homeReportsValue = listOf(report)
        fakeFishingRepository.refreshHomeReportsException = RuntimeException("Network Error")

        val vm = createViewModel()
        val job = launch { vm.homeUiState.collect {} }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.homeUiState.value is HomeUiState.Success)
        assertEquals(listOf(report), (vm.homeUiState.value as HomeUiState.Success).reports)
        job.cancel()
    }

    @Test
    fun `non empty cache and successful refresh emits Success`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = createReport()
        fakeFishingRepository.homeReportsValue = listOf(report)

        val vm = createViewModel()
        val job = launch { vm.homeUiState.collect {} }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.homeUiState.value is HomeUiState.Success)
        assertEquals(listOf(report), (vm.homeUiState.value as HomeUiState.Success).reports)
        job.cancel()
    }

    @Test
    fun `initial loading is Loading until first refresh completed when empty`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        fakeFishingRepository.homeReportsValue = emptyList()
        val gate = CompletableDeferred<Unit>()
        fakeFishingRepository.refreshHomeReportsGate = gate

        val vm = createViewModel()
        val job = launch { vm.homeUiState.collect {} }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(HomeUiState.Loading, vm.homeUiState.value)

        gate.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(HomeUiState.Empty, vm.homeUiState.value)
        job.cancel()
    }

    // --- Likes (P0) ---

    private fun otherUserReport(id: UUID = UUID.randomUUID()): FishingReport {
        return createReport(id = id, userId = UUID.randomUUID())
    }

    @Test
    fun `like success applies optimistic state then confirms it`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = otherUserReport()
        fakeFishingRepository.likeStatesValue = mapOf(report.id to ReportLikeState(report.id, false, 10))

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ReportLikeState(report.id, false, 10), vm.likeStates.value[report.id])

        vm.toggleLike(report)
        // Optimistic state is applied synchronously, before the RPC completes.
        assertEquals(ReportLikeState(report.id, true, 11), vm.likeStates.value[report.id])

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.addLikeCallCount)
        assertEquals(0, fakeFishingRepository.removeLikeCallCount)
        assertEquals(ReportLikeState(report.id, true, 11), vm.likeStates.value[report.id])
        assertNull(vm.likeError.value)
    }

    @Test
    fun `unlike success decrements count`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = otherUserReport()
        fakeFishingRepository.likeStatesValue = mapOf(report.id to ReportLikeState(report.id, true, 10))

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleLike(report)
        assertEquals(ReportLikeState(report.id, false, 9), vm.likeStates.value[report.id])

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.removeLikeCallCount)
        assertEquals(0, fakeFishingRepository.addLikeCallCount)
        assertEquals(ReportLikeState(report.id, false, 9), vm.likeStates.value[report.id])
        assertNull(vm.likeError.value)
    }

    @Test
    fun `like failure rolls back optimistic state and publishes error`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = otherUserReport()
        fakeFishingRepository.likeStatesValue = mapOf(report.id to ReportLikeState(report.id, false, 10))
        fakeFishingRepository.addLikeException = RuntimeException("Network error")

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleLike(report)
        assertEquals(ReportLikeState(report.id, true, 11), vm.likeStates.value[report.id])

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.addLikeCallCount)
        assertEquals(ReportLikeState(report.id, false, 10), vm.likeStates.value[report.id])
        assertNotNull(vm.likeError.value)
        assertTrue(vm.likeError.value!!.contains("Network error"))

        // Retry after clearing the failure must succeed and clear the error.
        fakeFishingRepository.addLikeException = null
        vm.toggleLike(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeFishingRepository.addLikeCallCount)
        assertNull(vm.likeError.value)
        assertEquals(ReportLikeState(report.id, true, 11), vm.likeStates.value[report.id])
    }

    @Test
    fun `duplicate like toggle while in-flight is ignored`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = otherUserReport()
        fakeFishingRepository.likeStatesValue = mapOf(report.id to ReportLikeState(report.id, false, 10))

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val likeGate = CompletableDeferred<Unit>()
        fakeFishingRepository.addLikeGate = likeGate

        vm.toggleLike(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.addLikeCallCount)
        assertEquals(ReportLikeState(report.id, true, 11), vm.likeStates.value[report.id])

        // Second tap while the first op is still in flight must not start another operation.
        vm.toggleLike(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeFishingRepository.addLikeCallCount)

        likeGate.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.addLikeCallCount)
        assertEquals(ReportLikeState(report.id, true, 11), vm.likeStates.value[report.id])
    }

    @Test
    fun `self-like never calls repository`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val ownReport = createReport(userId = testUserId)

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        vm.toggleLike(ownReport)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeFishingRepository.addLikeCallCount)
        assertEquals(0, fakeFishingRepository.removeLikeCallCount)
        assertNull(vm.likeStates.value[ownReport.id])
        assertNull(vm.likeError.value)
    }

    @Test
    fun `logout clears like state`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = otherUserReport()
        fakeFishingRepository.likeStatesValue = mapOf(report.id to ReportLikeState(report.id, true, 11))

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ReportLikeState(report.id, true, 11), vm.likeStates.value[report.id])

        fakeAuthRepository.sessionUser = null
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.likeStates.value.isEmpty())
        assertNull(vm.likeError.value)
    }

    @Test
    fun `late like result after logout does not restore state`() = runTest {
        fakeAuthRepository.sessionUser = testUser
        val report = otherUserReport()
        fakeFishingRepository.likeStatesValue = mapOf(report.id to ReportLikeState(report.id, false, 10))

        val vm = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val likeGate = CompletableDeferred<Unit>()
        fakeFishingRepository.addLikeGate = likeGate

        vm.toggleLike(report)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ReportLikeState(report.id, true, 11), vm.likeStates.value[report.id])

        // Log out while the RPC is still in flight.
        fakeAuthRepository.sessionUser = null
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.likeStates.value.isEmpty())

        likeGate.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.addLikeCallCount)
        assertTrue(vm.likeStates.value.isEmpty())
        assertNull(vm.likeError.value)
    }
}
