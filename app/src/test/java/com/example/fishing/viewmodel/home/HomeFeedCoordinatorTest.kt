package com.example.fishing.viewmodel.home

import com.example.fishing.model.ReportDisplayMode
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.FakeUserPreferencesRepository
import com.example.fishing.testutil.testReport
import com.example.fishing.testutil.testUser
import com.example.fishing.viewmodel.HomeUiState
import com.example.fishing.viewmodel.photos.PhotoUrlResolver
import com.example.fishing.viewmodel.session.UserSession
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class HomeFeedCoordinatorTest {

    @Test
    fun `loads cached reports and emits success`() = runTest(UnconfinedTestDispatcher()) {
        val auth = FakeAuthRepository().apply { sessionUser = testUser() }
        val repository = FakeFishingRepository()
        val report = testReport()
        repository.homeReportsValue = listOf(report)
        val coordinator = HomeFeedCoordinator(
            repository = repository,
            userPreferencesRepository = FakeUserPreferencesRepository(),
            session = UserSession(auth),
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )
        val stateJob = backgroundScope.launch { coordinator.homeUiState.collect {} }

        coordinator.loadReports(force = false)
        advanceUntilIdle()

        assertEquals(1, repository.refreshHomeReportsCallCount)
        assertEquals(listOf(report), coordinator.reports.value)
        assertFalse(coordinator.isInitialLoading.value)
        assertTrue(coordinator.homeUiState.value is HomeUiState.Success)
        stateJob.cancel()
    }

    @Test
    fun `without a session loading stops with an error`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val coordinator = HomeFeedCoordinator(
            repository = repository,
            userPreferencesRepository = FakeUserPreferencesRepository(),
            session = UserSession(FakeAuthRepository()),
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )

        coordinator.loadReports(force = false)
        advanceUntilIdle()

        assertFalse(coordinator.isInitialLoading.value)
        assertEquals("Нет активной сессии. Войдите снова.", coordinator.error.value)
        assertNull(coordinator.reports.value.firstOrNull())
        assertEquals(0, repository.refreshHomeReportsCallCount)
    }

    @Test
    fun `sorted reports follow the selected order`() = runTest(UnconfinedTestDispatcher()) {
        val auth = FakeAuthRepository().apply { sessionUser = testUser() }
        val repository = FakeFishingRepository()
        val older = testReport(publishedAt = Date(1_000), createdAt = Date(1_000))
        val newer = testReport(publishedAt = Date(2_000), createdAt = Date(2_000))
        repository.homeReportsValue = listOf(older, newer)
        val coordinator = HomeFeedCoordinator(
            repository = repository,
            userPreferencesRepository = FakeUserPreferencesRepository(),
            session = UserSession(auth),
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )
        val sortedJob = backgroundScope.launch { coordinator.sortedReports.collect {} }

        coordinator.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)
        coordinator.loadReports(force = false)
        advanceUntilIdle()

        assertEquals(listOf(newer, older), coordinator.sortedReports.value)
        sortedJob.cancel()
    }

    @Test
    fun `clear resets the feed but keeps the selected sort order`() = runTest(UnconfinedTestDispatcher()) {
        val auth = FakeAuthRepository().apply { sessionUser = testUser() }
        val repository = FakeFishingRepository()
        repository.homeReportsValue = listOf(testReport())
        val coordinator = HomeFeedCoordinator(
            repository = repository,
            userPreferencesRepository = FakeUserPreferencesRepository(),
            session = UserSession(auth),
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )
        val stateJob = backgroundScope.launch { coordinator.homeUiState.collect {} }

        coordinator.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)
        coordinator.setDisplayMode(ReportDisplayMode.LIST)
        coordinator.loadReports(force = false)
        advanceUntilIdle()

        coordinator.clear()
        advanceUntilIdle()

        assertTrue(coordinator.reports.value.isEmpty())
        assertTrue(coordinator.isInitialLoading.value)
        assertFalse(coordinator.isRefreshing.value)
        assertFalse(coordinator.hasMoreReports.value)
        assertNull(coordinator.error.value)
        assertEquals(ReportDisplayMode.CARD, coordinator.reportDisplayMode.value)
        assertEquals(ReportSortOrder.BY_PUBLISH_DATE, coordinator.reportSortOrder.value)
        stateJob.cancel()
    }

    @Test
    fun `on user changed applies stored preferences`() = runTest(UnconfinedTestDispatcher()) {
        val user = testUser()
        val preferences = FakeUserPreferencesRepository().apply {
            setSortOrder(user.id, ReportSortOrder.BY_PUBLISH_DATE)
            setDisplayMode(user.id, ReportDisplayMode.LIST)
        }
        val repository = FakeFishingRepository()
        val coordinator = HomeFeedCoordinator(
            repository = repository,
            userPreferencesRepository = preferences,
            session = UserSession(FakeAuthRepository().apply { sessionUser = user }),
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )

        coordinator.onUserChanged(user.id)

        assertEquals(ReportSortOrder.BY_PUBLISH_DATE, coordinator.reportSortOrder.value)
        assertEquals(ReportDisplayMode.LIST, coordinator.reportDisplayMode.value)
    }
}
