package com.example.fishing.viewmodel.user

import com.example.fishing.data.report.UserProfileData
import com.example.fishing.model.FishingPhoto
import com.example.fishing.model.ReportSortOrder
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.testReport
import com.example.fishing.testutil.testUser
import com.example.fishing.viewmodel.UserProfileUiState
import com.example.fishing.viewmodel.photos.PhotoUrlResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileCoordinatorTest {

    private fun TestScope.coordinator(repository: FakeFishingRepository) = UserProfileCoordinator(
        repository = repository,
        photoUrls = PhotoUrlResolver(repository),
        scope = backgroundScope
    )

    @Test
    fun `publishes author with aggregate counters`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val author = testUser()
        repository.userProfileResult = Result.success(
            UserProfileData(
                user = author,
                reports = listOf(
                    testReport(userId = author.id),
                    testReport(
                        userId = author.id,
                        fish = listOf(
                            com.example.fishing.model.Fish(id = UUID.randomUUID(), name = "Pike", count = 3)
                        )
                    )
                )
            )
        )

        val coordinator = coordinator(repository)
        coordinator.load(author.id)
        advanceUntilIdle()

        val state = coordinator.uiState.value
        assertTrue(state is UserProfileUiState.Success)
        state as UserProfileUiState.Success
        assertEquals(author.id, state.user?.id)
        assertEquals(2, state.reportsCount)
        // 1 fish in the first report, 3 in the second.
        assertEquals(4, state.caughtCount)
    }

    @Test
    fun `reports are newest fishing date first by default and follow the selected order`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeFishingRepository()
            val olderFishing = testReport(
                fishingStartAt = Instant.parse("2026-05-01T08:00:00Z"),
                createdAt = Date(1_800_000_000_000L)
            )
            val newerFishing = testReport(
                fishingStartAt = Instant.parse("2026-06-01T08:00:00Z"),
                createdAt = Date(1_700_000_000_000L)
            )
            repository.userProfileResult = Result.success(
                UserProfileData(user = null, reports = listOf(olderFishing, newerFishing))
            )

            val coordinator = coordinator(repository)
            coordinator.load(newerFishing.userId)
            advanceUntilIdle()

            assertEquals(ReportSortOrder.BY_FISHING_TIME, coordinator.sortOrder.value)
            assertEquals(
                listOf(newerFishing.id, olderFishing.id),
                (coordinator.uiState.value as UserProfileUiState.Success).reports.map { it.id }
            )

            // "By publish date" re-orders by createdAt, the same way the home feed does.
            coordinator.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)

            assertEquals(ReportSortOrder.BY_PUBLISH_DATE, coordinator.sortOrder.value)
            assertEquals(
                listOf(olderFishing.id, newerFishing.id),
                (coordinator.uiState.value as UserProfileUiState.Success).reports.map { it.id }
            )
        }

    @Test
    fun `returning to the same author keeps the page instead of showing a spinner`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeFishingRepository()
            val author = testUser()
            repository.userProfileResult = Result.success(
                UserProfileData(user = author, reports = listOf(testReport(userId = author.id)))
            )

            val coordinator = coordinator(repository)
            coordinator.load(author.id)
            advanceUntilIdle()
            assertTrue(coordinator.uiState.value is UserProfileUiState.Success)

            // Back from a report: the same destination loads again.
            val gate = CompletableDeferred<Unit>()
            repository.loadUserProfileGate = gate
            coordinator.load(author.id)

            // The refresh is still in flight, yet the page stays on screen.
            assertTrue(coordinator.uiState.value is UserProfileUiState.Success)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(2, repository.loadUserProfileCallCount)
            assertTrue(coordinator.uiState.value is UserProfileUiState.Success)
        }

    @Test
    fun `another author still shows the loading state`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val first = testUser()
        val second = testUser()
        repository.userProfileResult = Result.success(UserProfileData(user = first, reports = emptyList()))

        val coordinator = coordinator(repository)
        coordinator.load(first.id)
        advanceUntilIdle()

        repository.loadUserProfileGate = CompletableDeferred()
        coordinator.load(second.id)

        assertEquals(UserProfileUiState.Loading, coordinator.uiState.value)
    }

    @Test
    fun `sort order is locked while photo urls are being signed`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeFishingRepository()
            val report = testReport().copy(
                photos = listOf(FishingPhoto(url = "reports/old.jpg"))
            )
            repository.userProfileResult = Result.success(
                UserProfileData(user = null, reports = listOf(report))
            )

            val signingGate = CompletableDeferred<Unit>()
            repository.getPhotoSignedUrlsGate = signingGate

            val coordinator = coordinator(repository)
            coordinator.load(report.userId)
            // The coroutine is suspended inside the signed-url request.
            assertFalse(coordinator.canChangeSortOrder.value)

            coordinator.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)

            // The tap is ignored: the order stays and no half-signed page is published.
            assertEquals(ReportSortOrder.BY_FISHING_TIME, coordinator.sortOrder.value)

            signingGate.complete(Unit)
            advanceUntilIdle()

            assertTrue(coordinator.canChangeSortOrder.value)
            coordinator.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)
            assertEquals(ReportSortOrder.BY_PUBLISH_DATE, coordinator.sortOrder.value)
        }

    @Test
    fun `failed background refresh keeps the visible page`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeFishingRepository()
            val author = testUser()
            repository.userProfileResult = Result.success(
                UserProfileData(user = author, reports = listOf(testReport(userId = author.id)))
            )

            val coordinator = coordinator(repository)
            coordinator.load(author.id)
            advanceUntilIdle()

            repository.userProfileResult = Result.failure(IllegalStateException("offline"))
            coordinator.load(author.id)
            advanceUntilIdle()

            assertTrue(coordinator.uiState.value is UserProfileUiState.Success)
        }

    @Test
    fun `changing the sort order keeps the signed photo urls`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeFishingRepository()
            val older = testReport(
                fishingStartAt = Instant.parse("2026-05-01T08:00:00Z"),
                createdAt = Date(1_800_000_000_000L)
            ).copy(photos = listOf(FishingPhoto(url = "reports/old.jpg")))
            val newer = testReport(
                fishingStartAt = Instant.parse("2026-06-01T08:00:00Z"),
                createdAt = Date(1_700_000_000_000L)
            ).copy(photos = listOf(FishingPhoto(url = "reports/new.jpg")))
            repository.userProfileResult = Result.success(
                UserProfileData(user = null, reports = listOf(older, newer))
            )

            val coordinator = coordinator(repository)
            coordinator.load(newer.userId)
            advanceUntilIdle()

            fun visiblePhotoUrls(): List<String> =
                (coordinator.uiState.value as UserProfileUiState.Success)
                    .reports.map { it.photos.first().url }

            // Storage paths are swapped for signed urls by the coordinator.
            assertEquals(
                listOf("signed:reports/new.jpg", "signed:reports/old.jpg"),
                visiblePhotoUrls()
            )

            coordinator.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)

            // Re-sorting republishes the loaded page: the thumbnails must stay
            // signed urls instead of falling back to raw storage paths.
            assertEquals(
                listOf("signed:reports/old.jpg", "signed:reports/new.jpg"),
                visiblePhotoUrls()
            )
        }

    @Test
    fun `failure is published as error state`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        repository.userProfileResult = Result.failure(IllegalStateException("boom"))

        val coordinator = coordinator(repository)
        coordinator.load(UUID.randomUUID())
        advanceUntilIdle()

        assertTrue(coordinator.uiState.value is UserProfileUiState.Error)
    }

    @Test
    fun `clear resets state and sort order`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeFishingRepository()
        val report = testReport()
        repository.userProfileResult = Result.success(
            UserProfileData(user = null, reports = listOf(report))
        )

        val coordinator = coordinator(repository)
        coordinator.load(report.userId)
        advanceUntilIdle()
        coordinator.setSortOrder(ReportSortOrder.BY_PUBLISH_DATE)
        coordinator.clear()

        assertEquals(UserProfileUiState.Loading, coordinator.uiState.value)
        assertEquals(ReportSortOrder.BY_FISHING_TIME, coordinator.sortOrder.value)
    }
}
