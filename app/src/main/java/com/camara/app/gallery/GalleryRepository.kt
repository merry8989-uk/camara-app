package com.camara.app.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class MediaEntry(
    val uri: Uri,
    val isVideo: Boolean,
    val dateAdded: Long,
    val durationMs: Long
)

/** Reads captured photos and videos back out of the shared media store. */
object GalleryRepository {

    fun load(context: Context, limit: Int = 400): List<MediaEntry> {
        val items = ArrayList<MediaEntry>(limit)
        items += queryImages(context)
        items += queryVideos(context)
        return items.sortedByDescending { it.dateAdded }.take(limit)
    }

    private fun queryImages(context: Context): List<MediaEntry> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        val out = ArrayList<MediaEntry>()
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                out += MediaEntry(
                    uri = ContentUris.withAppendedId(collection, id),
                    isVideo = false,
                    dateAdded = cursor.getLong(dateCol),
                    durationMs = 0L
                )
            }
        }
        return out
    }

    private fun queryVideos(context: Context): List<MediaEntry> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION
        )
        val out = ArrayList<MediaEntry>()
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                out += MediaEntry(
                    uri = ContentUris.withAppendedId(collection, id),
                    isVideo = true,
                    dateAdded = cursor.getLong(dateCol),
                    durationMs = cursor.getLong(durCol)
                )
            }
        }
        return out
    }
}
