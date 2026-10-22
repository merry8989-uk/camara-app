package com.camara.app.util

import android.Manifest
import android.os.Build

/** Central place for the runtime permissions Camara needs, version-aware. */
object Permissions {

    val CAMERA = arrayOf(Manifest.permission.CAMERA)

    /**
     * Requested when entering the camera. On API < 29 the shared media store can only be
     * written with WRITE_EXTERNAL_STORAGE — captures *and* saved edits both go through
     * MediaStore, so without it they fail silently on Android 7–9.
     */
    val CAMERA_AND_MIC: Array<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    /** Permission set required to read the gallery, which changed in API 33. */
    fun mediaRead(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** Permission needed to write media on pre-scoped-storage devices (API <= 28). */
    fun mediaWrite(): Array<String> =
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            emptyArray()
        }
}
