package com.camara.app.lenses

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Draws the active [lens] over the camera preview.
 *
 * [LensRenderer] handles the FILL_CENTER mapping, so the overlay just hands it the
 * view size and whether the preview is mirrored.
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
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        drawIntoCanvas { canvas ->
            LensRenderer.draw(
                canvas = canvas.nativeCanvas,
                lens = lens,
                face = face,
                viewW = size.width,
                viewH = size.height,
                mirrored = mirrored
            )
        }
    }
}
