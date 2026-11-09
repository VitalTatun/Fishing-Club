package com.example.fishing.data

import android.app.Application
import androidx.room.Room
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.report.ReportRemoteQueries
import com.example.fishing.data.report.RoomHomeFeedStore
import com.example.fishing.data.report.RoomMarkerStore
import com.example.fishing.data.report.RoomReportDetailsStore
import com.example.fishing.data.report.SupabaseFavoritesStore
import com.example.fishing.data.report.SupabaseLikesStore
import com.example.fishing.data.report.SupabasePhotoUrlProvider
import com.example.fishing.data.report.SupabaseReportWriter
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import com.example.fishing.model.FishingPhoto
import com.example.fishing.model.FishingReport
import com.example.fishing.model.FishingType
import com.example.fishing.model.User
import com.example.fishing.model.Water
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class SupabaseFishingRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var auth: FakeAuthRepository
    private lateinit var remote: FakeReportRemoteDataSource
    private lateinit var repository: SupabaseFishingRepository

    private val user = User(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        name = "Test User",
        email = "test@example.com",
        image = ""
    )

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        auth = FakeAuthRepository().apply { sessionUser = user }
        remote = FakeReportRemoteDataSource()
        val queries = mockk<ReportRemoteQueries>(relaxed = true)
        val likesStore = SupabaseLikesStore(
            supabase = mockk(relaxed = true),
            authRepository = auth,
            database = db,
            reportDetailsDao = db.reportDetailsDao(),
            likeDao = db.likeDao(),
            reportRemoteQueries = queries
        )
        val photoUrlProvider = SupabasePhotoUrlProvider(supabase = mockk(relaxed = true))
        repository = SupabaseFishingRepository(
            homeFeedStore = RoomHomeFeedStore(
                reportRemoteQueries = queries,
                likesStore = likesStore,
                reportDetailsDao = db.reportDetailsDao(),
                database = db,
                authRepository = auth
            ),
            reportDetailsStore = RoomReportDetailsStore(
                reportRemoteQueries = queries,
                likesStore = likesStore,
                reportDetailsDao = db.reportDetailsDao(),
                database = db,
                authRepository = auth
            ),
            markerStore = RoomMarkerStore(
                reportRemoteQueries = queries,
                markerDao = db.markerDao(),
                database = db,
                authRepository = auth
            ),
            favoritesStore = SupabaseFavoritesStore(
                supabase = mockk(relaxed = true),
                authRepository = auth,
                database = db,
                reportDetailsDao = db.reportDetailsDao(),
                favoriteReportDao = db.favoriteDao(),
                reportRemoteQueries = queries
            ),
            likesStore = likesStore,
            reportWriter = SupabaseReportWriter(
                authRepository = auth,
                reportRemoteDataSource = remote,
                reportDetailsDao = db.reportDetailsDao(),
                markerDao = db.markerDao(),
                favoriteReportDao = db.favoriteDao(),
                likeDao = db.likeDao(),
                database = db,
                photoUrlProvider = photoUrlProvider,
                ioDispatcher = mainDispatcherRule.testDispatcher
            ),
            photoUrlProvider = photoUrlProvider
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun localPhoto(name: String): FishingPhoto {
        val file = File(RuntimeEnvironment.getApplication().cacheDir, name).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        // Absolute path => treated as a new local photo (not a storage path).
        return FishingPhoto(id = UUID.randomUUID(), url = file.absolutePath)
    }

    private fun report(
        photos: List<FishingPhoto>,
        isPublic: Boolean = true
    ): FishingReport = FishingReport(
        id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
        userId = user.id,
        type = FishingType.FISHING_LOG,
        name = "Спиннинг • Щука",
        water = Water(waterName = "Озеро Нарочь", latitude = 54.85, longitude = 26.77),
        photos = photos,
        fishingStartAt = Instant.now().minusSeconds(3600),
        fishingEndAt = Instant.now(),
        weight = 2.0,
        fish = listOf(Fish(id = UUID.randomUUID(), name = "Щука", count = 1)),
        fishingMethod = FishingMethod.SPINNING,
        bait = listOf(Bait.WOBBLER),
        comment = "",
        user = user,
        fishingFromTheShore = true,
        isPublic = isPublic
    )

    private fun expectedStoragePath(report: FishingReport, photo: FishingPhoto): String {
        val extension = File(photo.url).extension.ifEmpty { "jpg" }
        return "${report.userId}/${report.id}/${photo.id}.$extension"
    }

    private fun markerCount(): Int =
        db.query("SELECT COUNT(*) FROM markers", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun cachedReportCount(): Int =
        db.query("SELECT COUNT(*) FROM report_details", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    @Test
    fun `successful publish uploads photos, writes the cache row and deletes removed objects`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val first = localPhoto("first.jpg")
            val second = localPhoto("second.jpg")
            val existingPath = "user/report/existing.jpg"
            val report = report(
                listOf(
                    FishingPhoto(id = UUID.randomUUID(), url = existingPath),
                    first,
                    second
                )
            )
            remote.removedPhotoPaths = listOf("user/report/old.jpg")
            remote.publishedReportId = report.id
            val progress = mutableListOf<Pair<Int, Int>>()

            val result = repository.saveReport(report) { uploaded, total ->
                progress += uploaded to total
            }

            assertTrue(result.isSuccess)
            assertEquals(
                listOf(expectedStoragePath(report, first), expectedStoragePath(report, second)),
                remote.uploadedPaths
            )
            // One already-published photo + two uploads.
            assertEquals(listOf(1 to 3, 2 to 3, 3 to 3), progress)
            assertEquals(listOf(listOf("user/report/old.jpg")), remote.deletedPathBatches)

            val payload = remote.publishedPayloads.single()
            assertEquals(report.id.toString(), payload["id"]?.jsonPrimitive?.content)
            assertTrue(payload["is_public"]!!.jsonPrimitive.boolean)
            assertNotNull(payload["published_at"]?.jsonPrimitive?.content)
            assertEquals(1, payload["fish"]!!.jsonArray.size)
            assertEquals(1, payload["baits"]!!.jsonArray.size)
            assertEquals(3, payload["photos"]!!.jsonArray.size)

            val cached = db.reportDetailsDao().getByIdOneShot(report.id)
            assertNotNull(cached)
            assertEquals(
                listOf(
                    existingPath,
                    expectedStoragePath(report, first),
                    expectedStoragePath(report, second)
                ),
                cached!!.imageUrls
            )
            assertEquals(1, markerCount())
        }

    @Test
    fun `failed second upload removes photos uploaded in this attempt and leaves no row`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val first = localPhoto("first.jpg")
            val second = localPhoto("second.jpg")
            val report = report(listOf(first, second))
            remote.uploadFailureAtCall = 2

            val result = repository.saveReport(report)

            assertTrue(result.isFailure)
            assertNull(db.reportDetailsDao().getByIdOneShot(report.id))
            assertEquals(0, markerCount())
            assertEquals(
                listOf(listOf(expectedStoragePath(report, first))),
                remote.deletedPathBatches
            )
            assertTrue(remote.publishedPayloads.isEmpty())
        }

    @Test
    fun `failed publish removes uploaded photos and leaves no partial rows`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val photo = localPhoto("photo.jpg")
            val report = report(listOf(photo))
            remote.publishFailure = IllegalStateException("rpc failed")

            val result = repository.saveReport(report)

            assertTrue(result.isFailure)
            assertNull(db.reportDetailsDao().getByIdOneShot(report.id))
            assertEquals(0, markerCount())
            assertEquals(
                listOf(listOf(expectedStoragePath(report, photo))),
                remote.deletedPathBatches
            )
        }

    @Test
    fun `retry after a lost publish acknowledgement does not upload the photo again`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val photo = localPhoto("photo.jpg")
            val report = report(listOf(photo))
            val storagePath = expectedStoragePath(report, photo)
            remote.publishFailure = IllegalStateException("timeout after commit")
            remote.publishFailureAfterCommit = true

            val first = repository.saveReport(report)

            // The server committed, but the client only saw a timeout. The
            // objects it references must not be compensated away.
            assertTrue(first.isFailure)
            assertEquals(1, remote.uploadCalls)
            assertTrue(remote.exists)
            assertEquals(setOf(storagePath), remote.attachedPaths)
            assertTrue(remote.deletedPathBatches.isEmpty())
            assertTrue(File(photo.url).exists())

            remote.publishFailure = null
            remote.publishFailureAfterCommit = false
            val retry = repository.saveReport(report)

            assertTrue(retry.isSuccess)
            // The retry sees the object in Storage and skips the upload.
            assertEquals(1, remote.uploadCalls)
            assertEquals(2, remote.publishAttempts)
            assertEquals(1, cachedReportCount())
            assertEquals(1, markerCount())
            assertEquals(storagePath, db.reportDetailsDao().getByIdOneShot(report.id)!!.imageUrls.single())
        }

    @Test
    fun `a photo object left by an earlier attempt is not uploaded again`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val photo = localPhoto("photo.jpg")
            val report = report(listOf(photo))
            val storagePath = expectedStoragePath(report, photo)
            // A previous attempt uploaded the object, then the RPC failed while
            // the compensating delete could not reach Storage.
            remote.objects += storagePath

            val result = repository.saveReport(report)

            assertTrue(result.isSuccess)
            assertEquals(0, remote.uploadCalls)
            assertEquals(1, remote.publishAttempts)
            assertEquals(setOf(storagePath), remote.attachedPaths)
        }

    @Test
    fun `an existing report without its photo still uploads the missing object`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val photo = localPhoto("photo.jpg")
            val report = report(listOf(photo))
            val storagePath = expectedStoragePath(report, photo)
            // The report row survived an earlier partial attempt, but the photo
            // object never reached Storage.
            remote.exists = true

            val result = repository.saveReport(report)

            assertTrue(result.isSuccess)
            assertEquals(1, remote.uploadCalls)
            assertEquals(setOf(storagePath), remote.attachedPaths)
            assertEquals(setOf(storagePath), remote.objects)
        }

    @Test
    fun `isReportSynced requires the report, its attachment rows and the stored object`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val photo = localPhoto("photo.jpg")
            val report = report(listOf(photo))
            val storagePath = expectedStoragePath(report, photo)
            remote.exists = true
            remote.attachedPaths += storagePath
            remote.objects += storagePath

            assertTrue(repository.isReportSynced(report))

            remote.attachedPaths.clear()
            assertFalse(repository.isReportSynced(report))

            remote.attachedPaths += storagePath
            remote.objects.clear()
            assertFalse(repository.isReportSynced(report))

            remote.objects += storagePath
            remote.exists = false
            assertFalse(repository.isReportSynced(report))
        }

    @Test
    fun `retry after a failed publish reuses the same report id and does not duplicate rows`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val photo = localPhoto("photo.jpg")
            val report = report(listOf(photo))
            remote.publishFailure = IllegalStateException("offline")

            assertTrue(repository.saveReport(report).isFailure)
            remote.publishFailure = null

            val retry = repository.saveReport(report)

            assertTrue(retry.isSuccess)
            assertEquals(2, remote.publishAttempts)
            // Deterministic paths: the retry overwrites the same object instead of
            // creating a second one.
            assertEquals(listOf(expectedStoragePath(report, photo)), remote.uploadedPaths)
            assertEquals(
                listOf(expectedStoragePath(report, photo)),
                db.reportDetailsDao().getByIdOneShot(report.id)!!.imageUrls
            )
            assertEquals(1, cachedReportCount())
            assertEquals(1, markerCount())
        }

    @Test
    fun `existing report on the server keeps uploaded files when the RPC fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val photo = localPhoto("photo.jpg")
            val report = report(listOf(photo))
            remote.exists = true
            remote.publishFailure = IllegalStateException("rpc failed")

            val result = repository.saveReport(report)

            assertTrue(result.isFailure)
            assertTrue(remote.deletedPathBatches.isEmpty())
        }

    @Test
    fun `failed cleanup of removed photos does not fail a published report`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val photo = localPhoto("photo.jpg")
            val report = report(listOf(photo))
            remote.removedPhotoPaths = listOf("user/report/old.jpg")
            remote.deleteFailure = IllegalStateException("storage unavailable")

            val result = repository.saveReport(report)

            assertTrue(result.isSuccess)
            assertNotNull(db.reportDetailsDao().getByIdOneShot(report.id))
        }

    @Test
    fun `cancellation is propagated and never compensated as an error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val first = localPhoto("first.jpg")
            val second = localPhoto("second.jpg")
            val report = report(listOf(first, second))
            remote.uploadFailure = CancellationException("cancelled")
            remote.uploadFailureAtCall = 2

            var cancellation: CancellationException? = null
            try {
                repository.saveReport(report)
            } catch (e: CancellationException) {
                cancellation = e
            }

            assertNotNull(cancellation)
            assertTrue(remote.deletedPathBatches.isEmpty())
            assertNull(db.reportDetailsDao().getByIdOneShot(report.id))
        }

    @Test
    fun `saving a report for another user is rejected before touching the server`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val report = report(listOf(localPhoto("photo.jpg")))
                .copy(userId = UUID.randomUUID())

            val result = repository.saveReport(report)

            assertTrue(result.isFailure)
            assertTrue(remote.uploadedPaths.isEmpty())
            assertTrue(remote.publishedPayloads.isEmpty())
            assertFalse(result.exceptionOrNull() is CancellationException)
        }

    @Test
    fun `a draft is stored without published_at`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val report = report(listOf(localPhoto("photo.jpg")), isPublic = false)

            assertTrue(repository.saveReport(report).isSuccess)

            val payload = remote.publishedPayloads.single()
            assertFalse(payload["is_public"]!!.jsonPrimitive.boolean)
            assertNull(payload["published_at"])
            assertNull(db.reportDetailsDao().getByIdOneShot(report.id)!!.publishedAt)
        }

    @Test
    fun `deleteReport removes the remote report and the local cache rows`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val report = report(listOf(FishingPhoto(UUID.randomUUID(), "user/report/photo.jpg")))
            assertTrue(repository.saveReport(report).isSuccess)

            val result = repository.deleteReport(report.id)

            assertTrue(result.isSuccess)
            assertEquals(listOf(report.id), remote.deletedReportIds)
            assertNull(db.reportDetailsDao().getByIdOneShot(report.id))
            assertEquals(0, markerCount())
        }

    @Test
    fun `failed remote deletion keeps the local cache for a retry`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val report = report(listOf(FishingPhoto(UUID.randomUUID(), "user/report/photo.jpg")))
            assertTrue(repository.saveReport(report).isSuccess)
            remote.deleteReportFailure = IllegalStateException("offline")

            val result = repository.deleteReport(report.id)

            assertTrue(result.isFailure)
            assertNotNull(db.reportDetailsDao().getByIdOneShot(report.id))
            assertEquals(1, markerCount())
        }

    private class FakeReportRemoteDataSource : ReportRemoteDataSource {
        var exists = false
        /** Storage objects currently present in the bucket. */
        val objects = mutableSetOf<String>()
        /** `fishing_photos` rows attached to the report. */
        val attachedPaths = mutableSetOf<String>()
        var uploadedPaths = mutableListOf<String>()
        var uploadCalls = 0
        var uploadFailureAtCall: Int? = null
        var uploadFailure: Exception = IllegalStateException("upload failed")
        var publishFailure: Exception? = null
        /** Simulates a timeout after the RPC already committed on the server. */
        var publishFailureAfterCommit = false
        var publishAttempts = 0
        var publishedReportId: UUID? = null
        var removedPhotoPaths: List<String> = emptyList()
        val publishedPayloads = mutableListOf<JsonObject>()
        val deletedPathBatches = mutableListOf<List<String>>()
        val deletedReportIds = mutableListOf<UUID>()
        var deleteFailure: Exception? = null
        var deleteReportFailure: Exception? = null

        override suspend fun reportExists(reportId: UUID): Boolean = exists

        override suspend fun reportPhotoPaths(reportId: UUID): List<String> =
            attachedPaths.toList()

        override suspend fun photoExists(storagePath: String): Boolean =
            storagePath in objects

        override suspend fun uploadPhoto(storagePath: String, bytes: ByteArray) {
            uploadCalls++
            if (uploadCalls == uploadFailureAtCall) throw uploadFailure
            // Storage upserts at deterministic paths, so a retry overwrites
            // the existing object instead of adding another one.
            uploadedPaths.remove(storagePath)
            uploadedPaths += storagePath
            objects += storagePath
        }

        override suspend fun publishReport(payload: JsonObject): PublishReportResult {
            publishAttempts++
            if (publishFailure != null && !publishFailureAfterCommit) {
                throw publishFailure!!
            }
            publishedPayloads += payload
            // The RPC is atomic: once it commits, both the report row and every
            // photo row are visible to the next read.
            exists = true
            attachedPaths.clear()
            attachedPaths += payload["photos"]!!.jsonArray.map {
                it.jsonObject.getValue("storage_path").jsonPrimitive.content
            }
            publishFailure?.let { throw it }
            return PublishReportResult(publishedReportId, removedPhotoPaths)
        }

        override suspend fun deletePhotos(paths: List<String>) {
            deleteFailure?.let { throw it }
            deletedPathBatches += paths
            objects -= paths.toSet()
        }

        override suspend fun deleteReport(reportId: UUID) {
            deleteReportFailure?.let { throw it }
            deletedReportIds += reportId
            exists = false
            attachedPaths.clear()
        }
    }
}
