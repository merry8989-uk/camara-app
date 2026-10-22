package com.camara.app.core

/**
 * Walks a pipeline in which every stage returns a new value and the previous one is
 * finished with.
 *
 * The rule it enforces:
 *
 *  - each intermediate is released as soon as the next value arrives;
 *  - a stage that returns its input **unchanged** releases nothing and changes nothing;
 *  - the value the caller supplied is **never** released — it still belongs to them;
 *  - the final value is never released either; the caller receives it.
 *
 * This matters twice over. Getting it wrong one way leaks a full-size bitmap per stage,
 * which is how a large photo export OOMs a 2 GB device. Getting it wrong the other way
 * releases a bitmap that is still being drawn, which is a crash. Since the rule is subtle
 * and used in more than one pipeline, it lives here and is unit tested.
 */
class ReleasingChain<T>(
    original: T,
    private val release: (T) -> Unit,
) {
    private var current: T = original
    private var owned = false

    /** The value as it stands now. */
    val value: T get() = current

    /** Accepts the output of the next stage. */
    fun advance(next: T) {
        if (next === current) return
        if (owned) release(current)
        current = next
        owned = true
    }
}
