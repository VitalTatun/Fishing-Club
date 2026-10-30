package com.example.fishing

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingDateTimeError
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingType
import com.example.fishing.model.ReportField
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.MainDispatcherRule
import com.example.fishing.viewmodel.CreateReportSaveState
import com.example.fishing.viewmodel.CreateReportViewModel
import com.example.fishing.viewmodel.ReportPhoto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class CreateReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeFishingRepository: FakeFishingRepository
    private lateinit var context: Context

    private val testUser = User(
        id = UUID.randomUUID(),
        name = "Test User",
        email = "test@example.com",
        image = ""
    )

    @Before
    fun setup() {
        fakeAuthRepository = FakeAuthRepository()
        fakeFishingRepository = FakeFishingRepository()
        fakeAuthRepository.sessionUser = testUser
        context = RuntimeEnvironment.getApplication()
    }

    private fun createViewModel(): CreateReportViewModel {
        return CreateReportViewModel(
            repository = fakeFishingRepository,
            authRepository = fakeAuthRepository,
            context = context,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    private fun setStartEndDateTimes(vm: CreateReportViewModel, start: Instant, end: Instant) {
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("ru"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        vm.formStartDate = dateFormat.format(Date.from(start))
        vm.formStartTime = timeFormat.format(Date.from(start))
        vm.formEndDate = dateFormat.format(Date.from(end))
        vm.formEndTime = timeFormat.format(Date.from(end))
    }

    private fun fillValidReport(vm: CreateReportViewModel, start: Instant, end: Instant) {
        vm.formWaterName = "Озеро Нарочь"
        vm.formLocation = GeoPoint(54.9, 26.7)
        setStartEndDateTimes(vm, start, end)
        vm.formSelectedMethod = FishingMethod.SPINNING
        vm.formSelectedBaits = listOf(Bait.WOBBLER)
        vm.formSelectedFish = listOf(Fish(id = UUID.randomUUID(), name = "Щука", count = 2))
    }

    private fun validPastRange(): Pair<Instant, Instant> {
        val end = Instant.now().minusSeconds(3600)
        return Pair(end.minusSeconds(7200), end)
    }

    /** Creates a real JPEG on disk so PhotoUtils can decode it on both passes. */
    private fun createTestPhotoFile(): Uri {
        val file = File(context.cacheDir, "test_photo_${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun `valid report enables save`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        assertTrue(vm.isSaveEnabled)
    }

    @Test
    fun `missing water name allows save when location exists`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formWaterName = ""
        assertTrue(vm.isSaveEnabled)
    }

    @Test
    fun `missing location disables save`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formLocation = null
        assertFalse(vm.isSaveEnabled)
    }

    @Test
    fun `missing method disables save`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formSelectedMethod = FishingMethod.NONE
        assertFalse(vm.isSaveEnabled)
    }

    @Test
    fun `missing bait disables save`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formSelectedBaits = emptyList()
        assertFalse(vm.isSaveEnabled)
    }

    @Test
    fun `missing fish disables save`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formSelectedFish = emptyList()
        assertFalse(vm.isSaveEnabled)
    }

    @Test
    fun `future start time disables save`() = runTest {
        val vm = createViewModel()
        val now = Instant.now()
        val start = now.plusSeconds(3600)
        val end = now.plusSeconds(7200)
        fillValidReport(vm, start, end)
        assertFalse(vm.isSaveEnabled)
        assertTrue(vm.dateTimeErrors().contains(FishingDateTimeError.START_IN_FUTURE))
    }

    @Test
    fun `future end time disables save`() = runTest {
        val vm = createViewModel()
        val now = Instant.now()
        val start = now.minusSeconds(3600)
        val end = now.plusSeconds(3600)
        fillValidReport(vm, start, end)
        assertFalse(vm.isSaveEnabled)
        assertTrue(vm.dateTimeErrors().contains(FishingDateTimeError.END_IN_FUTURE))
    }

    @Test
    fun `end before start disables save`() = runTest {
        val vm = createViewModel()
        val now = Instant.now()
        val start = now.minusSeconds(3600)
        val end = now.minusSeconds(10800)
        fillValidReport(vm, start, end)
        assertFalse(vm.isSaveEnabled)
        assertTrue(vm.dateTimeErrors().contains(FishingDateTimeError.END_NOT_AFTER_START))
    }

    @Test
    fun `cross midnight fishing is still valid`() = runTest {
        val vm = createViewModel()
        // Baseline chosen so that both times are in the past regardless of run time.
        val now = Instant.now()
        val todayMidnight = now.truncatedTo(ChronoUnit.DAYS)
        val baseline = if (now.isBefore(todayMidnight.plus(4, ChronoUnit.HOURS))) {
            todayMidnight.minus(1, ChronoUnit.DAYS)
        } else {
            todayMidnight
        }
        val start = baseline.minus(1, ChronoUnit.HOURS) // 23:00
        val end = baseline.plus(2, ChronoUnit.HOURS)    // 02:00 next day
        fillValidReport(vm, start, end)
        assertTrue(vm.isSaveEnabled)
        assertTrue(vm.dateTimeErrors().isEmpty())
    }

    @Test
    fun `successful save records user id derived name and datetimes`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(1, fakeFishingRepository.saveReportCallCount)
        val saved = fakeFishingRepository.savedReports.single()
        assertEquals(testUser.id, saved.userId)
        assertEquals("${context.getString(FishingMethod.SPINNING.labelRes)} • Щука", saved.name)
        assertEquals(start.truncatedTo(ChronoUnit.MINUTES), saved.fishingStartAt)
        assertEquals(end.truncatedTo(ChronoUnit.MINUTES), saved.fishingEndAt)
    }

    @Test
    fun `report name is method and first fish`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        val saved = fakeFishingRepository.savedReports.single()
        assertEquals(
            "${context.getString(FishingMethod.SPINNING.labelRes)} • Щука",
            saved.name
        )
    }

    @Test
    fun `report name is feeder and first fish bream`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formSelectedMethod = FishingMethod.FEEDER
        vm.formSelectedBaits = listOf(Bait.WORM)
        vm.formSelectedFish = listOf(
            Fish(id = UUID.randomUUID(), name = "Лещ", count = 2),
            Fish(id = UUID.randomUUID(), name = "Карась", count = 1)
        )

        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val saved = fakeFishingRepository.savedReports.single()
        assertEquals(
            "${context.getString(FishingMethod.FEEDER.labelRes)} • Лещ",
            saved.name
        )
    }

    @Test
    fun `report name is bobber and first fish crucian`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formSelectedMethod = FishingMethod.BOBBER
        vm.formSelectedBaits = listOf(Bait.WORM)
        vm.formSelectedFish = listOf(Fish(id = UUID.randomUUID(), name = "Карась", count = 4))

        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val saved = fakeFishingRepository.savedReports.single()
        assertEquals(
            "${context.getString(FishingMethod.BOBBER.labelRes)} • Карась",
            saved.name
        )
    }

    @Test
    fun `successful save reports Success state and resets form`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        var stateAtSuccess: CreateReportSaveState? = null
        vm.saveReport { stateAtSuccess = vm.saveState }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CreateReportSaveState.Success, stateAtSuccess)
        assertEquals(CreateReportSaveState.Success, vm.saveState)
        assertTrue(vm.formSelectedFish.isEmpty())
        assertTrue(vm.formSelectedBaits.isEmpty())
    }

    @Test
    fun `failed save sets error and does not reset form`() = runTest {
        fakeFishingRepository.saveReportException = RuntimeException("boom")
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(1, fakeFishingRepository.saveReportCallCount)
        assertEquals(CreateReportSaveState.Error, vm.saveState)
        assertNotNull(vm.saveErrorMessage)
        assertEquals("Озеро Нарочь", vm.formWaterName)
        assertFalse(vm.formSelectedFish.isEmpty())
    }

    @Test
    fun `save ignores second call while saving`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        vm.saveReport {}
        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeFishingRepository.saveReportCallCount)
    }

    @Test
    fun `unauthenticated user gets error and repository is not called`() = runTest {
        fakeAuthRepository.sessionUser = null
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        var successCalled = false
        vm.saveReport { successCalled = true }

        assertFalse(successCalled)
        assertEquals(0, fakeFishingRepository.saveReportCallCount)
        assertEquals(CreateReportSaveState.Error, vm.saveState)
        assertNotNull(vm.saveErrorMessage)
    }

    @Test
    fun `trophy requires exactly one fish`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        val photoUri = Uri.parse("content://test/photo")
        vm.formReportType = FishingType.HAUL
        vm.formPhotos = listOf(ReportPhoto(uri = photoUri))
        vm.formSelectedFish = listOf(
            Fish(id = UUID.randomUUID(), name = "Щука", count = 1),
            Fish(id = UUID.randomUUID(), name = "Окунь", count = 1)
        )
        assertFalse(vm.isSaveEnabled)

        vm.formSelectedFish = listOf(Fish(id = UUID.randomUUID(), name = "Щука", count = 1))
        assertTrue(vm.isSaveEnabled)
    }

    @Test
    fun `trophy requires a photo`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formReportType = FishingType.HAUL
        vm.formSelectedFish = listOf(Fish(id = UUID.randomUUID(), name = "Щука", count = 1))
        assertFalse(vm.isSaveEnabled)

        vm.formPhotos = listOf(ReportPhoto(uri = Uri.parse("content://test/photo")))
        assertTrue(vm.isSaveEnabled)
    }

    @Test
    fun `regular report saves without photos`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        assertTrue(vm.isSaveEnabled)

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        val saved = fakeFishingRepository.savedReports.single()
        assertTrue(saved.photos.isEmpty())
    }

    @Test
    fun `failed save preserves copied photo files for retry`() = runTest {
        fakeFishingRepository.saveReportException = RuntimeException("storage upload failed")
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formPhotos = listOf(ReportPhoto(uri = createTestPhotoFile()))

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(1, fakeFishingRepository.saveReportCallCount)
        assertEquals(CreateReportSaveState.Error, vm.saveState)
        assertNotNull(vm.saveErrorMessage)

        val photosDir = File(context.filesDir, "photos")
        assertTrue(photosDir.listFiles()?.isNotEmpty() ?: false)
    }

    @Test
    fun `successful save with photo passes internal path and cleans up temp files`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formPhotos = listOf(ReportPhoto(uri = createTestPhotoFile()))

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(1, fakeFishingRepository.saveReportCallCount)
        val saved = fakeFishingRepository.savedReports.single()
        assertEquals(1, saved.photos.size)
        assertTrue(saved.photos.single().url.startsWith(File(context.filesDir, "photos").absolutePath))

        val photosDir = File(context.filesDir, "photos")
        assertTrue(photosDir.listFiles()?.isEmpty() ?: true)
    }

    @Test
    fun `partially unreadable photos block save and keep the form`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formPhotos = listOf(
            ReportPhoto(uri = createTestPhotoFile()),
            ReportPhoto(uri = Uri.fromFile(File(context.cacheDir, "missing_${UUID.randomUUID()}.jpg")))
        )

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(0, fakeFishingRepository.saveReportCallCount)
        assertEquals(CreateReportSaveState.Error, vm.saveState)
        assertNotNull(vm.saveErrorMessage)
        assertEquals(2, vm.formPhotos.size)
        assertFalse(vm.formSelectedFish.isEmpty())
    }

    @Test
    fun `cancelled save does not produce an error and keeps form data`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        var successCalled = false
        vm.saveReport { successCalled = true }
        vm.cancelSave()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(0, fakeFishingRepository.saveReportCallCount)
        assertEquals(CreateReportSaveState.Idle, vm.saveState)
        assertNull(vm.saveErrorMessage)
        assertFalse(vm.formSelectedFish.isEmpty())
    }

    @Test
    fun `network error is mapped to a friendly message`() = runTest {
        fakeFishingRepository.saveReportException = RuntimeException("timeout")
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CreateReportSaveState.Error, vm.saveState)
        assertEquals("Нет соединения с интернетом", vm.saveErrorMessage)
    }

    @Test
    fun `save preserves report id on failure and resets on success`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        fakeFishingRepository.saveReportException = RuntimeException("fail")
        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val idAfterFail = fakeFishingRepository.lastSavedReportId
        assertNotNull(idAfterFail)

        fakeFishingRepository.saveReportException = null
        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(idAfterFail, fakeFishingRepository.lastSavedReportId)

        // Reset form should generate new ID (via success or explicit reset)
        // Success already happened, form is reset
        fillValidReport(vm, start, end)
        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val idAfterSuccess = fakeFishingRepository.lastSavedReportId
        assertNotNull(idAfterSuccess)
        assertTrue(idAfterFail != idAfterSuccess)
    }

    @Test
    fun `form config shows date time errors when dates missing`() = runTest {
        val vm = createViewModel()
        vm.formStartDate = ""
        vm.formEndDate = ""
        val config = vm.formConfig

        val dateTimeSection = config.first { it.id == "date_time" }
        assertTrue(dateTimeSection.items.filterIsInstance<ReportField.ListItemField>().any { it.isError })
    }
}