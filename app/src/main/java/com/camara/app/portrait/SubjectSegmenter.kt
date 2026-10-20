package com.camara.app.portrait

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.camara.app.util.Blur
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Separates the subject from the background using ML Kit's on-device selfie
 * segmenter, then turns the confidence map into a soft alpha mask.
 *
 * The user can tap a point to choose *which* subject stays sharp: we keep the
 * connected blob of foreground under the tap and drop the rest. Without a tap we
 * keep the largest blob.
 *
 * Work happens on a downscaled copy — the mask only needs to be smooth, not
 * pixel-exact — which keeps it fast and light on memory.
 */
object SubjectSegmenter {

    private const val WORK_MAX = 1024
    private const val LOW = 0.35f
    private const val HIGH = 0.75f

    suspend fun subjectMask(context: Context, bitmap: Bitmap, subjectPoint: Offset?): Bitmap? =
        withContext(Dispatchers.Default) {
            try {
                val scale = WORK_MAX.toFloat() / max(bitmap.width, bitmap.height)
                val work = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    bitmap
                }

                val segmenter = Segmentation.getClient(
                    SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build()
                )
                val mask = try {
                    Tasks.await(
                        segmenter.process(InputImage.fromBitmap(work, 0)),
                        8,
                        TimeUnit.SECONDS
                    )
                } finally {
                    try {
                        segmenter.close()
                    } catch (_: Throwable) {
                    }
                }

                val mw = mask.width
                val mh = mask.height
                val buffer = mask.buffer
                buffer.rewind()
                val confidence = FloatArray(mw * mh)
                buffer.asFloatBuffer().get(confidence)

                // Confidence -> alpha with a smooth ramp, so edges stay soft.
                val alpha = IntArray(mw * mh)
                for (i in alpha.indices) {
                    val t = ((confidence[i] - LOW) / (HIGH - LOW)).coerceIn(0f, 1f)
                    alpha[i] = (t * t * (3f - 2f * t) * 255f).toInt()
                }

                val seed = subjectPoint?.let {
                    val x = (it.x * mw).toInt().coerceIn(0, mw - 1)
                    val y = (it.y * mh).toInt().coerceIn(0, mh - 1)
                    y * mw + x
                } ?: -1
                val kept = keepComponent(alpha, mw, mh, seed)

                val pixels = IntArray(mw * mh)
                for (i in pixels.indices) {
                    pixels[i] = (kept[i] shl 24) or 0x00FFFFFF
                }
                val small = Bitmap.createBitmap(pixels, mw, mh, Bitmap.Config.ARGB_8888)

                val full = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
                small.recycle()
                if (work !== bitmap) work.recycle()

                val feather = max(2, (minOf(bitmap.width, bitmap.height) * 0.012f).toInt())
                val feathered = Blur.featherMask(full, feather)
                full.recycle()
                feathered
            } catch (t: Throwable) {
                null
            }
        }

    /**
     * Alpha of the foreground blob containing [seed]. If [seed] is -1 or lands on
     * background, the largest blob wins.
     */
    private fun keepComponent(alpha: IntArray, w: Int, h: Int, seed: Int): IntArray {
        val n = w * h
        val visited = BooleanArray(n)
        val members = IntArray(n)
        val stack = IntArray(n)
        val scratch = IntArray(n)

        // Fills `scratch` with the blob reachable from `start`; returns its size.
        fun flood(start: Int): Int {
            var top = 0
            var count = 0
            stack[top++] = start
            visited[start] = true
            while (top > 0) {
                val idx = stack[--top]
                scratch[count++] = idx
                val x = idx % w
                val y = idx / w
                if (x > 0 && !visited[idx - 1] && alpha[idx - 1] > 0) {
                    visited[idx - 1] = true; stack[top++] = idx - 1
                }
                if (x < w - 1 && !visited[idx + 1] && alpha[idx + 1] > 0) {
                    visited[idx + 1] = true; stack[top++] = idx + 1
                }
                if (y > 0 && !visited[idx - w] && alpha[idx - w] > 0) {
                    visited[idx - w] = true; stack[top++] = idx - w
                }
                if (y < h - 1 && !visited[idx + w] && alpha[idx + w] > 0) {
                    visited[idx + w] = true; stack[top++] = idx + w
                }
            }
            return count
        }

        fun materialise(count: Int): IntArray {
            val out = IntArray(n)
            for (i in 0 until count) out[scratch[i]] = alpha[scratch[i]]
            return out
        }

        if (seed in 0 until n && alpha[seed] > 0) {
            return materialise(flood(seed))
        }

        var bestCount = 0
        for (i in 0 until n) {
            if (!visited[i] && alpha[i] > 0) {
                val count = flood(i)
                if (count > bestCount) {
                    bestCount = count
                    System.arraycopy(scratch, 0, members, 0, count)
                }
            }
        }
        val out = IntArray(n)
        for (i in 0 until bestCount) out[members[i]] = alpha[members[i]]
        return out
    }
}
