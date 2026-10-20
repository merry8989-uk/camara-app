package com.camara.app.beauty

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.camara.app.lenses.FaceMapper
import com.camara.app.lenses.FaceState
import com.camara.app.util.Blur
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Builds a soft-edged mask for the face in a still image.
 *
 * Two uses:
 *  - face-only beauty (smoothing and grading are confined to the face)
 *  - keeping a subject sharp while the background is blurred
 *
 * The mask follows the real face contour when ML Kit provides one, falling back to
 * an ellipse fitted to the bounding box.
 */
object FaceMasker {

    /** Runs face detection on a bitmap. Blocking — call from a background thread. */
    fun detectFace(bitmap: Bitmap): FaceState? {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setMinFaceSize(0.06f)
                .build()
        )
        return try {
            val faces = Tasks.await(
                detector.process(InputImage.fromBitmap(bitmap, 0)),
                6,
                TimeUnit.SECONDS
            )
            val best = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            best?.let {
                FaceMapper.run { it.toFaceState(bitmap.width.toFloat(), bitmap.height.toFloat()) }
            }
        } catch (t: Throwable) {
            null
        } finally {
            try {
                detector.close()
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * A white-on-transparent mask covering the face, feathered so beauty blends into
     * the rest of the photo instead of stopping at a hard line.
     */
    fun buildMask(src: Bitmap, face: FaceState, expansion: Float = 1.06f): Bitmap {
        val mask = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)

        val cx = face.cx * src.width
        val cy = face.cy * src.height
        val path = Path()
        face.outline().forEachIndexed { i, p ->
            // Expand slightly outward from the face centre so the jaw line isn't cut.
            val x = cx + (p.x * src.width - cx) * expansion
            val y = cy + (p.y * src.height - cy) * expansion
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
        )

        val feather = max(2, (min(src.width, src.height) * 0.02f).toInt())
        return Blur.featherMask(mask, feather)
    }
}
