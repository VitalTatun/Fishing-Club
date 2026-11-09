package com.example.fishing.data.upload

import android.app.Application
import android.content.Context
import com.example.fishing.data.local.dao.PendingReportUploadDao
import com.example.fishing.data.local.entity.PendingReportUploadEntity
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.ReportUploadState
import com.example.fishing.model.User
import com.example.fishing.model.Water
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.io.File
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class ReportUploadProcessorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var dao: FakePendingReportUploadDao
    private lateinit var repository: FakeFishingRepository
    private lateinit var codec: PendingReportPayloadCodec
    private lateinit var processor: ReportUploadProcessor

    private val user = User(
        id = UUID.randomUUID(),
        image = "",
        name = "Test User",
        email = "test@example.com"
    )

    private val reportId = UUID.randomUUID()

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        dao = FakePendingReportUploadDao()
        repository = FakeFishingRepository()
        codec = PendingReportPayloadCodec()
        processor = ReportUploadProcessor(
            context = context,
            repository = repository,
            dao = dao,
            codec = codec,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    private fun report(id: UUID = reportId, photos: List<String> = emptyList()): FishingReport =
        FishingReport(
            id = id,
            userId = user.id,
            type = FishingType.FISHING_LOG,
            name = "Спиннинг • Щука",
            water = Water(
                waterName = "Озеро Нарочь",
                latitude = 54.85,
                longitude = 26.77
            ),
            photos = photos.map { com.example.fishing.model.FishingPhoto(url = it) },
            fishingStartAt = Instant.now().minusSeconds(3600),
            fishingEndAt = Instant.now(),
            weight = 2.0,
            fish = listOf(Fish(id = UUID.randomUUID(), name = "Щука", count = 1)),
            fishingMethod = FishingMethod.SPINNING,
            bait = listOf(Bait.WOBBLER),
            comment = "",
            user = user,
            fishingFromTheShore = true,
            isPublic = true
        )

    private fun entity(
        id: UUID = reportId,
        report: FishingReport = report(),
        localPhotoPaths: List<String> = emptyList(),
        state: ReportUploadState = ReportUploadState.QUEUED,
        attempts: Int = 0,
        lastError: String? = null
    ) = PendingReportUploadEntity(
        id = id,
        reportJson = codec.encode(report),
        localPhotoPathsJson = codec.encodePaths(localPhotoPaths),
        temporaryPhotoPathsJson = codec.encodePaths(localPhotoPaths),
        isPublic = report.isPublic,
        createdAt = System.currentTimeMillis(),
        attempts = attempts,
        state = state.name,
        uploadedPhotos = 0,
        totalPhotos = report.photos.size,
        lastError = lastError
    )

    private fun photoFile(name: String): File {
        val photosDir = File(context.filesDir, "photos")
        photosDir.mkdirs()
        return File(photosDir, name).apply {
            writeText("jpeg")
        }
    }

    @Test
    fun `successful upload removes the row and its photo copies`() = runTest {
        val first = photoFile("one.jpg")
        val second = photoFile("two.jpg")
        val paths = listOf(first.absolutePath, second.absolutePath)
        dao.rows[reportId] = entity(
            report = report(photos = paths),
            localPhotoPaths = paths
        )

        val outcome = processor.process(reportId)

        assertEquals(ReportUploadOutcome.Success(isPublic = true), outcome)
        assertEquals(1, repository.saveReportCallCount)
        assertEquals(null, dao.rows[reportId])
        assertFalse(first.exists())
        assertFalse(second.exists())
    }

    @Test
    fun `progress callback updates the row while uploading`() = runTest {
        val paths = listOf(photoFile("one.jpg").absolutePath, photoFile("two.jpg").absolutePath)
        dao.rows[reportId] = entity(
            report = report(photos = paths),
            localPhotoPaths = paths
        )

        processor.process(reportId)

        assertEquals(2, dao.progress[reportId]?.first)
        assertEquals(2, dao.progress[reportId]?.second)
    }

    @Test
    fun `retry after the server saved the report but the acknowledgement was lost`() = runTest {
        val photo = photoFile("one.jpg")
        val paths = listOf(photo.absolutePath)
        dao.rows[reportId] = entity(
            report = report(photos = paths),
            localPhotoPaths = paths
        )
        repository.saveReportException = RuntimeException("timeout after commit")
        repository.saveReportPersistsBeforeFailure = true

        val first = processor.process(reportId)

        // The client cannot tell that the server committed, so it keeps the row.
        assertTrue(first is ReportUploadOutcome.Retry)
        assertEquals(1, repository.saveReportCallCount)
        assertEquals(ReportUploadState.QUEUED.name, dao.rows.getValue(reportId).state)
        assertTrue(photo.exists())

        repository.saveReportException = null
        val retry = processor.process(reportId)

        assertEquals(ReportUploadOutcome.Success(isPublic = true), retry)
        // The report and every photo were already confirmed, so the retry must
        // not upload the same report again.
        assertEquals(1, repository.saveReportCallCount)
        assertEquals(reportId, repository.savedReports.single().id)
        assertEquals(null, dao.rows[reportId])
        assertFalse(photo.exists())
    }

    @Test
    fun `an existing report with missing photos still goes through the upload`() = runTest {
        val photo = photoFile("one.jpg")
        val paths = listOf(photo.absolutePath)
        dao.rows[reportId] = entity(
            report = report(photos = paths),
            localPhotoPaths = paths,
            attempts = 1
        )
        // The report row exists on the server, but its attachment was never
        // uploaded. An existence-only check would lose this photo.
        repository.serverHasReport = true
        repository.serverHasAllPhotos = false

        val outcome = processor.process(reportId)

        assertEquals(ReportUploadOutcome.Success(isPublic = true), outcome)
        assertEquals(1, repository.saveReportCallCount)
        assertEquals(null, dao.rows[reportId])
        assertFalse(photo.exists())
    }

    @Test
    fun `row is kept when the server does not confirm all attachments`() = runTest {
        val photo = photoFile("one.jpg")
        val paths = listOf(photo.absolutePath)
        dao.rows[reportId] = entity(
            report = report(photos = paths),
            localPhotoPaths = paths
        )
        // The RPC returned, but the end state is not visible yet.
        repository.saveReportReturnsSuccessWithoutPersistence = true

        val outcome = processor.process(reportId)

        assertTrue(outcome is ReportUploadOutcome.Retry)
        assertEquals(UNCONFIRMED_UPLOAD_MESSAGE, (outcome as ReportUploadOutcome.Retry).message)
        val row = dao.rows.getValue(reportId)
        assertEquals(ReportUploadState.QUEUED.name, row.state)
        assertEquals(1, row.attempts)
        assertEquals(UNCONFIRMED_UPLOAD_MESSAGE, row.lastError)
        assertTrue(photo.exists())
    }

    @Test
    fun `failed upload below the attempt limit is queued for retry`() = runTest {
        repository.saveReportException = RuntimeException("timeout")
        val photo = photoFile("one.jpg")
        dao.rows[reportId] = entity(localPhotoPaths = listOf(photo.absolutePath))

        val outcome = processor.process(reportId)

        assertTrue(outcome is ReportUploadOutcome.Retry)
        val row = dao.rows.getValue(reportId)
        assertEquals(ReportUploadState.QUEUED.name, row.state)
        assertEquals(1, row.attempts)
        assertTrue(photo.exists())
    }

    @Test
    fun `failed upload at the attempt limit becomes failed`() = runTest {
        repository.saveReportException = RuntimeException("timeout")
        dao.rows[reportId] = entity(attempts = MAX_UPLOAD_ATTEMPTS - 1)

        val outcome = processor.process(reportId)

        assertTrue(outcome is ReportUploadOutcome.Failed)
        val row = dao.rows.getValue(reportId)
        assertEquals(ReportUploadState.FAILED.name, row.state)
        assertEquals(MAX_UPLOAD_ATTEMPTS, row.attempts)
    }

    @Test
    fun `failed row is not uploaded again without explicit retry`() = runTest {
        dao.rows[reportId] = entity(state = ReportUploadState.FAILED, attempts = MAX_UPLOAD_ATTEMPTS)

        val outcome = processor.process(reportId)

        assertEquals(ReportUploadOutcome.Missing, outcome)
        assertEquals(0, repository.saveReportCallCount)
    }

    @Test
    fun `missing row is reported without calling the repository`() = runTest {
        val outcome = processor.process(UUID.randomUUID())

        assertEquals(ReportUploadOutcome.Missing, outcome)
        assertEquals(0, repository.saveReportCallCount)
    }
}

/** Hand-written DAO backed by a map; test-only implementation of the Room contract. */
private class FakePendingReportUploadDao : PendingReportUploadDao {

    val rows = linkedMapOf<UUID, PendingReportUploadEntity>()
    val progress = mutableMapOf<UUID, Pair<Int, Int>>()
    private val allRows = MutableStateFlow<List<PendingReportUploadEntity>>(emptyList())

    override fun observeAll(): Flow<List<PendingReportUploadEntity>> = allRows

    override suspend fun getAll(): List<PendingReportUploadEntity> = rows.values.toList()

    override suspend fun getById(id: UUID): PendingReportUploadEntity? = rows[id]

    override suspend fun getOlderThan(cutoff: Long): List<PendingReportUploadEntity> =
        rows.values.filter { it.createdAt < cutoff }

    override suspend fun upsert(entity: PendingReportUploadEntity) {
        rows[entity.id] = entity
        allRows.value = rows.values.toList()
    }

    override suspend fun markUploading(id: UUID, attempts: Int, state: String) {
        rows[id] = rows.getValue(id).copy(state = state, attempts = attempts, uploadedPhotos = 0)
    }

    override suspend fun resetForRetry(id: UUID, attempts: Int, state: String) {
        rows[id] = rows.getValue(id).copy(
            state = state,
            attempts = attempts,
            uploadedPhotos = 0,
            lastError = null
        )
    }

    override suspend fun markWaiting(id: UUID, attempts: Int, error: String?, state: String) {
        rows[id] = rows.getValue(id).copy(
            state = state,
            attempts = attempts,
            lastError = error,
            uploadedPhotos = 0
        )
    }

    override suspend fun markFailed(id: UUID, error: String?, state: String) {
        rows[id] = rows.getValue(id).copy(state = state, lastError = error)
    }

    override suspend fun updateProgress(id: UUID, uploaded: Int, total: Int) {
        progress[id] = uploaded to total
        rows[id]?.let { rows[id] = it.copy(uploadedPhotos = uploaded, totalPhotos = total) }
    }

    override suspend fun deleteById(id: UUID) {
        rows.remove(id)
        allRows.value = rows.values.toList()
    }

    override suspend fun deleteAll() {
        rows.clear()
        allRows.value = emptyList()
    }
}
