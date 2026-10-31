package com.example.fishing.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.math.roundToInt

object PhotoUtils {
    /** Longest side of a stored photo. */
    private const val MAX_DIMENSION = 2048

    /** Hard cap for a single processed JPEG, matching the bucket limit policy. */
    private const val MAX_OUTPUT_BYTES = 3L * 1024 * 1024

    private const val JPEG_QUALITY = 85
    private const val MIN_JPEG_QUALITY = 55
    private const val QUALITY_STEP = 15
    private const val DOWNSCALE_FACTOR = 0.75f
    private const val MAX_COMPRESSION_ATTEMPTS = 6

    fun createTempPhotoFile(context: Context): File {
        val storageDir = context.cacheDir
        return File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        )
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Removes temporary photos written by the app. Safe to call on start-up and
     * on logout: an interrupted edit is not resumable, and a pending upload keeps
     * its files only while the process is alive.
     */
    fun clearSavedPhotos(filesDir: File) {
        val photosDir = File(filesDir, "photos")
        photosDir.listFiles()?.forEach { file ->
            runCatching { file.delete() }
        }
    }

    /**
     * Persists a captured camera photo as a processed JPEG. The camera app writes
     * a temporary file; this method reads it once, applies EXIF orientation and
     * stores the result in `filesDir/photos`.
     */
    fun persistCameraPhoto(
        contentResolver: ContentResolver,
        filesDir: File,
        sourceUri: Uri
    ): Uri? {
        val path = copyPhotoToInternalStorage(contentResolver, filesDir, sourceUri) ?: return null
        return Uri.fromFile(File(path))
    }

    /**
     * Copies a picked photo into `filesDir/photos`, applying EXIF orientation,
     * limiting the longest side to [MAX_DIMENSION] and re-encoding it as JPEG.
     * Re-encoding strips EXIF, including GPS coordinates.
     *
     * Returns the absolute path, or `null` when the image cannot be decoded or
     * exceeds [MAX_OUTPUT_BYTES] even after compression.
     */
    fun copyPhotoToInternalStorage(
        contentResolver: ContentResolver,
        filesDir: File,
        uri: Uri
    ): String? {
        val photosDir = File(filesDir, "photos")

        // Camera photos are already processed by persistCameraPhoto; do not
        // decode and re-compress them a second time.
        val existingFile = uri.takeIf { it.scheme == "file" }?.path?.let(::File)
        if (existingFile != null && existingFile.isFile && isInside(photosDir, existingFile)) {
            return existingFile.absolutePath
        }

        return try {
            val bitmap = decodeBitmap(contentResolver, uri) ?: return null
            val oriented = applyExifOrientation(contentResolver, uri, bitmap)
            val scaled = scaleToMaxDimension(oriented)
            try {
                photosDir.mkdirs()
                val outputFile = File(photosDir, "photo_${UUID.randomUUID()}.jpg")
                val written = compressWithinLimit(scaled, outputFile)
                if (written == null || written.length() == 0L) {
                    outputFile.delete()
                    null
                } else {
                    written.absolutePath
                }
            } finally {
                if (scaled !== oriented) scaled.recycle()
                if (oriented !== bitmap) oriented.recycle()
                bitmap.recycle()
            }
        } catch (e: OutOfMemoryError) {
            // A very large source image must fail gracefully instead of crashing.
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(contentResolver, uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return openStream(contentResolver, uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    /**
     * Halves the decoded size until the longest side is at most twice the target,
     * keeping enough pixels for a high-quality final scale.
     */
    private fun calculateInSampleSize(width: Int, height: Int): Int {
        val longest = maxOf(width, height)
        var sampleSize = 1
        while (longest / (sampleSize * 2) >= MAX_DIMENSION) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyExifOrientation(
        contentResolver: ContentResolver,
        uri: Uri,
        bitmap: Bitmap
    ): Bitmap {
        val orientation = readOrientation(contentResolver, uri)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        // If the rotated copy cannot be allocated, keep the original rather than
        // losing the photo entirely.
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun readOrientation(contentResolver: ContentResolver, uri: Uri): Int {
        return try {
            openStream(contentResolver, uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun scaleToMaxDimension(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true
        )
    }

    /**
     * Compresses the bitmap, lowering quality and then dimensions until the file
     * fits into [MAX_OUTPUT_BYTES]. Returns `null` if that is not achievable.
     */
    private fun compressWithinLimit(bitmap: Bitmap, outputFile: File): File? {
        var current = bitmap
        var scaledCopy: Bitmap? = null
        var quality = JPEG_QUALITY
        try {
            repeat(MAX_COMPRESSION_ATTEMPTS) {
                FileOutputStream(outputFile).use { output ->
                    current.compress(Bitmap.CompressFormat.JPEG, quality, output)
                }
                if (outputFile.length() <= MAX_OUTPUT_BYTES) {
                    return outputFile
                }
                if (quality > MIN_JPEG_QUALITY) {
                    quality = maxOf(MIN_JPEG_QUALITY, quality - QUALITY_STEP)
                } else {
                    val smaller = Bitmap.createScaledBitmap(
                        current,
                        (current.width * DOWNSCALE_FACTOR).roundToInt().coerceAtLeast(1),
                        (current.height * DOWNSCALE_FACTOR).roundToInt().coerceAtLeast(1),
                        true
                    )
                    if (smaller !== current) {
                        scaledCopy?.recycle()
                        scaledCopy = smaller
                        current = smaller
                    }
                }
            }
            return null
        } finally {
            scaledCopy?.recycle()
        }
    }

    private fun openStream(contentResolver: ContentResolver, uri: Uri): InputStream? {
        return contentResolver.openInputStream(uri)
    }

    private fun isInside(directory: File, file: File): Boolean {
        return try {
            file.canonicalPath.startsWith(directory.canonicalPath + File.separator)
        } catch (e: IOException) {
            false
        }
    }
}
