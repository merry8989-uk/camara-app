package com.camara.app.editor.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.camara.app.core.ReleasingChain
import com.camara.app.filters.ColorMatrixMath
import com.camara.app.stickers.PlacedSticker
import com.camara.app.util.BitmapUtils
import kotlin.math.max
import kotlin.math.min

/**
 * Flattens an edit into a single bitmap.
 *
 * The work is split into stages so a caller can interleave face-aware steps
 * (face-only beauty, portrait bokeh, lens baking) between them — see
 * `core.PhotoProcessor`.
 */
object ImageExporter {

    // ------------------------------------------------------------- stage 1

    /** Rotation, flips and crop. */
    fun renderGeometry(source: Bitmap, edit: ImageEditState): Bitmap {
        // Rotate, flip and crop each allocate. Release each intermediate as the next one
        // arrives, but never the caller's `source`. See [ReleasingChain].
        val chain = ReleasingChain(source) { it.recycle() }
        if (edit.rotationDeg != 0f) chain.advance(BitmapUtils.rotate(chain.value, edit.rotationDeg))
        if (edit.flipH) chain.advance(BitmapUtils.flip(chain.value, true))
        if (edit.flipV) chain.advance(BitmapUtils.flip(chain.value, false))
        if (edit.crop != CropRect.FULL) chain.advance(cropTo(chain.value, edit.crop))
        return chain.value
    }

    // ------------------------------------------------------------- stage 2

    /** Filter (at its chosen strength) composed with manual adjustments. */
    fun renderColour(src: Bitmap, edit: ImageEditState): Bitmap {
        val filterMatrix = edit.filter.matrixAt(edit.filterIntensity)
        val needsColour = !edit.filter.isIdentity || !edit.adjustments.isDefault
        if (!needsColour) return src
        val matrix = ColorMatrixMath.compose(filterMatrix, edit.adjustments.matrix())
        return applyMatrix(src, matrix)
    }

    // ------------------------------------------------------------- stage 3

    /** Strokes, stickers, text and frame. */
    fun renderOverlays(src: Bitmap, edit: ImageEditState): Bitmap {
        if (edit.strokes.isEmpty() && edit.stickers.isEmpty() &&
            edit.texts.isEmpty() && edit.frame == FrameStyle.NONE
        ) {
            return src
        }
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        drawStrokes(canvas, out, edit.strokes)
        drawStickers(canvas, out, edit.stickers)
        drawTexts(canvas, out, edit.texts)
        drawFrame(canvas, out, edit.frame)
        return out
    }

    // -------------------------------------------------------- convenience

    /** Geometry + colour + overlays, with no face-aware steps. */
    fun render(source: Bitmap, edit: ImageEditState): Bitmap {
        var bmp = renderGeometry(source, edit)
        bmp = renderColour(bmp, edit)
        return renderOverlays(bmp, edit)
    }

    private fun applyMatrix(src: Bitmap, matrix: FloatArray): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** Cuts [src] to the normalised [rect]. */
    fun cropTo(src: Bitmap, rect: CropRect): Bitmap {
        val x = (rect.left * src.width).toInt().coerceIn(0, src.width - 1)
        val y = (rect.top * src.height).toInt().coerceIn(0, src.height - 1)
        val w = (rect.width * src.width).toInt().coerceIn(1, src.width - x)
        val h = (rect.height * src.height).toInt().coerceIn(1, src.height - y)
        return Bitmap.createBitmap(src, x, y, w, h)
    }

    private fun drawStrokes(canvas: Canvas, out: Bitmap, strokes: List<DrawStroke>) {
        strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = stroke.colorArgb
                strokeWidth = max(2f, stroke.widthFraction * min(out.width, out.height))
            }
            val path = Path()
            val first = stroke.points.first()
            path.moveTo(first.x * out.width, first.y * out.height)
            stroke.points.drop(1).forEach { p ->
                path.lineTo(p.x * out.width, p.y * out.height)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawStickers(canvas: Canvas, out: Bitmap, stickers: List<PlacedSticker>) {
        val base = out.width * 0.16f
        stickers.forEach { s ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = base * s.scale
                typeface = Typeface.DEFAULT_BOLD
                color = Color.WHITE
            }
            val cx = s.x * out.width
            val cy = s.y * out.height
            canvas.save()
            canvas.rotate(s.rotation, cx, cy)
            val metrics = paint.fontMetrics
            canvas.drawText(s.emoji, cx, cy - (metrics.ascent + metrics.descent) / 2f, paint)
            canvas.restore()
        }
    }

    private fun drawTexts(canvas: Canvas, out: Bitmap, texts: List<TextItem>) {
        texts.forEach { t ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = t.sizeFraction * out.width * t.scale
                typeface = Typeface.DEFAULT_BOLD
                color = t.colorArgb
                setShadowLayer(6f, 0f, 2f, 0xAA000000.toInt())
            }
            val cx = t.x * out.width
            val cy = t.y * out.height
            canvas.save()
            canvas.rotate(t.rotation, cx, cy)
            val metrics = paint.fontMetrics
            canvas.drawText(t.text, cx, cy - (metrics.ascent + metrics.descent) / 2f, paint)
            canvas.restore()
        }
    }

    private fun drawFrame(canvas: Canvas, out: Bitmap, frame: FrameStyle) {
        val w = out.width.toFloat()
        val h = out.height.toFloat()
        val minDim = min(w, h)
        when (frame) {
            FrameStyle.NONE -> Unit
            FrameStyle.WHITE -> {
                val bw = minDim * 0.035f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.WHITE
                    strokeWidth = bw
                }
                val inset = bw / 2f
                canvas.drawRect(inset, inset, w - inset, h - inset, paint)
            }
            FrameStyle.ROUNDED -> {
                val bw = minDim * 0.03f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.WHITE
                    strokeWidth = bw
                }
                val r = minDim * 0.05f
                val inset = bw / 2f
                canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), r, r, paint)
            }
            FrameStyle.FILM -> {
                val bar = h * 0.075f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
                canvas.drawRect(0f, 0f, w, bar, paint)
                canvas.drawRect(0f, h - bar, w, h, paint)
                val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                val count = 12
                val holeW = w / (count * 2f)
                for (i in 0 until count) {
                    val x = (i * 2 + 0.5f) * holeW
                    canvas.drawRect(x, bar * 0.3f, x + holeW, bar * 0.7f, hole)
                    canvas.drawRect(x, h - bar * 0.7f, x + holeW, h - bar * 0.3f, hole)
                }
            }
            FrameStyle.VIGNETTE -> {
                val shader = RadialGradient(
                    w / 2f, h / 2f, max(w, h) * 0.75f,
                    intArrayOf(0x00000000, 0x00000000, 0xAA000000.toInt()),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, Paint().apply { this.shader = shader })
            }
        }
    }
}
