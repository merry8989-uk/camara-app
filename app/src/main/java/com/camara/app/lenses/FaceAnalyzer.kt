package com.camara.app.lenses

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camara.app.beauty.BeautySettings
import com.camara.app.face.FaceCompositor
import com.camara.app.face.FaceTracker
import com.camara.app.filters.Filter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/** The look the preview should apply, or null when there is nothing to apply. */
data class FaceLook(
    val filter: Filter,
    val intensity: Float,
    val beauty: BeautySettings,
) {
    val isActive: Boolean
        get() = (!filter.isIdentity && intensity > 0.01f) || beauty.hasGrade || beauty.smooth > 0.01f
}

/**
 * CameraX analyzer that runs ML Kit face detection and, when a look is active, composites
 * the effect onto **the face only**, publishing a layer the preview draws over itself.
 *
 * Three things make this usable rather than a slideshow:
 *
 *  - detection runs at ~8 fps, not per frame; [FaceTracker] carries the pose in between;
 *  - the analysis stream is RGBA_8888 at 480×640, so getting pixels costs no YUV
 *    conversion and compositing is cheap;
 *  - every failure is swallowed and reported as "no layer", so the hardware preview keeps
 *    running and the worst case is simply no live effect.
 */
class FaceAnalyzer(
    private val look: () -> FaceLook?,
    private val onFace: (FaceState?) -> Unit,
    private val onLayer: (Bitmap?) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.12f)
            .build()
    )

    private val tracker = FaceTracker()

    @Volatile
    private var busy = false
    private var lastDetectAt = 0L
    private var lastLayerWasNull: Boolean? = null

    override fun analyze(imageProxy: ImageProxy) {
        var frame: Bitmap? = null
        try {
            val now = SystemClock.elapsedRealtime()
            val raw = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            frame = upright(raw, rotation)
            if (frame !== raw) raw.recycle()

            // Detection is the expensive part. Run it a few times a second and let the
            // tracker carry the pose in between — that is what the smoothing is for.
            if (!busy && now - lastDetectAt >= DETECT_INTERVAL_MS) {
                busy = true
                lastDetectAt = now
                detect(frame, now)
            }

            val active = look()
            val pose = tracker.current
            if (active == null) {
                publish(null)
            } else if (pose != null) {
                // Compositing is skipped while the pose is only being held: the frame no
                // longer contains the face, so the effect would land on empty background.
                val layer = if (tracker.holding) {
                    null
                } else {
                    FaceCompositor.faceLayer(frame, pose, active.filter, active.intensity, active.beauty)
                }
                publish(layer)
            } else {
                publish(null)
            }
        } catch (t: Throwable) {
            publish(null)
        } finally {
            // The layer must not reference the frame bitmap, and it does not: the
            // compositor returns a new bitmap. Safe to release the frame here.
            frame?.recycle()
            imageProxy.close()
        }
    }

    private fun publish(layer: Bitmap?) {
        // Avoid handing the same null to Compose on every single frame.
        if (layer == null && lastLayerWasNull == true) return
        lastLayerWasNull = layer == null
        onLayer(layer)
    }

    private fun detect(frame: Bitmap, now: Long) {
        detector.process(InputImage.fromBitmap(frame, 0))
            .addOnSuccessListener { faces ->
                val best = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                val state = best?.let {
                    FaceMapper.run { it.toFaceState(frame.width.toFloat(), frame.height.toFloat()) }
                }
                // The tracker decides whether this is a new pose, a refinement, or a
                // dropout worth holding.
                val tracked = tracker.update(state, now)
                onFace(tracked)
            }
            .addOnFailureListener {
                val tracked = tracker.update(null, SystemClock.elapsedRealtime())
                onFace(tracked)
            }
            .addOnCompleteListener { busy = false }
    }

    /** Rotates the analysis frame upright, so face coordinates and pixels share a space. */
    private fun upright(src: Bitmap, rotation: Int): Bitmap {
        if (rotation % 360 == 0) return src
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    fun release() {
        tracker.reset()
        runCatching { detector.close() }
    }

    private companion object {
        /** ~8 fps of detection. The tracker interpolates and holds between. */
        const val DETECT_INTERVAL_MS = 120L
    }
}
