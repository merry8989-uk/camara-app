package com.camara.app.video

/**
 * One clip on the timeline, with its own trim.
 *
 * [trimEndMs] defaults to [Float.MAX_VALUE] and is resolved against the clip's real
 * duration, so a clip added before its length is known still means "to the end".
 */
data class TimelineClip(
    val id: Long,
    val uri: String,
    val sourceDurationMs: Float,
    val trimStartMs: Float = 0f,
    val trimEndMs: Float = Float.MAX_VALUE,
) {
    fun startMs(): Float = trimStartMs.coerceIn(0f, sourceDurationMs)

    fun endMs(): Float =
        if (trimEndMs == Float.MAX_VALUE) sourceDurationMs
        else trimEndMs.coerceIn(0f, sourceDurationMs)

    /** Length of the trimmed clip. */
    val durationMs: Float get() = (endMs() - startMs()).coerceAtLeast(0f)

    fun trimmed(startMs: Float, endMs: Float): TimelineClip {
        val lo = startMs.coerceIn(0f, sourceDurationMs)
        val hi = endMs.coerceIn(lo, sourceDurationMs)
        return copy(trimStartMs = lo, trimEndMs = hi)
    }
}

/** A clip plus how far into it a timeline position falls. */
data class ClipPosition(val clip: TimelineClip, val localMs: Float)

/**
 * An ordered list of trimmed clips that plays and exports as one video.
 *
 * Kept as a value type with pure helpers, so every change produces a new timeline — which
 * is what the editor's undo stack stores.
 */
data class VideoTimeline(val clips: List<TimelineClip> = emptyList()) {

    val isEmpty: Boolean get() = clips.isEmpty()

    val totalDurationMs: Float
        get() = clips.fold(0f) { sum, clip -> sum + clip.durationMs }

    /** Where [positionMs] on the whole timeline lands, or null past the end. */
    fun locate(positionMs: Float): ClipPosition? {
        if (clips.isEmpty()) return null
        var remaining = positionMs.coerceAtLeast(0f)
        for (clip in clips) {
            val length = clip.durationMs
            if (remaining < length || clip === clips.last()) {
                val local = (clip.startMs() + remaining).coerceIn(clip.startMs(), clip.endMs())
                return ClipPosition(clip, local)
            }
            remaining -= length
        }
        return null
    }

    fun indexOf(id: Long): Int = clips.indexOfFirst { it.id == id }

    fun clip(id: Long): TimelineClip? = clips.firstOrNull { it.id == id }

    fun add(clip: TimelineClip): VideoTimeline = copy(clips = clips + clip)

    fun remove(id: Long): VideoTimeline = copy(clips = clips.filterNot { it.id == id })

    fun replace(clip: TimelineClip): VideoTimeline =
        copy(clips = clips.map { if (it.id == clip.id) clip else it })

    /**
     * Moves a clip one place earlier ([delta] = -1) or later (+1). Reordering is how
     * clips get arranged; a no-op at either end rather than an error.
     */
    fun moved(id: Long, delta: Int): VideoTimeline {
        val from = indexOf(id)
        if (from < 0) return this
        val to = (from + delta).coerceIn(0, clips.size - 1)
        if (to == from) return this
        val reordered = clips.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return copy(clips = reordered)
    }

    /** Start of each clip on the whole timeline — used to lay out the clip strip. */
    fun startOf(id: Long): Float {
        var sum = 0f
        for (clip in clips) {
            if (clip.id == id) return sum
            sum += clip.durationMs
        }
        return 0f
    }
}
