package com.camara.app.lenses

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import kotlin.math.max

/**
 * Draws a [Lens] anchored to a detected face.
 *
 * Everything is positioned from landmarks and sized in units of the inter-ocular
 * distance, then rotated by the eye-line angle. That is what makes the pieces track
 * the face properly instead of drifting with the bounding box. Contour-following
 * pieces (lipstick, face paint, lashes) trace the actual detected contour.
 *
 * The same code serves the live preview and baking the lens into a captured photo —
 * only the canvas dimensions differ.
 */
object LensRenderer {

    fun draw(
        canvas: Canvas,
        lens: Lens,
        face: FaceState,
        viewW: Float,
        viewH: Float,
        mirrored: Boolean
    ) {
        if (lens.art.isEmpty() || viewW <= 0f || viewH <= 0f) return

        val aspect = if (face.imageAspect > 0f) face.imageAspect else 0.75f
        val displayedW = max(viewW, viewH * aspect)
        val displayedH = displayedW / aspect
        val offX = (viewW - displayedW) / 2f
        val offY = (viewH - displayedH) / 2f

        fun mx(nx: Float) = offX + (if (mirrored) 1f - nx else nx) * displayedW
        fun my(ny: Float) = offY + ny * displayedH

        val roll = if (mirrored) -face.eyeAngle else face.eyeAngle
        val iod = face.interOcular * displayedW

        fun anchorPoint(a: Anchor): Offset = when (a) {
            Anchor.EYE_MID -> face.eyeMid?.let { Offset(mx(it.x), my(it.y)) }
                ?: Offset(mx(face.cx), my(face.cy - face.h * 0.1f))
            Anchor.EYE_LEFT -> face.leftEye?.let { Offset(mx(it.x), my(it.y)) } ?: Offset(mx(face.cx), my(face.cy))
            Anchor.EYE_RIGHT -> face.rightEye?.let { Offset(mx(it.x), my(it.y)) } ?: Offset(mx(face.cx), my(face.cy))
            Anchor.NOSE -> face.nose?.let { Offset(mx(it.x), my(it.y)) } ?: Offset(mx(face.cx), my(face.cy + face.h * 0.08f))
            Anchor.MOUTH -> face.mouth?.let { Offset(mx(it.x), my(it.y)) } ?: Offset(mx(face.cx), my(face.cy + face.h * 0.22f))
            Anchor.CHIN -> Offset(mx(face.cx), my(face.cy + face.h * 0.42f))
            Anchor.FOREHEAD -> Offset(mx(face.cx), my(face.cy - face.h * 0.5f))
            Anchor.FACE_CENTRE -> Offset(mx(face.cx), my(face.cy))
            Anchor.EAR_LEFT -> face.leftEar?.let { Offset(mx(it.x), my(it.y)) } ?: Offset(mx(face.cx - face.w * 0.5f), my(face.cy))
            Anchor.EAR_RIGHT -> face.rightEar?.let { Offset(mx(it.x), my(it.y)) } ?: Offset(mx(face.cx + face.w * 0.5f), my(face.cy))
            Anchor.CHEEK_LEFT -> face.leftCheek?.let { Offset(mx(it.x), my(it.y)) } ?: Offset(mx(face.cx - face.w * 0.28f), my(face.cy + face.h * 0.12f))
            Anchor.CHEEK_RIGHT -> face.rightCheek?.let { Offset(mx(it.x), my(it.y)) } ?: Offset(mx(face.cx + face.w * 0.28f), my(face.cy + face.h * 0.12f))
        }

        // Contour helpers in canvas space.
        fun contourPts(pts: List<Offset>): List<Offset> = pts.map { Offset(mx(it.x), my(it.y)) }

        lens.art.forEach { art ->
            when (art) {
                is EmojiArt -> {
                    val base = anchorPoint(art.anchor)
                    val x = base.x + art.dx * iod
                    val y = base.y + art.dy * iod
                    drawEmoji(canvas, art.emoji, x, y, art.size * iod, roll)
                }

                is DrawnArt -> {
                    val base = anchorPoint(art.anchor)
                    val x = base.x + art.dx * iod
                    val y = base.y + art.dy * iod
                    val w = art.size * iod
                    when (art.kind) {
                        DrawKind.LIPS -> drawLips(canvas, contourPts(face.upperLipTop), contourPts(face.lowerLipBottom), w)
                        DrawKind.FACE_PAINT -> drawFacePaint(canvas, contourPts(face.outline()))
                        DrawKind.LASHES -> drawLashes(
                            canvas,
                            contourPts(face.leftEyeContour),
                            contourPts(face.rightEyeContour),
                            w
                        )
                        else -> drawShape(canvas, art.kind, x, y, w, roll, face, ::mx, ::my, mirrored)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- emoji

    private fun drawEmoji(canvas: Canvas, emoji: String, x: Float, y: Float, size: Float, roll: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = size.coerceAtLeast(6f)
            color = Color.WHITE
        }
        canvas.save()
        canvas.rotate(roll, x, y)
        val fm = paint.fontMetrics
        canvas.drawText(emoji, x, y - (fm.ascent + fm.descent) / 2f, paint)
        canvas.restore()
    }

    // ------------------------------------------------------- contour pieces

    private fun drawLips(canvas: Canvas, upper: List<Offset>, lower: List<Offset>, width: Float) {
        if (upper.size < 3 || lower.size < 3) return
        val path = Path()
        val pts = upper + lower.reversed()
        path.moveTo(pts.first().x, pts.first().y)
        pts.drop(1).forEach { path.lineTo(it.x, it.y) }
        path.close()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD6336C.toInt()
            style = Paint.Style.FILL
            alpha = 205
        }
        canvas.drawPath(path, paint)
        // A touch of gloss.
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x55FFFFFF
                style = Paint.Style.STROKE
                strokeWidth = (width * 0.05f).coerceAtLeast(1f)
            }
        )
    }

    private fun drawFacePaint(canvas: Canvas, outline: List<Offset>) {
        if (outline.size < 3) return
        val path = Path()
        path.moveTo(outline.first().x, outline.first().y)
        outline.drop(1).forEach { path.lineTo(it.x, it.y) }
        path.close()
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF3D7BFF.toInt()
                style = Paint.Style.FILL
                alpha = 90
            }
        )
    }

    private fun drawLashes(canvas: Canvas, left: List<Offset>, right: List<Offset>, width: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1A1A1E.toInt()
            style = Paint.Style.STROKE
            strokeWidth = (width * 0.03f).coerceAtLeast(1.5f)
            strokeCap = Paint.Cap.ROUND
        }
        listOf(left, right).forEach { contour ->
            if (contour.size < 4) return@forEach
            // The top of the eye contour is the lash line.
            val top = contour.take(contour.size / 2)
            val path = Path()
            path.moveTo(top.first().x, top.first().y)
            top.drop(1).forEach { path.lineTo(it.x, it.y) }
            canvas.drawPath(path, paint)
        }
    }

    // --------------------------------------------------------- drawn shapes

    private fun drawShape(
        canvas: Canvas,
        kind: DrawKind,
        x: Float,
        y: Float,
        w: Float,
        roll: Float,
        face: FaceState,
        mx: (Float) -> Float,
        my: (Float) -> Float,
        mirrored: Boolean
    ) {
        canvas.save()
        canvas.rotate(roll, x, y)
        when (kind) {
            DrawKind.GLASSES -> drawGlasses(canvas, x, y, w, round = false, heart = false)
            DrawKind.ROUND_GLASSES -> drawGlasses(canvas, x, y, w, round = true, heart = false)
            DrawKind.HEART_GLASSES -> drawGlasses(canvas, x, y, w, round = false, heart = true)
            DrawKind.CAT_EARS -> drawCatEars(canvas, x, y, w)
            DrawKind.MUSTACHE -> drawMustache(canvas, x, y, w)
            DrawKind.BLUSH -> drawBlush(canvas, x, y, w)
            DrawKind.CROWN -> drawCrown(canvas, x, y, w)
            DrawKind.HALO -> drawHalo(canvas, x, y, w)
            DrawKind.PARTY_HAT -> drawPartyHat(canvas, x, y, w)
            DrawKind.ANIMAL_NOSE -> drawAnimalNose(canvas, x, y, w)
            DrawKind.WHISKERS -> drawWhiskers(canvas, x, y, w, face, mx, my, mirrored)
            else -> Unit
        }
        canvas.restore()
    }

    private fun drawGlasses(canvas: Canvas, x: Float, y: Float, w: Float, round: Boolean, heart: Boolean) {
        val lensW = w * 0.40f
        val lensH = if (round) w * 0.38f else w * 0.28f
        val gap = w * 0.07f
        // Real glass is not one flat colour: it darkens at the top where it meets the
        // brow, catches a little ambient blue in the middle and lifts at the bottom. A
        // vertical gradient with a slight blue cast reads as a lens; a flat fill reads as
        // a black shape pasted on the face.
        val dark = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            shader = LinearGradient(
                0f, y - lensH / 2f, 0f, y + lensH / 2f,
                intArrayOf(0xF20B0E14.toInt(), 0xE6121720.toInt(), 0xE6263040.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        val r = if (round) lensH / 2f else lensH * 0.28f

        val left = RectF(x - gap / 2f - lensW, y - lensH / 2f, x - gap / 2f, y + lensH / 2f)
        val right = RectF(x + gap / 2f, y - lensH / 2f, x + gap / 2f + lensW, y + lensH / 2f)

        if (heart) {
            listOf(left, right).forEach { rect ->
                val cx = rect.centerX()
                val cy = rect.centerY()
                val s = rect.width() / 2f
                val path = Path().apply {
                    moveTo(cx, cy + s * 0.75f)
                    cubicTo(cx - s * 1.3f, cy - s * 0.3f, cx - s * 0.5f, cy - s * 1.0f, cx, cy - s * 0.35f)
                    cubicTo(cx + s * 0.5f, cy - s * 1.0f, cx + s * 1.3f, cy - s * 0.3f, cx, cy + s * 0.75f)
                    close()
                }
                canvas.drawPath(path, dark)
            }
        } else if (round) {
            canvas.drawOval(left, dark)
            canvas.drawOval(right, dark)
        } else {
            canvas.drawRoundRect(left, r, r, dark)
            canvas.drawRoundRect(right, r, r, dark)
        }

        val bridge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.035f).coerceAtLeast(1.5f)
            color = 0xE6101014.toInt()
        }
        canvas.drawLine(x - gap / 2f, y, x + gap / 2f, y, bridge)

        // Two highlights rather than one: a broad soft sheen plus a tighter bright streak.
        // The pair is what makes a lens look wet and reflective instead of matte.
        val sheen = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            shader = LinearGradient(
                0f, y - lensH / 2f, 0f, y + lensH / 2f,
                intArrayOf(0x40FFFFFF, 0x10FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        val glare = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF }
        listOf(left, right).forEach { rect ->
            canvas.drawRoundRect(rect, r, r, sheen)
            canvas.drawRoundRect(
                RectF(
                    rect.left + rect.width() * 0.16f,
                    rect.top + rect.height() * 0.14f,
                    rect.left + rect.width() * 0.44f,
                    rect.top + rect.height() * 0.40f
                ),
                r, r, glare
            )
        }
    }

    private fun drawCatEars(canvas: Canvas, x: Float, y: Float, w: Float) {
        val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2B2B33.toInt() }
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF9EC4.toInt() }
        val earW = w * 0.36f
        val earH = w * 0.48f
        val dx = w * 0.28f
        listOf(-1f, 1f).forEach { side ->
            val baseX = x + side * dx
            canvas.drawPath(
                Path().apply {
                    moveTo(baseX - earW / 2f, y + earH / 2f)
                    lineTo(baseX, y - earH / 2f)
                    lineTo(baseX + earW / 2f, y + earH / 2f)
                    close()
                }, outer
            )
            canvas.drawPath(
                Path().apply {
                    moveTo(baseX - earW * 0.22f, y + earH * 0.32f)
                    lineTo(baseX, y - earH * 0.20f)
                    lineTo(baseX + earW * 0.22f, y + earH * 0.32f)
                    close()
                }, inner
            )
        }
    }

    private fun drawMustache(canvas: Canvas, x: Float, y: Float, w: Float) {
        val half = w * 0.5f
        val h = w * 0.26f
        // Hair is not one colour either — it catches light along the top and sits darker
        // underneath. Warm brown with a dark core looks like hair; flat grey looks drawn.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            shader = LinearGradient(
                0f, y - h, 0f, y + h * 0.55f,
                intArrayOf(0xFF4A362B.toInt(), 0xFF241812.toInt(), 0xFF3A2A21.toInt()),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(
            Path().apply {
                moveTo(x, y)
                cubicTo(x - half * 0.5f, y - h, x - half, y - h * 0.2f, x - half, y + h * 0.35f)
                cubicTo(x - half * 0.55f, y + h * 0.55f, x - half * 0.2f, y + h * 0.5f, x, y + h * 0.1f)
                close()
            }, paint
        )
        canvas.drawPath(
            Path().apply {
                moveTo(x, y)
                cubicTo(x + half * 0.5f, y - h, x + half, y - h * 0.2f, x + half, y + h * 0.35f)
                cubicTo(x + half * 0.55f, y + h * 0.55f, x + half * 0.2f, y + h * 0.5f, x, y + h * 0.1f)
                close()
            }, paint
        )
    }

    private fun drawBlush(canvas: Canvas, x: Float, y: Float, w: Float) {
        // A flat translucent circle reads as a sticker dot. Blush is a soft pool of colour
        // that fades to nothing at its edge, so it is drawn as a radial gradient with a
        // transparent outer stop — the single biggest thing that makes it look natural.
        val r = w * 0.21f
        listOf(x - w * 0.30f, x + w * 0.30f).forEach { cx ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
                shader = RadialGradient(
                    cx, y, r,
                    intArrayOf(0x5CFF5C8A, 0x2EFF7FA8, 0x00FF9BBB),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, y, r, paint)
        }
    }

    private fun drawCrown(canvas: Canvas, x: Float, y: Float, w: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFC107.toInt() }
        val half = w * 0.5f
        val h = w * 0.42f
        canvas.drawPath(
            Path().apply {
                moveTo(x - half, y + h / 2f)
                lineTo(x - half, y - h / 2f)
                lineTo(x - half * 0.5f, y)
                lineTo(x, y - h * 0.7f)
                lineTo(x + half * 0.5f, y)
                lineTo(x + half, y - h / 2f)
                lineTo(x + half, y + h / 2f)
                close()
            }, paint
        )
        canvas.drawCircle(x, y - h * 0.32f, w * 0.055f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF2D78.toInt() })
    }

    private fun drawHalo(canvas: Canvas, x: Float, y: Float, w: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.10f).coerceAtLeast(2f)
            color = 0xFFFFE082.toInt()
        }
        val rect = RectF(x - w * 0.5f, y - w * 0.16f, x + w * 0.5f, y + w * 0.16f)
        canvas.drawOval(rect, paint)
    }

    private fun drawPartyHat(canvas: Canvas, x: Float, y: Float, w: Float) {
        val h = w * 0.9f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7C3AED.toInt() }
        canvas.drawPath(
            Path().apply {
                moveTo(x - w * 0.35f, y + h * 0.4f)
                lineTo(x + w * 0.35f, y + h * 0.4f)
                lineTo(x, y - h * 0.6f)
                close()
            }, paint
        )
        val stripe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.08f).coerceAtLeast(2f)
            color = 0xFFFFD54F.toInt()
        }
        for (i in 0..2) {
            val t = 0.15f + i * 0.25f
            val yy = y + h * 0.4f - h * (0.4f + 0.6f) * t
            val halfW = w * 0.35f * (1f - t)
            canvas.drawLine(x - halfW, yy, x + halfW, yy, stripe)
        }
        canvas.drawCircle(x, y - h * 0.62f, w * 0.07f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF2D78.toInt() })
    }

    private fun drawAnimalNose(canvas: Canvas, x: Float, y: Float, w: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A3A42.toInt() }
        canvas.drawPath(
            Path().apply {
                moveTo(x - w * 0.22f, y - w * 0.10f)
                quadTo(x, y - w * 0.30f, x + w * 0.22f, y - w * 0.10f)
                quadTo(x + w * 0.14f, y + w * 0.22f, x, y + w * 0.26f)
                quadTo(x - w * 0.14f, y + w * 0.22f, x - w * 0.22f, y - w * 0.10f)
                close()
            }, paint
        )
    }

    private fun drawWhiskers(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        face: FaceState,
        mx: (Float) -> Float,
        my: (Float) -> Float,
        mirrored: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.018f).coerceAtLeast(1.2f)
            color = 0xCC2B2B33.toInt()
            pathEffect = DashPathEffect(floatArrayOf(w * 0.06f, w * 0.03f), 0f)
        }
        // Use the eye line as the whisker direction so they follow head tilt.
        val roll = if (mirrored) -face.eyeAngle else face.eyeAngle
        canvas.save()
        canvas.rotate(roll, x, y)
        for (side in listOf(-1f, 1f)) {
            for (i in -1..1) {
                val sy = y + i * w * 0.10f
                canvas.drawLine(
                    x + side * w * 0.16f, sy,
                    x + side * w * 0.62f, sy + i * w * 0.06f - w * 0.04f,
                    paint
                )
            }
        }
        canvas.restore()
    }

    // ------------------------------------------------------------------ bake

    /** Bakes the lens into a captured bitmap. */
    fun bake(src: android.graphics.Bitmap, lens: Lens, face: FaceState): android.graphics.Bitmap {
        if (lens.art.isEmpty()) return src
        val out = src.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        draw(
            canvas = Canvas(out),
            lens = lens,
            face = face,
            viewW = out.width.toFloat(),
            viewH = out.height.toFloat(),
            mirrored = false
        )
        return out
    }
}
