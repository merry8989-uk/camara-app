package com.camara.app.lenses

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * CameraX [ImageAnalysis.Analyzer] that runs ML Kit face detection and reports the
 * largest face as a normalised [FaceState], including landmarks and contours.
 *
 * Contours are what make lenses follow the real face shape. Note ML Kit only
 * returns contours for the single most prominent face, which is exactly what a
 * face lens wants.
 */
class FaceAnalyzer(
    private val onFace: (FaceState?) -> Unit
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
                onFace(best?.let { FaceMapper.run { it.toFaceState(uprightW, uprightH) } })
            }
            .addOnFailureListener { onFace(null) }
            .addOnCompleteListener { imageProxy.close() }
    }
}
