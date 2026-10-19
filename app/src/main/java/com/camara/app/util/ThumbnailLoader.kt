package com.camara.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads square-ish thumbnails for gallery tiles without pulling full-size media. */
object ThumbnailLoader {

    suspend fun load(context: Context, uri: Uri, isVideo: Boolean, size: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(size, size), null)
                } else {
                    legacy(context, uri, isVideo, size)
                }
            } catch (t: Throwable) {
                null
            }
        }

    @Suppress("DEPRECATION")
    private fun legacy(context: Context, uri: Uri, isVideo: Boolean, size: Int): Bitmap? {
        return if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.getFrameAtTime(0)
            } finally {
                retriever.release()
            }
        } else {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }
    }
}
