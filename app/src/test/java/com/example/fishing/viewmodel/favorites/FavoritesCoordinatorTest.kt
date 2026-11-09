package com.example.fishing.viewmodel.favorites

import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.testReport
import com.example.fishing.testutil.testUser
import com.example.fishing.viewmodel.photos.PhotoUrlResolver
import com.example.fishing.viewmodel.session.UserSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesCoordinatorTest {

    @Test
    fun `toggle applies optimistic state and releases the in-flight guard`() = runTest(UnconfinedTestDispatcher()) {
        val auth = FakeAuthRepository().apply { sessionUser = testUser() }
        val session = UserSession(auth).apply { bind() }
        val repository = FakeFishingRepository()
        val coordinator = FavoritesCoordinator(
            repository = repository,
            session = session,
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )
        val report = testReport()
        val gate = CompletableDeferred<Unit>()
        repository.addFavoriteGate = gate

        coordinator.toggleFavorite(report)
        advanceUntilIdle()

        assertEquals(1, repository.addFavoriteCallCount)
        assertTrue(coordinator.favoriteIds.value.contains(report.id))
        assertTrue(coordinator.favoriteOpsInFlight.value.contains(report.id))

        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(coordinator.favoriteIds.value.contains(report.id))
        assertTrue(coordinator.favoriteOpsInFlight.value.isEmpty())
        assertNull(coordinator.favoriteError.value)
    }

    @Test
    fun `filter favorites are loaded once per user until forced`() = runTest(UnconfinedTestDispatcher()) {
        val user = testUser()
        val auth = FakeAuthRepository().apply { sessionUser = user }
        val session = UserSession(auth).apply { bind() }
        val repository = FakeFishingRepository()
        val report = testReport(userId = user.id)
        repository.favoriteReportsValue = listOf(report)
        val coordinator = FavoritesCoordinator(
            repository = repository,
            session = session,
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )

        coordinator.loadFilterFavoriteReports(force = false)
        advanceUntilIdle()
        coordinator.loadFilterFavoriteReports(force = false)
        advanceUntilIdle()

        assertEquals(1, repository.loadFavoriteReportsCallCount)
        assertEquals(listOf(report), coordinator.filterFavoriteReports.value)

        coordinator.loadFilterFavoriteReports(force = true)
        advanceUntilIdle()

        assertEquals(2, repository.loadFavoriteReportsCallCount)
    }

    @Test
    fun `clear resets ids overlays and errors`() = runTest(UnconfinedTestDispatcher()) {
        val auth = FakeAuthRepository().apply { sessionUser = testUser() }
        val session = UserSession(auth).apply { bind() }
        val repository = FakeFishingRepository()
        val coordinator = FavoritesCoordinator(
            repository = repository,
            session = session,
            photoUrls = PhotoUrlResolver(repository),
            scope = backgroundScope
        )
        val report = testReport()

        coordinator.toggleFavorite(report)
        advanceUntilIdle()
        assertTrue(coordinator.favoriteIds.value.isNotEmpty())

        coordinator.clear()

        assertTrue(coordinator.favoriteIds.value.isEmpty())
        assertTrue(coordinator.favoriteOpsInFlight.value.isEmpty())
        assertTrue(coordinator.filterFavoriteReports.value.isEmpty())
        assertNull(coordinator.favoriteError.value)
    }
}
