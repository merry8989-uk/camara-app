package com.camara.app.settings

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * The one place the contact handle lives (Rule 4 / Rule 5). Change it here and it
 * changes in the Settings footer.
 */
object Contact {

    const val INSTAGRAM_HANDLE = "osintgram_io"
    const val INSTAGRAM_URL = "https://instagram.com/osintgram_io"

    /** Opens Instagram for the handle — the app if installed, else the browser. */
    fun openInstagram(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
