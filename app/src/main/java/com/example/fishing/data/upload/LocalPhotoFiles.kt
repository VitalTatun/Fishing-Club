package com.example.fishing.data.upload

import android.content.Context
import java.io.File

/** Deletes queue-owned photos without ever following paths outside the cache. */
internal object LocalPhotoFiles {

    fun delete(context: Context, paths: Iterable<String>) {
        val photosDir = File(context.filesDir, "photos").canonicalFile
        paths.forEach { path ->
            runCatching {
                val file = File(path).canonicalFile
                if (file.path.startsWith(photosDir.path + File.separator)) {
                    file.delete()
                }
            }
        }
    }
}
