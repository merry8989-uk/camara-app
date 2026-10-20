package com.camara.app.lenses

import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark

/**
 * Single place that converts an ML Kit [Face] into Camara's normalised
 * [FaceState]. Shared by the live camera analyzer and the still-image masker so
 * both agree on coordinates and available features.
 */
object FaceMapper {

    fun Face.toFaceState(imgW: Float, imgH: Float): FaceState {
        val box = boundingBox

        fun lm(type: Int): Offset? =
            getLandmark(type)?.position?.let { Offset(it.x / imgW, it.y / imgH) }

        fun contour(type: Int): List<Offset> =
            getContour(type)?.points?.map { Offset(it.x / imgW, it.y / imgH) } ?: emptyList()

        return FaceState(
            cx = (((box.left + box.right) / 2f) / imgW).coerceIn(0f, 1f),
            cy = (((box.top + box.bottom) / 2f) / imgH).coerceIn(0f, 1f),
            w = box.width() / imgW,
            h = box.height() / imgH,
            imageAspect = imgW / imgH,
            leftEye = lm(FaceLandmark.LEFT_EYE),
            rightEye = lm(FaceLandmark.RIGHT_EYE),
            nose = lm(FaceLandmark.NOSE_BASE),
            mouth = lm(FaceLandmark.MOUTH_BOTTOM),
            leftEar = lm(FaceLandmark.LEFT_EAR),
            rightEar = lm(FaceLandmark.RIGHT_EAR),
            leftCheek = lm(FaceLandmark.LEFT_CHEEK),
            rightCheek = lm(FaceLandmark.RIGHT_CHEEK),
            faceContour = contour(FaceContour.FACE),
            leftEyeContour = contour(FaceContour.LEFT_EYE),
            rightEyeContour = contour(FaceContour.RIGHT_EYE),
            upperLipTop = contour(FaceContour.UPPER_LIP_TOP),
            lowerLipBottom = contour(FaceContour.LOWER_LIP_BOTTOM)
        )
    }
}
