package com.camara.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Helpers for inserting captured media into the shared gallery. */
object MediaStoreUtils {

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun createImageUri(context: Context): Uri {
        val name = "CAMARA_${stamp()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Camara")
            }
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create image entry")
    }

    fun createVideoUri(context: Context): Uri {
        val name = "CAMARA_${stamp()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Camara")
            }
        }
        return context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create video entry")
    }

    fun createExportImageUri(context: Context, suffix: String): Uri {
        val name = "CAMARA_edit_${stamp()}_$suffix.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Camara")
            }
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create export entry")
    }

    fun createExportVideoUri(context: Context): Uri {
        val name = "CAMARA_edit_${stamp()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Camara")
            }
        }
        return context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create export entry")
    }

    /** Writes a bitmap to the given content Uri as JPEG. */
    fun writeImage(context: Context, uri: Uri, data: ByteArray) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
            ?: error("Could not open $uri for writing")
    }

    fun cacheFile(context: Context, name: String): File {
        val dir = File(context.cacheDir, "camara")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, name)
    }
}
