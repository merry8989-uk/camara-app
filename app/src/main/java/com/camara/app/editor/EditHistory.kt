package com.camara.app.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Undo/redo for the editors, shared by the photo and video editors.
 *
 * Rapid changes are **coalesced**: a new entry is only pushed when more than
 * [coalesceMs] has passed since the last one, so dragging a slider undoes in a single
 * step instead of filling the stack with one entry per frame.
 *
 * `canUndo`/`canRedo` are Compose state, so toolbar buttons enable and disable themselves.
 *
 * The clock is injectable so the coalescing window can be tested without waiting.
 */
class EditHistory<T : Any>(
    private val limit: Int = 60,
    private val coalesceMs: Long = 700L,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val past = ArrayDeque<T>()
    private val future = ArrayDeque<T>()
    private var lastRecordedAt = Long.MIN_VALUE

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    /** Records the state as it was *before* the change that just happened. */
    fun record(previous: T) {
        val now = clock()
        // Long.MIN_VALUE would overflow if subtracted, so treat the first record as fresh.
        val withinWindow = lastRecordedAt != Long.MIN_VALUE && now - lastRecordedAt <= coalesceMs
        if (!withinWindow) {
            past.addLast(previous)
            while (past.size > limit) past.removeFirst()
        }
        future.clear()
        lastRecordedAt = now
        sync()
    }

    /** Steps back, returning the state to restore, or null when there is nothing to undo. */
    fun undo(current: T): T? {
        val previous = past.removeLastOrNull() ?: return null
        future.addLast(current)
        lastRecordedAt = Long.MIN_VALUE
        sync()
        return previous
    }

    /** Steps forward again after an undo. */
    fun redo(current: T): T? {
        val next = future.removeLastOrNull() ?: return null
        past.addLast(current)
        lastRecordedAt = Long.MIN_VALUE
        sync()
        return next
    }

    /** Forgets everything — used when the edited item itself changes. */
    fun clear() {
        past.clear()
        future.clear()
        lastRecordedAt = Long.MIN_VALUE
        sync()
    }

    private fun sync() {
        canUndo = past.isNotEmpty()
        canRedo = future.isNotEmpty()
    }
}
