package com.example.fishing.viewmodel.detail

import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.testReport
import com.example.fishing.viewmodel.ReportDetailUiState
import com.example.fishing.viewmodel.photos.PhotoUrlResolver
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDetailCoordinatorTest {

    @Test
    fun `loads report details from the cache`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val report = testReport()
        repository.reportDetailsValue = report
        val coordinator = ReportDetailCoordinator(
            repository = repository,
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )

        coordinator.loadReportDetails(report.id)
        advanceUntilIdle()

        val state = coordinator.reportDetailUiState.value
        assertTrue(state is ReportDetailUiState.Success)
        assertEquals(report.id, (state as ReportDetailUiState.Success).report.id)
    }

    @Test
    fun `absent report resolves to empty`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val missingId = UUID.randomUUID()
        val coordinator = ReportDetailCoordinator(
            repository = repository,
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )

        coordinator.loadReportDetails(missingId)
        advanceUntilIdle()

        assertEquals(ReportDetailUiState.Empty(missingId), coordinator.reportDetailUiState.value)
    }

    @Test
    fun `delete success publishes the deleted id`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val report = testReport()
        repository.homeReportsValue = listOf(report)
        val coordinator = ReportDetailCoordinator(
            repository = repository,
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )

        coordinator.deleteReport(report.id)
        advanceUntilIdle()

        assertEquals(1, repository.deleteReportCallCount)
        assertEquals(report.id, coordinator.deletedReportId.value)
        assertFalse(coordinator.isDeletingReport.value)
        assertNull(coordinator.deleteReportError.value)
    }

    @Test
    fun `visibility change updates the repository with the target state`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeFishingRepository()
            val report = testReport(isPublic = true)
            repository.reportDetailsValue = report
            repository.homeReportsValue = listOf(report)
            val coordinator = ReportDetailCoordinator(
                repository = repository,
                photoUrls = PhotoUrlResolver(repository),
                scope = backgroundScope
            )

            coordinator.updateVisibility(report.id, isPublic = false)
            advanceUntilIdle()

            assertEquals(1, repository.updateReportVisibilityCallCount)
            assertEquals(report.id, repository.lastVisibilityReportId)
            assertEquals(false, repository.lastVisibilityIsPublic)
            assertFalse(coordinator.isUpdatingVisibility.value)
            assertNull(coordinator.visibilityError.value)
            assertEquals("Отчёт теперь виден только вам", coordinator.visibilityResult.value)
        }

    @Test
    fun `visibility failure surfaces a message and stops the spinner`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeFishingRepository()
            val report = testReport(isPublic = false)
            repository.reportDetailsValue = report
            repository.updateReportVisibilityException = RuntimeException("offline")
            val coordinator = ReportDetailCoordinator(
                repository = repository,
                photoUrls = PhotoUrlResolver(repository),
                scope = backgroundScope
            )

            coordinator.updateVisibility(report.id, isPublic = true)
            advanceUntilIdle()

            assertFalse(coordinator.isUpdatingVisibility.value)
            assertTrue(coordinator.visibilityError.value?.isNotBlank() == true)
            assertNull(coordinator.visibilityResult.value)
        }

    @Test
    fun `clear resets detail delete and error state`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val report = testReport()
        repository.reportDetailsValue = report
        repository.deleteReportException = RuntimeException("offline")
        val coordinator = ReportDetailCoordinator(
            repository = repository,
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )

        coordinator.loadReportDetails(report.id)
        advanceUntilIdle()
        coordinator.deleteReport(report.id)
        advanceUntilIdle()

        coordinator.clear()

        assertEquals(ReportDetailUiState.Loading, coordinator.reportDetailUiState.value)
        assertFalse(coordinator.isDeletingReport.value)
        assertNull(coordinator.deleteReportError.value)
        assertNull(coordinator.deletedReportId.value)
    }
}
