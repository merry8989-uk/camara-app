package com.camara.app.lenses

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

/**
 * CameraX [ImageAnalysis.Analyzer] that runs ML Kit face detection and reports the
 * largest face as a normalised [FaceState].
 *
 * ML Kit reports the bounding box in the *upright* (rotation-applied) image space,
 * so we normalise against the upright frame dimensions, swapping width/height for
 * quarter-turn rotations. Callbacks arrive on the analysis executor.
 */
class FaceAnalyzer(
    private val onFace: (FaceState?) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.12f)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val quarterTurn = rotation == 90 || rotation == 270
        val uprightW = (if (quarterTurn) imageProxy.height else imageProxy.width).toFloat().coerceAtLeast(1f)
        val uprightH = (if (quarterTurn) imageProxy.width else imageProxy.height).toFloat().coerceAtLeast(1f)

        val input = InputImage.fromMediaImage(mediaImage, rotation)
        detector.process(input)
            .addOnSuccessListener { faces ->
                val best = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                onFace(best?.toState(uprightW, uprightH))
            }
            .addOnFailureListener { onFace(null) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun Face.toState(imgW: Float, imgH: Float): FaceState {
        val box = boundingBox
        fun landmark(type: Int): Offset? =
            getLandmark(type)?.position?.let { Offset(it.x / imgW, it.y / imgH) }

        return FaceState(
            cx = (((box.left + box.right) / 2f) / imgW).coerceIn(0f, 1f),
            cy = (((box.top + box.bottom) / 2f) / imgH).coerceIn(0f, 1f),
            w = box.width() / imgW,
            h = box.height() / imgH,
            rollDegrees = headEulerAngleZ,
            imageAspect = imgW / imgH,
            leftEye = landmark(FaceLandmark.LEFT_EYE),
            rightEye = landmark(FaceLandmark.RIGHT_EYE),
            nose = landmark(FaceLandmark.NOSE_BASE),
            mouth = landmark(FaceLandmark.MOUTH_BOTTOM)
        )
    }
}
