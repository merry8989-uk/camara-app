package com.camara.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Bitmap loading / saving helpers with EXIF orientation handling. */
object BitmapUtils {

    /**
     * Decodes [uri], downscaling so the longest edge is at most [maxDim].
     *
     * The default is deliberately 2560 rather than something larger: a 4096x3072 ARGB
     * bitmap is ~50 MB on its own, and the editor holds a preview, a face mask and export
     * intermediates at the same time, which is enough to OOM a 2 GB device.
     */
    fun decodeUri(context: Context, uri: Uri, maxDim: Int = 2560): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        var sample = 1
        val largest = maxOf(opts.outWidth, opts.outHeight)
        while (largest / sample > maxDim) sample *= 2

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        val rotation = readRotation(context, uri)
        return if (rotation != 0f) {
            val m = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated !== bmp) bmp.recycle()
            rotated
        } else {
            bmp
        }
    }

    private fun readRotation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                when (ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (t: Throwable) {
            0f
        }
    }

    fun toJpegBytes(bitmap: Bitmap, quality: Int = 95): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    fun rotate(src: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return src
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    fun flip(src: Bitmap, horizontal: Boolean): Bitmap {
        val m = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }
}
