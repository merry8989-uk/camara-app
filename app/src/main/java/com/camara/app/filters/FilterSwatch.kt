package com.camara.app.filters

import androidx.compose.ui.graphics.Color

/** Derives a representative swatch colour for a filter by grading a few sample tones. */
object FilterSwatch {

    private val SAMPLES = listOf(
        floatArrayOf(205f, 160f, 120f), // skin
        floatArrayOf(120f, 145f, 185f), // sky
        floatArrayOf(150f, 180f, 140f), // foliage
        floatArrayOf(90f, 90f, 95f)     // shadow
    )

    fun color(filter: Filter): Color {
        val m = filter.matrix
        var r = 0f; var g = 0f; var b = 0f
        SAMPLES.forEach { s ->
            r += m[0] * s[0] + m[1] * s[1] + m[2] * s[2] + m[4]
            g += m[5] * s[0] + m[6] * s[1] + m[7] * s[2] + m[9]
            b += m[10] * s[0] + m[11] * s[1] + m[12] * s[2] + m[14]
        }
        val n = SAMPLES.size
        return Color(
            red = (r / n / 255f).coerceIn(0f, 1f),
            green = (g / n / 255f).coerceIn(0f, 1f),
            blue = (b / n / 255f).coerceIn(0f, 1f)
        )
    }
}
