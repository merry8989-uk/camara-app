package com.camara.app.lenses

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Draws the active [lens] anchored to the detected [face], mapped from the upright
 * analysis frame onto the preview surface.
 *
 * The preview uses FILL_CENTER, so the displayed image is
 * `max(viewW, viewH * aspect)` wide; the face's normalised coordinates are mapped
 * through that box. For the front camera the horizontal axis is mirrored to match
 * the preview.
 */
@Composable
fun LensOverlay(
    lens: Lens?,
    face: FaceState?,
    mirrored: Boolean,
    modifier: Modifier = Modifier
) {
    if (lens == null || lens.art.isEmpty() || face == null) return

    Canvas(modifier = modifier) {
        val viewW = size.width
        val viewH = size.height
        if (viewW <= 0f || viewH <= 0f) return@Canvas

        val aspect = if (face.imageAspect > 0f) face.imageAspect else 0.75f
        val displayedW = maxOf(viewW, viewH * aspect)
        val displayedH = displayedW / aspect
        val offsetX = (viewW - displayedW) / 2f
        val offsetY = (viewH - displayedH) / 2f

        val nx = if (mirrored) 1f - face.cx else face.cx
        val cx = offsetX + nx * displayedW
        val cy = offsetY + face.cy * displayedH
        val faceW = face.w * displayedW
        val faceH = face.h * displayedH
        val roll = if (mirrored) -face.rollDegrees else face.rollDegrees

        drawIntoCanvas { canvas ->
            LensRenderer.draw(
                canvas = canvas.nativeCanvas,
                lens = lens,
                cx = cx,
                cy = cy,
                faceW = faceW,
                faceH = faceH,
                roll = roll
            )
        }
    }
}
