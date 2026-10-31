package com.example.fishing

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.fishing.utils.PhotoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `clearSavedPhotos removes temporary files`() {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val first = File(photosDir, "first.jpg").apply { writeBytes(byteArrayOf(1)) }
        val second = File(photosDir, "second.jpg").apply { writeBytes(byteArrayOf(2)) }

        PhotoUtils.clearSavedPhotos(context.filesDir)

        assertTrue(!first.exists())
        assertTrue(!second.exists())
    }
}
