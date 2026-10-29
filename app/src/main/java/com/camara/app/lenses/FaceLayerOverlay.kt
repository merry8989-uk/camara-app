package com.camara.app.lenses

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.max

/**
 * Draws the face-only effect layer over the camera preview.
 *
 * The layer is frame-sized and transparent everywhere the face is not, so the hardware
 * preview shows through everywhere else. It uses the same FILL_CENTER mapping as
 * [LensRenderer], which is what keeps it aligned with the preview underneath.
 *
 * Nothing here is fatal if it goes wrong: a null layer simply draws nothing.
 */
@Composable
fun FaceLayerOverlay(
    layer: Bitmap?,
    mirrored: Boolean,
    modifier: Modifier = Modifier,
) {
    if (layer == null || layer.isRecycled || layer.width <= 0 || layer.height <= 0) return

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val aspect = layer.width.toFloat() / layer.height.toFloat()
        val displayedW = max(w, h * aspect)
        val displayedH = displayedW / aspect
        val offX = (w - displayedW) / 2f
        val offY = (h - displayedH) / 2f

        val src = Rect(0, 0, layer.width, layer.height)
        val dst = RectF(offX, offY, offX + displayedW, offY + displayedH)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            if (mirrored) {
                // Mirror about the vertical centre line, the same way the preview is
                // mirrored for the front camera.
                native.save()
                native.scale(-1f, 1f, w / 2f, h / 2f)
                native.drawBitmap(layer, src, dst, paint)
                native.restore()
            } else {
                native.drawBitmap(layer, src, dst, paint)
            }
        }
    }
}
