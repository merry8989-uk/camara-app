package com.camara.app.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.TextureOverlay
import androidx.media3.effect.OverlayEffect

/**
 * A text or sticker overlay placed on the video.
 *
 * Positions and sizes are **fractions of the frame**, not pixels, so the same overlay looks
 * identical in the preview at any size and in the exported video at its own resolution.
 */
data class VideoOverlay(
    val id: Long,
    val kind: Kind,
    val text: String = "",
    val glyph: String = "",
    val color: Long = 0xFFFFFFFF,
    /** Height of the text or sticker, as a fraction of the frame height. */
    val sizeFraction: Float = 0.09f,
    /** Centre of the overlay, as a fraction of the frame. */
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    /** When it is on screen, in milliseconds from the start of the clip. */
    val startMs: Float = 0f,
    val endMs: Float = Float.MAX_VALUE,
) {
    enum class Kind { TEXT, STICKER }

    fun isVisibleAt(positionMs: Float): Boolean = positionMs >= startMs && positionMs <= endMs

    /** True when the overlay spans the whole clip, so the UI can say "the whole video". */
    fun isFullLength(clipMs: Float): Boolean = startMs <= 0f && endMs >= clipMs
}

/**
 * Draws overlays and turns them into the effects Media3 bakes into the export.
 *
 * The drawing routine is shared: the editor preview renders it into a Compose canvas, and
 * the export renders the same thing into a full-frame bitmap. One implementation means the
 * preview cannot drift from the result.
 */
@OptIn(UnstableApi::class)
object VideoOverlays {

    private const val SHADOW_ALPHA = 0x99000000.toInt()

    /** Draws [overlay] onto [canvas], which is [width] x [height] pixels. */
    fun draw(canvas: Canvas, overlay: VideoOverlay, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val cx = overlay.x * width
        val cy = overlay.y * height
        val size = (height * overlay.sizeFraction).coerceAtLeast(1f)

        when (overlay.kind) {
            VideoOverlay.Kind.TEXT -> {
                if (overlay.text.isBlank()) return
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = overlay.color.toInt()
                    textSize = size
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    // A shadow keeps light text readable over a light frame.
                    setShadowLayer(size * 0.06f, 0f, size * 0.02f, SHADOW_ALPHA)
                }
                canvas.drawText(overlay.text, cx, cy, paint)
            }

            VideoOverlay.Kind.STICKER -> {
                if (overlay.glyph.isBlank()) return
                val bmp = glyphBitmap(overlay.glyph, size.toInt().coerceAtLeast(1))
                canvas.drawBitmap(bmp, cx - bmp.width / 2f, cy - bmp.height / 2f, null)
                bmp.recycle()
            }
        }
    }

    /** A full-frame transparent bitmap with [overlay] drawn in place. */
    fun renderFrame(overlay: VideoOverlay, frameWidth: Int, frameHeight: Int): Bitmap {
        val out = Bitmap.createBitmap(
            frameWidth.coerceAtLeast(1),
            frameHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        draw(Canvas(out), overlay, out.width.toFloat(), out.height.toFloat())
        return out
    }

    /**
     * The Media3 effects that bake [overlays] into the exported video.
     *
     * Each overlay becomes a full-frame bitmap, so its position needs no anchor arithmetic
     * and lands exactly where the preview showed it. Visibility is decided per frame from
     * the presentation timestamp, which is what makes a title that only shows at the start
     * possible.
     */
    fun effects(overlays: List<VideoOverlay>, frameWidth: Int, frameHeight: Int): List<TextureOverlay> =
        overlays
            .filter { it.text.isNotBlank() || it.glyph.isNotBlank() }
            .map { TimedOverlay(renderFrame(it, frameWidth, frameHeight), it.startMs, it.endMs) }

    fun overlayEffect(overlays: List<VideoOverlay>, frameWidth: Int, frameHeight: Int): OverlayEffect? {
        val textureOverlays = effects(overlays, frameWidth, frameHeight)
        return if (textureOverlays.isEmpty()) null else OverlayEffect(textureOverlays)
    }

    /** Draws an emoji or short glyph centred in a square bitmap. */
    private fun glyphBitmap(glyph: String, sizePx: Int): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.86f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val baseline = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
        Canvas(out).drawText(glyph, sizePx / 2f, baseline, paint)
        return out
    }

}

/** One shared transparent pixel, used to hide an overlay outside its time window. */
@Volatile
private var blankPixel: Bitmap? = null

private fun blankPixel(): Bitmap {
    blankPixel?.let { return it }
    val made = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    made.eraseColor(0)
    blankPixel = made
    return made
}

/**
 * A [BitmapOverlay] that is only drawn between two timestamps.
 *
 * `getBitmap` cannot return null, so outside the window it returns a single fully
 * transparent pixel — invisible, and the texture cache sees a different generation id and
 * swaps correctly.
 */
@OptIn(UnstableApi::class)
private class TimedOverlay(
    private val frame: Bitmap,
    private val startMs: Float,
    private val endMs: Float,
) : BitmapOverlay() {

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val ms = presentationTimeUs / 1000f
        return if (ms >= startMs && ms <= endMs) frame else blankPixel()
    }
}
