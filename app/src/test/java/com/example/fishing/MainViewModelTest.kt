package com.example.fishing

import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
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
import kotlinx.coroutines.CompletableDeferred
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
            photo = emptyList(),
            fishingStartAt = Instant.now().minusSeconds(3600),
            weight = 1.0,
            fish = listOf(Fish(name = "Pike", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "Test comment",
            user = testUser,
            fishingFromTheShore = true,
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
        assertNull(vm.currentReport.value)
        assertNull(vm.error.value)
        assertFalse(vm.reportUnavailable.value)
        assertTrue(vm.isInitialLoading.value)
        assertFalse(vm.isRefreshing.value)
        assertEquals(0, vm.selectedTab.value)
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
}
