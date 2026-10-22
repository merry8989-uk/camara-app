package com.camara.app.core

import android.content.Context
import android.graphics.Bitmap
import com.camara.app.beauty.FaceBeauty
import com.camara.app.beauty.FaceMasker
import com.camara.app.editor.image.ImageEditState
import com.camara.app.editor.image.ImageExporter
import com.camara.app.lenses.Lens
import com.camara.app.lenses.LensRenderer
import com.camara.app.portrait.PortraitBokeh
import com.camara.app.portrait.SubjectSegmenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The single still-image pipeline used by both the camera (baking a capture) and
 * the image editor (exporting an edit).
 *
 * Order matters:
 *   1. geometry      rotate / flip / crop
 *   2. face-only beauty   smoothing and grading confined to the face
 *   3. colour        filter at its chosen strength, composed with adjustments
 *   4. portrait      real background bokeh behind the chosen subject
 *   5. lens          AR pieces baked in, anchored to the detected face
 *   6. overlays      strokes, stickers, text, frame
 *
 * Face detection runs once and is reused by both the beauty and lens steps.
 */
object PhotoProcessor {

    suspend fun process(
        context: Context,
        source: Bitmap,
        edit: ImageEditState,
        lens: Lens? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        // Each stage returns a new bitmap and the previous one is finished with, so it is
        // released as we go — otherwise a full export holds six full-size bitmaps at once,
        // which is enough to OOM a 2 GB device on a 12 MP photo. `source` belongs to the
        // caller and is never released. See [ReleasingChain].
        val chain = ReleasingChain(source) { it.recycle() }
        chain.advance(ImageExporter.renderGeometry(source, edit))

        val lensActive = lens != null && lens.art.isNotEmpty()
        val needsFace = !edit.beauty.isDefault || lensActive
        val face = if (needsFace) FaceMasker.detectFace(chain.value) else null

        if (!edit.beauty.isDefault) {
            chain.advance(FaceBeauty.apply(chain.value, edit.beauty, face))
        }

        chain.advance(ImageExporter.renderColour(chain.value, edit))

        if (edit.portrait.enabled) {
            val mask = SubjectSegmenter.subjectMask(context, chain.value, edit.portrait.subjectPoint)
            chain.advance(PortraitBokeh.apply(chain.value, mask, edit.portrait.strength))
            mask?.recycle()
        }

        if (lensActive && face != null) {
            chain.advance(LensRenderer.bake(chain.value, lens!!, face))
        }

        chain.advance(ImageExporter.renderOverlays(chain.value, edit))
        chain.value
    }
}
