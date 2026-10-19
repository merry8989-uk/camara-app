package com.camara.app.lenses

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Draws a [Lens] anchored to a face box. Used for both the live preview (via a
 * Compose canvas) and baking the lens into a captured photo, so the two match.
 *
 * Coordinates are in the target canvas' pixel space. [faceW]/[faceH] are the face
 * box size in pixels; [cx]/[cy] its centre; [roll] its in-plane rotation.
 */
object LensRenderer {

    fun draw(
        canvas: Canvas,
        lens: Lens,
        cx: Float,
        cy: Float,
        faceW: Float,
        faceH: Float,
        roll: Float
    ) {
        if (lens.art.isEmpty()) return
        canvas.save()
        canvas.rotate(roll, cx, cy)
        lens.art.forEach { art ->
            when (art) {
                is EmojiArt -> drawEmoji(canvas, art, cx, cy, faceW, faceH)
                is DrawnArt -> drawDrawn(canvas, art, cx, cy, faceW, faceH)
            }
        }
        canvas.restore()
    }

    private fun drawEmoji(canvas: Canvas, art: EmojiArt, cx: Float, cy: Float, faceW: Float, faceH: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = (art.size * faceW).coerceAtLeast(6f)
            color = Color.WHITE
        }
        val x = cx + art.rx * faceW
        val y = cy + art.ry * faceH
        val fm = paint.fontMetrics
        canvas.drawText(art.emoji, x, y - (fm.ascent + fm.descent) / 2f, paint)
    }

    private fun drawDrawn(canvas: Canvas, art: DrawnArt, cx: Float, cy: Float, faceW: Float, faceH: Float) {
        val x = cx + art.rx * faceW
        val y = cy + art.ry * faceH
        val w = (art.size * faceW).coerceAtLeast(8f)
        when (art.kind) {
            DrawKind.GLASSES -> drawGlasses(canvas, x, y, w)
            DrawKind.CAT_EARS -> drawCatEars(canvas, x, y, w)
            DrawKind.MUSTACHE -> drawMustache(canvas, x, y, w)
            DrawKind.BLUSH -> drawBlush(canvas, x, y, w)
            DrawKind.CROWN -> drawCrown(canvas, x, y, w)
        }
    }

    private fun drawGlasses(canvas: Canvas, x: Float, y: Float, w: Float) {
        val lensW = w * 0.42f
        val lensH = w * 0.30f
        val gap = w * 0.06f
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xE6101014.toInt()
        }
        val left = RectF(x - gap / 2f - lensW, y - lensH / 2f, x - gap / 2f, y + lensH / 2f)
        val right = RectF(x + gap / 2f, y - lensH / 2f, x + gap / 2f + lensW, y + lensH / 2f)
        val r = lensH * 0.35f
        canvas.drawRoundRect(left, r, r, frame)
        canvas.drawRoundRect(right, r, r, frame)
        val bridge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.035f
            color = 0xE6101014.toInt()
        }
        canvas.drawLine(x - gap / 2f, y, x + gap / 2f, y, bridge)
        // Subtle glare on each lens.
        val glare = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40FFFFFF }
        canvas.drawRoundRect(
            RectF(left.left + lensW * 0.15f, left.top + lensH * 0.15f, left.left + lensW * 0.45f, left.top + lensH * 0.40f),
            r, r, glare
        )
        canvas.drawRoundRect(
            RectF(right.left + lensW * 0.15f, right.top + lensH * 0.15f, right.left + lensW * 0.45f, right.top + lensH * 0.40f),
            r, r, glare
        )
    }

    private fun drawCatEars(canvas: Canvas, x: Float, y: Float, w: Float) {
        val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2B2B33.toInt() }
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF9EC4.toInt() }
        val earW = w * 0.34f
        val earH = w * 0.46f
        val dx = w * 0.30f

        listOf(-1f, 1f).forEach { side ->
            val baseX = x + side * dx
            val path = Path().apply {
                moveTo(baseX - earW / 2f, y + earH / 2f)
                lineTo(baseX, y - earH / 2f)
                lineTo(baseX + earW / 2f, y + earH / 2f)
                close()
            }
            canvas.drawPath(path, outer)
            val innerPath = Path().apply {
                moveTo(baseX - earW * 0.22f, y + earH * 0.34f)
                lineTo(baseX, y - earH * 0.22f)
                lineTo(baseX + earW * 0.22f, y + earH * 0.34f)
                close()
            }
            canvas.drawPath(innerPath, inner)
        }
    }

    private fun drawMustache(canvas: Canvas, x: Float, y: Float, w: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2B2B33.toInt()
            style = Paint.Style.FILL
        }
        val half = w * 0.5f
        val h = w * 0.26f
        val left = Path().apply {
            moveTo(x, y)
            cubicTo(x - half * 0.5f, y - h, x - half, y - h * 0.2f, x - half, y + h * 0.35f)
            cubicTo(x - half * 0.55f, y + h * 0.55f, x - half * 0.2f, y + h * 0.5f, x, y + h * 0.1f)
            close()
        }
        val right = Path().apply {
            moveTo(x, y)
            cubicTo(x + half * 0.5f, y - h, x + half, y - h * 0.2f, x + half, y + h * 0.35f)
            cubicTo(x + half * 0.55f, y + h * 0.55f, x + half * 0.2f, y + h * 0.5f, x, y + h * 0.1f)
            close()
        }
        canvas.drawPath(left, paint)
        canvas.drawPath(right, paint)
    }

    private fun drawBlush(canvas: Canvas, x: Float, y: Float, w: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FF6B9D }
        val r = w * 0.13f
        canvas.drawCircle(x - w * 0.30f, y, r, paint)
        canvas.drawCircle(x + w * 0.30f, y, r, paint)
    }

    private fun drawCrown(canvas: Canvas, x: Float, y: Float, w: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFC107.toInt() }
        val half = w * 0.5f
        val h = w * 0.42f
        val path = Path().apply {
            moveTo(x - half, y + h / 2f)
            lineTo(x - half, y - h / 2f)
            lineTo(x - half * 0.5f, y)
            lineTo(x, y - h * 0.7f)
            lineTo(x + half * 0.5f, y)
            lineTo(x + half, y - h / 2f)
            lineTo(x + half, y + h / 2f)
            close()
        }
        canvas.drawPath(path, paint)
        val jewel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF2D78.toInt() }
        canvas.drawCircle(x, y - h * 0.32f, w * 0.055f, jewel)
    }

    /** Bakes the lens into a captured bitmap using normalised face coordinates. */
    fun bake(src: Bitmap, lens: Lens, face: FaceState): Bitmap {
        if (lens.art.isEmpty()) return src
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        draw(
            canvas = canvas,
            lens = lens,
            cx = face.cx * out.width,
            cy = face.cy * out.height,
            faceW = face.w * out.width,
            faceH = face.h * out.height,
            roll = face.rollDegrees
        )
        return out
    }
}
