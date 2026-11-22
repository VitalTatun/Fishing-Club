package com.example.fishing.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.example.fishing.model.Fish
import com.example.fishing.model.FishSpecies
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishCatalogRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.FakeReportUploadQueue
import com.example.fishing.testutil.MainDispatcherRule
import com.example.fishing.testutil.testReport
import java.util.UUID
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
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
class CatchSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var fishingRepository: FakeFishingRepository
    private lateinit var catalogRepository: FakeFishCatalogRepository

    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private val pike = FishSpecies("esox_lucius", "Щука", listOf("щуки"), isPopular = true, sortOrder = 3)
    private val roach = FishSpecies("rutilus_rutilus", "Плотва", emptyList(), isPopular = true, sortOrder = 1)
    private val bream = FishSpecies("abramis_brama", "Лещ", listOf("подлещик"), isPopular = true, sortOrder = 4)
    private val perch = FishSpecies("perca_fluviatilis", "Окунь", emptyList(), isPopular = true, sortOrder = 5)
    private val catfish = FishSpecies("silurus_glanis", "Сом", emptyList(), isPopular = false, sortOrder = 9)

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository().apply {
            sessionUser = User(id = userId, name = "Test", email = "test@example.com", image = "")
        }
        fishingRepository = FakeFishingRepository()
        catalogRepository = FakeFishCatalogRepository(listOf(pike, roach, bream, perch, catfish))
    }

    private fun advance() = mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

    private fun createViewModel(handle: SavedStateHandle = SavedStateHandle()) = CreateReportViewModel(
        repository = fishingRepository,
        fishCatalogRepository = catalogRepository,
        authRepository = authRepository,
        reportUploadQueue = FakeReportUploadQueue(),
        context = RuntimeEnvironment.getApplication(),
        ioDispatcher = mainDispatcherRule.testDispatcher,
        savedStateHandle = handle
    )

    @Test
    fun `without history chips fall back to popular species`() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) {
            viewModel.catchSearchState.collect {}
        }
        advance()

        val state = viewModel.catchSearchState.value
        assertEquals(listOf("Плотва", "Щука", "Лещ", "Окунь"), state.chips.map { it.name })
        assertTrue(state.recent.isEmpty())
        assertFalse(state.isOpen)
    }

    @Test
    fun `history is ordered by report count and limited to three`() = runTest {
        val otherUser = UUID.randomUUID()
        fishingRepository.homeReportsValue = listOf(
            testReport(userId = userId, fish = listOf(fish("Плотва"))),
            testReport(userId = userId, fish = listOf(fish("Плотва"))),
            testReport(userId = userId, fish = listOf(fish("Щука"))),
            testReport(userId = userId, fish = listOf(fish("Лещ"))),
            testReport(userId = userId, fish = listOf(fish("Окунь"))),
            // Another user's report must not affect the history.
            testReport(userId = otherUser, fish = listOf(fish("Сом")))
        )

        val viewModel = createViewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) {
            viewModel.catchSearchState.collect {}
        }
        advance()

        val state = viewModel.catchSearchState.value
        assertEquals(3, state.recent.size)
        assertEquals("Плотва", state.recent.first().name)
        assertEquals(2, state.recent.first().reportCount)
        assertEquals(4, state.chips.size)
        assertTrue(state.chips.none { it.name == "Сом" })
    }

    @Test
    fun `open query and results are exposed and survive a new handle owner`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) {
            viewModel.catchSearchState.collect {}
        }

        viewModel.openCatchSearch()
        viewModel.onCatchSearchQueryChange("щу")
        advance()

        val state = viewModel.catchSearchState.value
        assertTrue(state.isOpen)
        assertEquals("щу", state.query)
        assertEquals(listOf("Щука"), state.results.map { it.canonicalName })

        val restored = createViewModel(handle)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) {
            restored.catchSearchState.collect {}
        }
        advance()

        assertTrue(restored.catchSearchState.value.isOpen)
        assertEquals("щу", restored.catchSearchState.value.query)
    }

    private fun fish(name: String) = Fish(id = UUID.randomUUID(), name = name, count = 1)
}
