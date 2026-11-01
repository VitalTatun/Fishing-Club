package com.example.fishing.data.cleanup

import android.app.Application
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class OrphanPhotoCleanupRunnerTest {

    private class FakeCleaner : OrphanPhotoCleaner {
        var receivedOlderThan: Instant? = null
        var report = OrphanPhotoCleanupReport(scanned = 4, deleted = 2)
        var failure: Exception? = null

        override suspend fun cleanup(olderThan: Instant): OrphanPhotoCleanupReport {
            receivedOlderThan = olderThan
            failure?.let { throw it }
            return report
        }
    }

    @Test
    fun `pass uses a 24 hour grace period`() = runTest {
        val cleaner = FakeCleaner()
        val runner = OrphanPhotoCleanupRunner(cleaner)
        val now = Instant.parse("2026-11-01T12:00:00Z")

        assertTrue(runner.run(now))

        assertEquals(now.minus(Duration.ofHours(24)), cleaner.receivedOlderThan)
        assertEquals(Duration.ofHours(24), OrphanPhotoCleanupRunner.GRACE)
    }

    @Test
    fun `failure is reported as retryable instead of crashing the worker`() = runTest {
        val cleaner = FakeCleaner().apply {
            failure = IllegalStateException("offline")
        }
        val runner = OrphanPhotoCleanupRunner(cleaner)

        assertFalse(runner.run(Instant.parse("2026-11-01T12:00:00Z")))
    }
}
