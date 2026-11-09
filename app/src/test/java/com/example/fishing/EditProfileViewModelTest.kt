package com.example.fishing

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.MainDispatcherRule
import com.example.fishing.viewmodel.EditProfileViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * AUD-01: avatar processing (bitmap decode/scale/compress/file write) and the
 * temp-file cleanup must run on the injected [CoroutineDispatcher], never on the
 * Main dispatcher where [EditProfileViewModel] is launched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var context: Context
    private val workers = mutableListOf<ExecutorCoroutineDispatcher>()

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        workers.forEach { it.close() }
    }

    private fun newWorkerDispatcher(): CoroutineDispatcher {
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "edit-profile-io")
        }
        return executor.asCoroutineDispatcher().also { workers += it }
    }

    private fun createJpeg(width: Int, height: Int): File {
        val file = File(context.cacheDir, "avatar_${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        bitmap.recycle()
        return file
    }

    @Test
    fun `avatar processing is dispatched to the io dispatcher`() = runTest {
        val ioTestDispatcher = StandardTestDispatcher()
        val countingDispatcher = CountingDispatcher(ioTestDispatcher)
        val repository = FakeAuthRepository()
        val viewModel = EditProfileViewModel(repository, countingDispatcher)
        val source = createJpeg(width = 120, height = 80)

        viewModel.updateProfile(
            context.contentResolver,
            context.filesDir,
            "Новое имя",
            Uri.fromFile(source)
        )

        // The photo step must be handed to the IoDispatcher instead of running
        // inline on Main: nothing may have been written or uploaded yet.
        assertEquals(1, countingDispatcher.dispatches.get())
        assertTrue(viewModel.isLoading.value)
        assertEquals(0, repository.updateProfileCalls)

        ioTestDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.updateProfileCalls)
        assertEquals("Новое имя", repository.lastUpdateProfileName)
        val savedPath = repository.lastUpdateProfileImage
        assertNotNull(savedPath)
        val photosDir = File(context.filesDir, "photos")
        assertTrue(savedPath!!.startsWith(photosDir.path + File.separator))
        assertTrue(viewModel.saveSuccess.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
        // Cleanup after the upload also belongs on the IoDispatcher.
        assertTrue(countingDispatcher.dispatches.get() >= 2)
        assertFalse("temp avatar copy must be cleaned up", File(savedPath).exists())
    }

    @Test
    fun `photo is saved and the temp copy removed on a worker thread`() = runBlocking {
        val repository = FakeAuthRepository()
        val viewModel = EditProfileViewModel(repository, newWorkerDispatcher())
        val source = createJpeg(width = 200, height = 150)

        viewModel.updateProfile(
            context.contentResolver,
            context.filesDir,
            "Новое имя",
            Uri.fromFile(source)
        )

        withTimeout(10_000) { viewModel.saveSuccess.first { it } }

        assertEquals(1, repository.updateProfileCalls)
        val savedPath = repository.lastUpdateProfileImage
        assertNotNull(savedPath)
        assertTrue(File(savedPath!!).startsWith(File(context.filesDir, "photos")))
        withTimeout(10_000) { viewModel.isLoading.first { !it } }
        assertFalse("temp avatar copy must be cleaned up", File(savedPath).exists())
    }

    @Test
    fun `unreadable photo does not block the profile save`() = runBlocking {
        val repository = FakeAuthRepository()
        val viewModel = EditProfileViewModel(repository, newWorkerDispatcher())
        val missing = File(context.cacheDir, "missing_${UUID.randomUUID()}.jpg")

        viewModel.updateProfile(
            context.contentResolver,
            context.filesDir,
            "Новое имя",
            Uri.fromFile(missing)
        )

        withTimeout(10_000) { viewModel.saveSuccess.first { it } }

        assertEquals(1, repository.updateProfileCalls)
        assertNull(repository.lastUpdateProfileImage)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `repository failure surfaces an error and stops loading`() = runBlocking {
        val repository = FakeAuthRepository().apply {
            updateProfileResult = Result.failure(Exception("Unable to resolve host"))
        }
        val viewModel = EditProfileViewModel(repository, newWorkerDispatcher())

        viewModel.updateProfile(context.contentResolver, context.filesDir, "Новое имя", null)

        withTimeout(10_000) { viewModel.error.first { it != null } }
        assertEquals("Нет соединения с интернетом", viewModel.error.value)
        withTimeout(10_000) { viewModel.isLoading.first { !it } }
        assertFalse(viewModel.saveSuccess.value)
    }

    /** Records every dispatch so a test can prove the work left the Main thread. */
    private class CountingDispatcher(
        private val delegate: CoroutineDispatcher
    ) : CoroutineDispatcher() {
        val dispatches = AtomicInteger(0)

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatches.incrementAndGet()
            delegate.dispatch(context, block)
        }
    }
}
