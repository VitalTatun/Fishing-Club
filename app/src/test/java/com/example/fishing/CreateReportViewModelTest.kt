package com.example.fishing

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingDateTimeError
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingType
import com.example.fishing.model.ReportField
import com.example.fishing.model.ReportSaveProgress
import com.example.fishing.model.ReportUploadState
import com.example.fishing.model.User
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.FakeFishCatalogRepository
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.FakeReportUploadQueue
import com.example.fishing.testutil.MainDispatcherRule
import com.example.fishing.utils.PhotoUtils
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
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
    private lateinit var fakeFishCatalogRepository: FakeFishCatalogRepository
    private lateinit var fakeReportUploadQueue: FakeReportUploadQueue
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
        fakeFishCatalogRepository = FakeFishCatalogRepository()
        fakeReportUploadQueue = FakeReportUploadQueue()
        fakeAuthRepository.sessionUser = testUser
        context = RuntimeEnvironment.getApplication()
    }

    private fun createViewModel(
        handle: SavedStateHandle = SavedStateHandle()
    ): CreateReportViewModel {
        return CreateReportViewModel(
            repository = fakeFishingRepository,
            fishCatalogRepository = fakeFishCatalogRepository,
            authRepository = fakeAuthRepository,
            reportUploadQueue = fakeReportUploadQueue,
            context = context,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            savedStateHandle = handle
        )
    }

    /** ContentResolver stub that records persistable grant takes and releases. */
    private class RecordingResolver(
        context: Context,
        private val releases: MutableList<Pair<Uri, Int>>,
        private val takes: MutableList<Pair<Uri, Int>> = mutableListOf()
    ) : ContentResolver(context) {
        override fun takePersistableUriPermission(uri: Uri, modeFlags: Int) {
            takes += uri to modeFlags
        }

        override fun releasePersistableUriPermission(uri: Uri, modeFlags: Int) {
            releases += uri to modeFlags
        }
    }

    private fun createViewModelRecordingGrants(
        releases: MutableList<Pair<Uri, Int>>,
        takes: MutableList<Pair<Uri, Int>> = mutableListOf(),
        handle: SavedStateHandle = SavedStateHandle()
    ): CreateReportViewModel {
        val resolver = RecordingResolver(context, releases, takes)
        val wrappedContext = object : ContextWrapper(context) {
            override fun getContentResolver(): ContentResolver = resolver
        }
        return CreateReportViewModel(
            repository = fakeFishingRepository,
            fishCatalogRepository = fakeFishCatalogRepository,
            authRepository = fakeAuthRepository,
            reportUploadQueue = fakeReportUploadQueue,
            context = wrappedContext,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            savedStateHandle = handle
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

    /** Same encoding as [createTestPhotoFile], but exposes the camera temp file. */
    private fun createCameraTempFile(): File {
        val file = File(context.cacheDir, "camera_photo_${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        bitmap.recycle()
        return file
    }

    /** A processed draft photo in filesDir/photos, where restored photos live. */
    private fun createDraftPhotoFile(): File {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(photosDir, "draft_photo_${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        bitmap.recycle()
        return file
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
        // Build the baseline from local midnight: the form parses dates in the
        // default zone, so a UTC-based baseline would not produce 23:00 -> 02:00.
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val todayMidnight = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
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
        assertEquals(1, fakeReportUploadQueue.enqueuedReports.size)
        val saved = fakeReportUploadQueue.enqueuedReports.single()
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
        val saved = fakeReportUploadQueue.enqueuedReports.single()
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

        val saved = fakeReportUploadQueue.enqueuedReports.single()
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

        val saved = fakeReportUploadQueue.enqueuedReports.single()
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
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        fakeReportUploadQueue.onEnqueue = { report ->
            fakeReportUploadQueue.setPendingRow(
                reportId = report.id,
                state = ReportUploadState.FAILED,
                lastError = "Не удалось загрузить фото"
            )
        }

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(1, fakeReportUploadQueue.enqueuedReports.size)
        assertEquals(CreateReportSaveState.Error, vm.saveState)
        assertEquals("Не удалось загрузить фото", vm.saveErrorMessage)
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

        assertEquals(1, fakeReportUploadQueue.enqueuedReports.size)
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
        val saved = fakeReportUploadQueue.enqueuedReports.single()
        assertTrue(saved.photos.isEmpty())
    }

    @Test
    fun `failed save preserves copied photo files for retry`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        vm.formPhotos = listOf(ReportPhoto(uri = createTestPhotoFile()))
        fakeReportUploadQueue.onEnqueue = { report ->
            fakeReportUploadQueue.setPendingRow(
                reportId = report.id,
                state = ReportUploadState.FAILED,
                lastError = "storage upload failed"
            )
        }

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(1, fakeReportUploadQueue.enqueuedReports.size)
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
        assertEquals(1, fakeReportUploadQueue.enqueuedReports.size)
        val saved = fakeReportUploadQueue.enqueuedReports.single()
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
        assertEquals(0, fakeReportUploadQueue.enqueuedReports.size)
        assertEquals(1, fakeReportUploadQueue.cancelledReportIds.size)
        assertEquals(CreateReportSaveState.Idle, vm.saveState)
        assertNull(vm.saveErrorMessage)
        assertFalse(vm.formSelectedFish.isEmpty())
    }

    @Test
    fun `network error is mapped to a friendly message`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        fakeReportUploadQueue.onEnqueue = { report ->
            fakeReportUploadQueue.setPendingRow(
                reportId = report.id,
                state = ReportUploadState.FAILED,
                lastError = "Нет соединения с интернетом"
            )
        }

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

        var failNext = true
        fakeReportUploadQueue.onEnqueue = { report ->
            if (failNext) {
                fakeReportUploadQueue.setPendingRow(
                    reportId = report.id,
                    state = ReportUploadState.FAILED,
                    lastError = "fail"
                )
            }
        }
        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val idAfterFail = fakeReportUploadQueue.enqueuedReports.single().id
        assertNotNull(idAfterFail)

        failNext = false
        fakeReportUploadQueue.pending.value = emptyList()
        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(idAfterFail, fakeReportUploadQueue.enqueuedReports[1].id)

        // Reset form should generate new ID (via success or explicit reset).
        fillValidReport(vm, start, end)
        vm.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val idAfterSuccess = fakeReportUploadQueue.enqueuedReports[2].id
        assertNotNull(idAfterSuccess)
        assertTrue(idAfterFail != idAfterSuccess)
    }

    @Test
    fun `save reports upload progress from the queue`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        fakeReportUploadQueue.onEnqueue = { report ->
            fakeReportUploadQueue.setPendingRow(
                reportId = report.id,
                state = ReportUploadState.UPLOADING,
                uploadedPhotos = 1,
                totalPhotos = 2
            )
        }

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()

        assertEquals(CreateReportSaveState.Saving, vm.saveState)
        assertEquals(1, vm.saveProgress?.uploadedPhotos)
        assertEquals(2, vm.saveProgress?.totalPhotos)
        assertFalse(successCalled)

        // The worker removes the row after the report is published.
        fakeReportUploadQueue.pending.value = emptyList()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(CreateReportSaveState.Success, vm.saveState)
        assertNull(vm.saveProgress)
    }

    @Test
    fun `enqueue failure keeps the form and shows an error`() = runTest {
        fakeReportUploadQueue.enqueueResult = Result.failure(RuntimeException("boom"))
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)

        var successCalled = false
        vm.saveReport { successCalled = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(CreateReportSaveState.Error, vm.saveState)
        assertNotNull(vm.saveErrorMessage)
        assertEquals("Озеро Нарочь", vm.formWaterName)
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

    @Test
    fun `new report starts with unset fishing date and time`() = runTest {
        val vm = createViewModel()

        assertNull(vm.formStartDate)
        assertNull(vm.formStartTime)
        assertNull(vm.formEndDate)
        assertNull(vm.formEndTime)

        vm.formStartDate = "1 янв 2020"
        vm.formStartTime = "08:15"
        vm.resetFormState()

        assertNull(vm.formStartDate)
        assertNull(vm.formStartTime)
        assertNull(vm.formEndDate)
        assertNull(vm.formEndTime)
    }

    @Test
    fun `fishing date or time counts as form data for discard confirmation`() = runTest {
        val vm = createViewModel()

        assertFalse(vm.formHasData)
        vm.formStartDate = "1 янв 2020"
        assertTrue(vm.formHasData)

        vm.resetFormState()
        assertFalse(vm.formHasData)

        vm.formEndTime = "18:45"
        assertTrue(vm.formHasData)
    }

    @Test
    fun `restores all form fields after process death`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        val fish = Fish(id = UUID.randomUUID(), name = "Щука", count = 3)
        val photo = ReportPhoto(id = UUID.randomUUID(), uri = createTestPhotoFile())

        first.formReportType = FishingType.HAUL
        first.formWaterName = "Озеро Нарочь"
        first.formPhotos = listOf(photo)
        first.formStartDate = "1 янв 2020"
        first.formStartTime = "08:15"
        first.formEndDate = "2 янв 2020"
        first.formEndTime = "18:45"
        first.formFishingFromShore = false
        first.formIsPublic = false
        first.formIsPaidWater = true
        first.formWeight = 3.5f
        first.formSelectedMethod = FishingMethod.SPINNING
        first.formSelectedBaits = listOf(Bait.WOBBLER, Bait.SPOONBAIT)
        first.formSelectedFish = listOf(fish)
        first.formMood = 5
        first.formComment = "Клюёт с утра"
        first.formLocation = GeoPoint(54.9, 26.7)

        val second = createViewModel(handle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(FishingType.HAUL, second.formReportType)
        assertEquals("Озеро Нарочь", second.formWaterName)
        assertEquals(1, second.formPhotos.size)
        assertEquals(photo.id, second.formPhotos.single().id)
        assertEquals(photo.uri, second.formPhotos.single().uri)
        assertEquals("1 янв 2020", second.formStartDate)
        assertEquals("08:15", second.formStartTime)
        assertEquals("2 янв 2020", second.formEndDate)
        assertEquals("18:45", second.formEndTime)
        assertFalse(second.formFishingFromShore)
        assertFalse(second.formIsPublic)
        assertTrue(second.formIsPaidWater)
        assertEquals(3.5f, second.formWeight, 0.001f)
        assertEquals(FishingMethod.SPINNING, second.formSelectedMethod)
        assertEquals(listOf(Bait.WOBBLER, Bait.SPOONBAIT), second.formSelectedBaits)
        assertEquals(listOf(fish), second.formSelectedFish)
        assertEquals(5, second.formMood)
        assertEquals("Клюёт с утра", second.formComment)
        assertEquals(54.9, second.formLocation?.latitude ?: 0.0, 0.0001)
        assertEquals(26.7, second.formLocation?.longitude ?: 0.0, 0.0001)
        assertFalse(second.isRestoringPhotos)
        assertEquals(0, second.restoredPhotosLost)
    }

    @Test
    fun `restored dates are not overwritten by defaults`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        first.formStartDate = "1 янв 2020"
        first.formStartTime = "05:30"
        first.formEndDate = "1 янв 2020"
        first.formEndTime = "19:00"

        val second = createViewModel(handle)

        assertEquals("1 янв 2020", second.formStartDate)
        assertEquals("05:30", second.formStartTime)
        assertEquals("1 янв 2020", second.formEndDate)
        assertEquals("19:00", second.formEndTime)
    }

    @Test
    fun `save state is idle after restore`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        first.saveState = CreateReportSaveState.Error
        first.saveErrorMessage = "boom"
        first.saveProgress = ReportSaveProgress(uploadedPhotos = 1, totalPhotos = 2)

        val second = createViewModel(handle)

        assertEquals(CreateReportSaveState.Idle, second.saveState)
        assertNull(second.saveErrorMessage)
        assertNull(second.saveProgress)
        assertFalse(second.isSaving)
    }

    @Test
    fun `reset form clears the saved draft`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        val (start, end) = validPastRange()
        fillValidReport(first, start, end)
        first.formPhotos = listOf(ReportPhoto(uri = createTestPhotoFile()))
        first.formComment = "черновик"
        first.formWeight = 2f

        first.resetFormState(cleanupPhotos = false)

        val second = createViewModel(handle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", second.formWaterName)
        assertEquals(FishingType.FISHING_LOG, second.formReportType)
        assertTrue(second.formPhotos.isEmpty())
        assertEquals(FishingMethod.NONE, second.formSelectedMethod)
        assertTrue(second.formSelectedBaits.isEmpty())
        assertTrue(second.formSelectedFish.isEmpty())
        assertEquals("", second.formComment)
        assertEquals(0f, second.formWeight, 0.001f)
        assertEquals(3, second.formMood)
        assertNull(second.formLocation)
    }

    @Test
    fun `repeat save uses the restored report id`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        val (start, end) = validPastRange()
        fillValidReport(first, start, end)
        fakeReportUploadQueue.onEnqueue = { report ->
            fakeReportUploadQueue.setPendingRow(
                reportId = report.id,
                state = ReportUploadState.FAILED,
                lastError = "fail"
            )
        }

        first.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        val failedId = fakeReportUploadQueue.enqueuedReports.single().id

        fakeReportUploadQueue.onEnqueue = null
        fakeReportUploadQueue.pending.value = emptyList()

        val second = createViewModel(handle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        fillValidReport(second, start, end)
        second.saveReport {}
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeReportUploadQueue.enqueuedReports.size)
        assertEquals(failedId, fakeReportUploadQueue.enqueuedReports[1].id)
    }

    @Test
    fun `interrupted camera capture is restored as a photo`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        // Draft photos live in filesDir/photos; startup cleanup must not eat them.
        val cameraFile = createDraftPhotoFile()
        first.beginCameraCapture(cameraFile)

        val second = createViewModel(handle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, second.formPhotos.size)
        assertNull(second.pendingCameraPath)
        val persisted = second.formPhotos.single().uri
        assertEquals("file", persisted.scheme)
        val persistedFile = File(requireNotNull(persisted.path))
        assertTrue(persistedFile.isFile)
        assertEquals(cameraFile.absolutePath, persistedFile.absolutePath)

        PhotoUtils.clearSavedPhotos(context.filesDir)

        assertTrue(persistedFile.isFile)
        assertEquals(1, second.formPhotos.size)
    }

    @Test
    fun `missing camera temp file is discarded`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        val cameraFile = createCameraTempFile()
        first.beginCameraCapture(cameraFile)
        assertTrue(cameraFile.delete())

        val second = createViewModel(handle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(second.formPhotos.isEmpty())
        assertNull(second.pendingCameraPath)
        assertFalse(cameraFile.exists())
    }

    @Test
    fun `unreadable restored photo is dropped and counted`() = runTest {
        val missingUri = Uri.parse("content://missing/photo_${UUID.randomUUID()}.jpg")
        // Robolectric returns an empty stream for unknown content URIs by default,
        // so make the provider behave like a removed gallery grant.
        Shadows.shadowOf(context.contentResolver).registerInputStreamSupplier(missingUri) {
            throw FileNotFoundException("grant revoked")
        }

        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        val (start, end) = validPastRange()
        fillValidReport(first, start, end)
        first.formPhotos = listOf(ReportPhoto(uri = missingUri))

        val vm = createViewModel(handle)
        // The form itself is valid; only the photo check may block saving.
        assertTrue(vm.isRestoringPhotos)
        assertFalse(vm.isSaveEnabled)

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.formPhotos.isEmpty())
        assertEquals(1, vm.restoredPhotosLost)
        assertFalse(vm.isRestoringPhotos)
        assertTrue(vm.isSaveEnabled)

        vm.consumeRestoredPhotosLost()
        assertEquals(0, vm.restoredPhotosLost)
    }

    @Test
    fun `empty camera temp file is discarded`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        val emptyFile = File(context.cacheDir, "empty_camera_${UUID.randomUUID()}.jpg").apply {
            writeBytes(ByteArray(0))
        }
        first.beginCameraCapture(emptyFile)

        val second = createViewModel(handle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(second.formPhotos.isEmpty())
        assertNull(second.pendingCameraPath)
        assertFalse(emptyFile.exists())
    }

    @Test
    fun `late camera result after recovery does not duplicate the photo`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        val cameraFile = createDraftPhotoFile()
        first.beginCameraCapture(cameraFile)

        val second = createViewModel(handle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, second.formPhotos.size)

        // The system may deliver the pending ActivityResult after the recovery
        // already claimed the file; it must be a no-op.
        second.onCameraCaptureResult(success = true)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, second.formPhotos.size)
    }

    @Test
    fun `draft from another user is discarded after process death`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        val (start, end) = validPastRange()
        fillValidReport(first, start, end)
        first.formPhotos = listOf(ReportPhoto(uri = Uri.fromFile(createDraftPhotoFile())))

        fakeAuthRepository.sessionUser = User(
            id = UUID.randomUUID(),
            name = "Other User",
            email = "other@example.com",
            image = ""
        )

        val second = createViewModel(handle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", second.formWaterName)
        assertTrue(second.formPhotos.isEmpty())
        assertTrue(second.formSelectedFish.isEmpty())
        assertNull(second.formLocation)
    }

    @Test
    fun `draft is restored for the same user after process death`() = runTest {
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        val (start, end) = validPastRange()
        fillValidReport(first, start, end)

        val second = createViewModel(handle)

        assertEquals("Озеро Нарочь", second.formWaterName)
        assertFalse(second.formSelectedFish.isEmpty())
    }

    @Test
    fun `logout after authentication resets the form`() = runTest {
        val vm = createViewModel()
        vm.formWaterName = "Черновик"
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        fakeAuthRepository.sessionUser = null
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", vm.formWaterName)
        assertTrue(vm.formPhotos.isEmpty())
    }

    @Test
    fun `cold start unauthenticated keeps the restored draft`() = runTest {
        fakeAuthRepository.sessionUser = null
        val handle = SavedStateHandle(mapOf("form_water_name" to "Черновик"))

        val vm = createViewModel(handle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Черновик", vm.formWaterName)
    }

    @Test
    fun `successful camera result publishes the photo and consumes the pending path`() = runTest {
        val vm = createViewModel()
        val cameraFile = createCameraTempFile()
        vm.beginCameraCapture(cameraFile)
        assertEquals(cameraFile.absolutePath, vm.pendingCameraPath)

        vm.onCameraCaptureResult(success = true)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.pendingCameraPath)
        assertFalse(cameraFile.exists())
        val photo = vm.formPhotos.single()
        assertEquals("file", photo.uri.scheme)
        assertTrue(File(requireNotNull(photo.uri.path)).isFile)
    }

    @Test
    fun `failed camera result deletes the temp file and adds no photo`() = runTest {
        val vm = createViewModel()
        val cameraFile = createCameraTempFile()
        vm.beginCameraCapture(cameraFile)

        vm.onCameraCaptureResult(success = false)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.pendingCameraPath)
        assertTrue(vm.formPhotos.isEmpty())
        assertFalse(cameraFile.exists())
    }

    @Test
    fun `starting a new capture deletes the abandoned temp file`() = runTest {
        val vm = createViewModel()
        val abandoned = createCameraTempFile()
        val current = createCameraTempFile()

        vm.beginCameraCapture(abandoned)
        vm.beginCameraCapture(current)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(current.absolutePath, vm.pendingCameraPath)
        assertFalse(abandoned.exists())
        assertTrue(current.exists())
    }

    @Test
    fun `corrupted restored report id is healed and stays stable`() = runTest {
        val handle = SavedStateHandle(mapOf("form_report_id" to "not-a-uuid"))

        createViewModel(handle)
        val healed = requireNotNull(handle.get<String>("form_report_id"))
        // Throws IllegalArgumentException, failing the test, if not healed.
        val healedId = UUID.fromString(healed)

        createViewModel(handle)

        assertEquals(healedId.toString(), handle.get<String>("form_report_id"))
    }

    @Test
    fun `removePhoto releases the gallery grant and keeps the other photos`() = runTest {
        val releases = mutableListOf<Pair<Uri, Int>>()
        val vm = createViewModelRecordingGrants(releases)
        val kept = ReportPhoto(uri = createTestPhotoFile())
        val galleryUri = Uri.parse("content://media/external/images/media/42")
        val removed = ReportPhoto(uri = galleryUri)
        vm.formPhotos = listOf(kept, removed)

        vm.removePhoto(removed)
        // Releasing the grant is a binder call: it must not run on the caller's
        // (main) thread.
        assertTrue(releases.isEmpty())

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(kept), vm.formPhotos)
        assertEquals(
            listOf(galleryUri to Intent.FLAG_GRANT_READ_URI_PERMISSION),
            releases
        )
    }

    @Test
    fun `removePhoto ignores unknown ids and never releases file uris`() = runTest {
        val releases = mutableListOf<Pair<Uri, Int>>()
        val vm = createViewModelRecordingGrants(releases)
        val filePhoto = ReportPhoto(uri = createTestPhotoFile())
        vm.formPhotos = listOf(filePhoto)

        // A photo that is not in the draft must stay a no-op.
        vm.removePhoto(ReportPhoto(uri = Uri.parse("content://media/external/images/media/7")))
        assertEquals(listOf(filePhoto), vm.formPhotos)

        vm.removePhoto(filePhoto)

        assertTrue(vm.formPhotos.isEmpty())
        // file:// needs no grant; releasing one here would be wrong.
        assertTrue(releases.isEmpty())
    }

    @Test
    fun `camera photo finishing after a reset is discarded`() = runTest {
        val vm = createViewModel()
        val cameraFile = createCameraTempFile()
        vm.beginCameraCapture(cameraFile)

        // Capture the draft token, then reset before the IO work can finish.
        vm.onCameraCaptureResult(success = true)
        vm.resetFormState(cleanupPhotos = false)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.formPhotos.isEmpty())
        assertFalse(cameraFile.exists())
        val photosDir = File(context.filesDir, "photos")
        assertTrue(photosDir.listFiles()?.isEmpty() ?: true)
    }

    @Test
    fun `photo reconciliation stopped by a reset does not report into the new draft`() = runTest {
        val missingUri = Uri.parse("content://missing/photo_${UUID.randomUUID()}.jpg")
        Shadows.shadowOf(context.contentResolver).registerInputStreamSupplier(missingUri) {
            throw FileNotFoundException("grant revoked")
        }
        val handle = SavedStateHandle()
        val first = createViewModel(handle)
        first.formPhotos = listOf(ReportPhoto(uri = missingUri))

        // Logout/account switch resets the draft while the readability check is
        // still on the IO queue.
        val second = createViewModel(handle)
        assertTrue(second.isRestoringPhotos)
        second.resetFormState(cleanupPhotos = false)

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(second.formPhotos.isEmpty())
        // The stale check must not warn about photos of a draft that is gone.
        assertEquals(0, second.restoredPhotosLost)
        assertFalse(second.isRestoringPhotos)
    }

    @Test
    fun `removePhoto is ignored while a save is in flight`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        val photoFile = createDraftPhotoFile()
        val photo = ReportPhoto(uri = Uri.fromFile(photoFile))
        vm.formPhotos = listOf(photo)

        vm.saveReport {}
        // The upload snapshot points at this file until the queue row settles.
        assertTrue(vm.isSaving)
        vm.removePhoto(photo)

        assertEquals(listOf(photo), vm.formPhotos)
        assertTrue(photoFile.exists())

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `picked gallery uris keep their read grant and file uris are skipped`() = runTest {
        val releases = mutableListOf<Pair<Uri, Int>>()
        val takes = mutableListOf<Pair<Uri, Int>>()
        val vm = createViewModelRecordingGrants(releases, takes)
        val galleryUri = Uri.parse("content://media/external/images/media/11")
        val fileUri = Uri.fromFile(createCameraTempFile())

        vm.retainPickedPhotoAccess(listOf(galleryUri, fileUri))
        // A binder call per uri must not run on the caller's (main) thread.
        assertTrue(takes.isEmpty())

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(galleryUri to Intent.FLAG_GRANT_READ_URI_PERMISSION), takes)
        // Taking a grant must never release one.
        assertTrue(releases.isEmpty())
    }

    @Test
    fun `removing a photo deletes its local draft file`() = runTest {
        val vm = createViewModel()
        val photoFile = createDraftPhotoFile()
        vm.formPhotos = listOf(ReportPhoto(uri = Uri.fromFile(photoFile)))
        val photo = vm.formPhotos.single()

        vm.removePhoto(photo)
        // The delete is queued on IO; nothing is removed on the calling thread.
        assertTrue(photoFile.exists())

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.formPhotos.isEmpty())
        assertFalse(photoFile.exists())
    }

    @Test
    fun `removePhoto keeps a file still referenced by the upload queue`() = runTest {
        val vm = createViewModel()
        val photoFile = createDraftPhotoFile()
        fakeReportUploadQueue.referencedPaths = setOf(photoFile.absolutePath)
        vm.formPhotos = listOf(ReportPhoto(uri = Uri.fromFile(photoFile)))
        val photo = vm.formPhotos.single()

        vm.removePhoto(photo)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.formPhotos.isEmpty())
        // A FAILED queue row may still upload this exact file.
        assertTrue(photoFile.exists())
    }

    @Test
    fun `save is disabled while a camera photo is publishing`() = runTest {
        val vm = createViewModel()
        val (start, end) = validPastRange()
        fillValidReport(vm, start, end)
        val cameraFile = createCameraTempFile()
        vm.beginCameraCapture(cameraFile)

        vm.onCameraCaptureResult(success = true)

        assertTrue(vm.isPublishingCameraPhoto)
        assertFalse(vm.isSaveEnabled)

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.isPublishingCameraPhoto)
        assertTrue(vm.isSaveEnabled)
    }
}
