package com.example.fishing.viewmodel.likes

import com.example.fishing.model.ReportLikeState
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.testReport
import com.example.fishing.testutil.testUser
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
class LikesCoordinatorTest {

    @Test
    fun `toggle applies the optimistic like and confirms it`() = runTest(UnconfinedTestDispatcher()) {
        val auth = FakeAuthRepository().apply { sessionUser = testUser() }
        val session = UserSession(auth).apply { bind() }
        val repository = FakeFishingRepository()
        val report = testReport()
        repository.likeStatesValue =
            mapOf(report.id to ReportLikeState(report.id, isLiked = false, likesCount = 10))
        val coordinator = LikesCoordinator(repository, session, backgroundScope)

        coordinator.refresh()
        advanceUntilIdle()

        val gate = CompletableDeferred<Unit>()
        repository.addLikeGate = gate
        coordinator.toggleLike(report)

        assertEquals(
            ReportLikeState(report.id, isLiked = true, likesCount = 11),
            coordinator.likeStates.value[report.id]
        )

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.addLikeCallCount)
        assertEquals(
            ReportLikeState(report.id, isLiked = true, likesCount = 11),
            coordinator.likeStates.value[report.id]
        )
        assertNull(coordinator.likeError.value)
    }

    @Test
    fun `late result after clear does not restore state`() = runTest(UnconfinedTestDispatcher()) {
        val auth = FakeAuthRepository().apply { sessionUser = testUser() }
        val session = UserSession(auth).apply { bind() }
        val repository = FakeFishingRepository()
        val report = testReport()
        repository.likeStatesValue =
            mapOf(report.id to ReportLikeState(report.id, isLiked = false, likesCount = 10))
        val coordinator = LikesCoordinator(repository, session, backgroundScope)

        coordinator.refresh()
        advanceUntilIdle()

        val gate = CompletableDeferred<Unit>()
        repository.addLikeGate = gate
        coordinator.toggleLike(report)
        advanceUntilIdle()

        session.clear()
        coordinator.clear()
        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(coordinator.likeStates.value.isEmpty())
        assertNull(coordinator.likeError.value)
    }

    @Test
    fun `clear resets states errors and guards`() = runTest(UnconfinedTestDispatcher()) {
        val auth = FakeAuthRepository().apply { sessionUser = testUser() }
        val session = UserSession(auth).apply { bind() }
        val repository = FakeFishingRepository()
        val report = testReport()
        repository.likeStatesValue =
            mapOf(report.id to ReportLikeState(report.id, isLiked = true, likesCount = 5))
        val coordinator = LikesCoordinator(repository, session, backgroundScope)

        coordinator.refresh()
        advanceUntilIdle()
        assertTrue(coordinator.likeStates.value.isNotEmpty())

        coordinator.clear()

        assertTrue(coordinator.likeStates.value.isEmpty())
        assertNull(coordinator.likeError.value)
    }
}
