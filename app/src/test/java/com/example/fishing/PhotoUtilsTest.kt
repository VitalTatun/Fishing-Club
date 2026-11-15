package com.example.fishing

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.fishing.utils.PhotoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class PhotoUtilsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    private fun createJpeg(width: Int, height: Int): File {
        val file = File(context.cacheDir, "source_${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        bitmap.recycle()
        return file
    }

    @Test
    fun `applies exif rotation and strips metadata`() {
        val source = createJpeg(width = 40, height = 20)
        ExifInterface(source.absolutePath).apply {
            setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString()
            )
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "55/1,45/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            saveAttributes()
        }

        val resultPath = PhotoUtils.copyPhotoToInternalStorage(
            context.contentResolver,
            context.filesDir,
            Uri.fromFile(source)
        )

        assertNotNull(resultPath)
        val decoded = BitmapFactory.decodeFile(resultPath)
        assertEquals(20, decoded.width)
        assertEquals(40, decoded.height)
        decoded.recycle()

        val resultExif = ExifInterface(resultPath!!)
        val resultOrientation = resultExif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        // Rotation is baked into the pixels, so the orientation tag is either
        // absent or explicitly normal — never a rotation.
        assertTrue(
            resultOrientation == ExifInterface.ORIENTATION_NORMAL ||
                resultOrientation == ExifInterface.ORIENTATION_UNDEFINED
        )
        assertNull(resultExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
    }

    @Test
    fun `limits the longest side to 2048 px`() {
        val source = createJpeg(width = 2100, height = 1000)

        val resultPath = PhotoUtils.copyPhotoToInternalStorage(
            context.contentResolver,
            context.filesDir,
            Uri.fromFile(source)
        )

        assertNotNull(resultPath)
        val decoded = BitmapFactory.decodeFile(resultPath)
        assertEquals(2048, decoded.width)
        assertTrue(decoded.height <= 2048)
        decoded.recycle()
    }

    @Test
    fun `already processed camera photo is not re-encoded`() {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val processed = File(photosDir, "processed_${UUID.randomUUID()}.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }

        val result = PhotoUtils.copyPhotoToInternalStorage(
            context.contentResolver,
            context.filesDir,
            Uri.fromFile(processed)
        )

        assertEquals(processed.absolutePath, result)
    }

    @Test
    fun `clearSavedPhotos keeps recently modified draft files`() {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val fresh = File(photosDir, "fresh.jpg").apply { writeBytes(byteArrayOf(1)) }

        PhotoUtils.clearSavedPhotos(context.filesDir, emptySet())

        assertTrue(fresh.exists())
    }

    @Test
    fun `clearSavedPhotos removes stale orphan files`() {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val stale = File(photosDir, "stale.jpg").apply { writeBytes(byteArrayOf(1)) }
        val eightDaysAgo = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000
        assertTrue(stale.setLastModified(eightDaysAgo))

        PhotoUtils.clearSavedPhotos(context.filesDir, emptySet())

        assertTrue(!stale.exists())
    }

    @Test
    fun `clearSavedPhotos applies the seven day grace window`() {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val withinGrace = File(photosDir, "within_grace.jpg").apply { writeBytes(byteArrayOf(1)) }
        val pastGrace = File(photosDir, "past_grace.jpg").apply { writeBytes(byteArrayOf(2)) }
        val now = System.currentTimeMillis()
        // An hour of margin on either side keeps this deterministic while still
        // pinning the cutoff to seven days instead of "anything not brand new".
        assertTrue(withinGrace.setLastModified(now - 167L * 60 * 60 * 1000))
        assertTrue(pastGrace.setLastModified(now - 169L * 60 * 60 * 1000))

        PhotoUtils.clearSavedPhotos(context.filesDir, emptySet())

        assertTrue(withinGrace.exists())
        assertFalse(pastGrace.exists())
    }

    @Test
    fun `clearSavedPhotos strict mode removes fresh files too`() {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val fresh = File(photosDir, "fresh.jpg").apply { writeBytes(byteArrayOf(1)) }
        val queued = File(photosDir, "queued.jpg").apply { writeBytes(byteArrayOf(2)) }

        PhotoUtils.clearSavedPhotos(
            context.filesDir,
            keepPaths = setOf(queued.absolutePath),
            graceMs = 0L,
        )

        // Logout/account deletion must not leave the session's photos behind,
        // but uploads referenced by the queue keep their files.
        assertFalse(fresh.exists())
        assertTrue(queued.exists())
    }

    @Test
    fun `clearSessionPhotos applies the strict logout policy`() {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val fresh = File(photosDir, "session.jpg").apply { writeBytes(byteArrayOf(1)) }

        PhotoUtils.clearSessionPhotos(context.filesDir, context.contentResolver)

        // Logout/account deletion must not leave the session's fresh photos.
        assertFalse(fresh.exists())
    }

    @Test
    fun `clearSavedPhotos keeps a file with an unavailable timestamp`() {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val unknown = File(photosDir, "unknown_time.jpg").apply { writeBytes(byteArrayOf(1)) }
        Assume.assumeTrue(
            "Filesystem cannot store an epoch timestamp",
            unknown.setLastModified(0L)
        )
        Assume.assumeTrue(unknown.lastModified() == 0L)

        PhotoUtils.clearSavedPhotos(context.filesDir, emptySet())

        // lastModified() == 0 means "unknown", which must not be read as "ancient".
        assertTrue(unknown.exists())
    }

    @Test
    fun `clearSavedPhotos keeps queued files regardless of age`() {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val queued = File(photosDir, "queued.jpg").apply { writeBytes(byteArrayOf(1)) }
        val stale = File(photosDir, "orphan.jpg").apply { writeBytes(byteArrayOf(2)) }
        val eightDaysAgo = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000
        assertTrue(queued.setLastModified(eightDaysAgo))
        assertTrue(stale.setLastModified(eightDaysAgo))

        PhotoUtils.clearSavedPhotos(context.filesDir, setOf(queued.absolutePath))

        assertTrue(queued.exists())
        assertTrue(!stale.exists())
    }
}
