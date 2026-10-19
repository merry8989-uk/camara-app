package com.camara.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Launches the system share sheet for a piece of media. */
object ShareUtils {

    fun shareMedia(context: Context, uri: Uri, mimeType: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share via").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // LocalContext in Compose is the Activity, so no NEW_TASK flag is needed.
        runCatching { context.startActivity(chooser) }
    }
}
