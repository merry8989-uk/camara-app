package com.camara.app.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.camara.app.R
import kotlin.math.min

/**
 * Real filter thumbnails for the filter browser.
 *
 * The chips used to show a colour gradient derived from the filter's matrix, which tells
 * you almost nothing about what the filter actually does to a photograph. Each chip now
 * shows a real photo with that filter applied, so the look is visible at a glance without
 * having to try each one.
 *
 * Rendering is cheap by design: the source is reduced to one [SIZE]² square, and each
 * filter is then a single small draw with a colour-matrix paint. Results are cached per
 * filter id and the cache is dropped when the source photo changes.
 */
object FilterThumbnails {

    private const val SIZE = 144

    private val lock = Any()
    private val cache = HashMap<String, Bitmap>()
    private var base: Bitmap? = null
    private var baseSourceId = 0
    private var sampleBitmap: Bitmap? = null

    /**
     * The bundled fallback photo, used until a real one is supplied. It is a portrait on
     * purpose: skin tones and a busy background are what make a filter's character
     * readable.
     */
    fun sample(context: Context): Bitmap? {
        sampleBitmap?.let { return it }
        val loaded = runCatching {
            ContextCompat.getDrawable(context, R.drawable.filter_sample)?.toBitmap(SIZE, SIZE)
        }.getOrNull() ?: return null
        sampleBitmap = loaded
        return loaded
    }

    /**
     * The chip image for [filter], rendered from [source] when one is given.
     *
     * Pass the photo being edited, or anything better than a flat gradient. A null
     * [source] falls back to the bundled sample, which is what the camera screen uses —
     * the live preview is often a plain wall, and every filter would look identical on it.
     */
    fun thumbnail(context: Context, filter: Filter, source: Bitmap?): Bitmap? {
        val src = source ?: sample(context) ?: return null
        ensureBase(src)
        val b = synchronized(lock) { base } ?: return null
        synchronized(lock) {
            cache[filter.id] ?: render(b, filter).also { cache[filter.id] = it }
        }
        return synchronized(lock) { cache[filter.id] }
    }

    /** Frees everything. Safe to call when the browser is gone. */
    fun release() {
        synchronized(lock) {
            // The cached thumbnails may still be on screen, so they are dropped for the
            // collector rather than recycled out from under the UI. `base` is ours alone
            // and is never drawn, so it can be recycled.
            cache.clear()
            base?.recycle()
            base = null
            baseSourceId = 0
        }
    }

    private fun ensureBase(src: Bitmap) {
        val id = System.identityHashCode(src)
        synchronized(lock) {
            if (id == baseSourceId && base != null) return
            cache.clear()
            base?.recycle()
            base = squareCrop(src)
            baseSourceId = id
        }
    }

    /** Centre-crops [source] to a square and scales it to [SIZE]. Always a new bitmap. */
    private fun squareCrop(source: Bitmap): Bitmap {
        val side = min(source.width, source.height).coerceAtLeast(1)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2

        val cropped = Bitmap.createBitmap(source, left, top, side, side)
        // createBitmap hands back the source itself when the request is the whole image.
        val own = if (cropped === source) cropped.copy(Bitmap.Config.ARGB_8888, false) else cropped

        if (own.width == SIZE && own.height == SIZE) return own
        val scaled = Bitmap.createScaledBitmap(own, SIZE, SIZE, true)
        if (scaled !== own) own.recycle()
        return scaled
    }

    private fun render(src: Bitmap, filter: Filter): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        if (!filter.isIdentity) {
            paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(filter.matrix))
        }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }
}
