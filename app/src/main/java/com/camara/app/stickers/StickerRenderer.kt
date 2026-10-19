package com.camara.app.stickers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/** Bakes placed stickers into a captured bitmap at the right size and rotation. */
object StickerRenderer {

    fun bake(src: Bitmap, stickers: List<PlacedSticker>): Bitmap {
        if (stickers.isEmpty()) return src
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
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
            // Vertically centre the glyph on its anchor point.
            val metrics = paint.fontMetrics
            val offset = -(metrics.ascent + metrics.descent) / 2f
            canvas.drawText(s.emoji, cx, cy + offset, paint)
            canvas.restore()
        }
        return out
    }
}
