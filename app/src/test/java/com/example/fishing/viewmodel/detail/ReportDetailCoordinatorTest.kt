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
