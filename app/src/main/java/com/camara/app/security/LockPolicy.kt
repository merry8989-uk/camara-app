package com.camara.app.security

/**
 * When the app should ask for authentication again.
 *
 * The grace period is the important part. The biometric prompt itself, the camera
 * permission dialog and the system photo picker all push the activity to the background,
 * so without a grace period the app would re-lock every time it came back and fight the
 * user — including fighting itself, since unlocking is what puts it in the background.
 *
 * Kept as a pure function so the timing rule can be tested without a device.
 */
object LockPolicy {

    /** How long the app can be away before it locks again. */
    const val DEFAULT_GRACE_MS = 20_000L

    /**
     * @param backgroundedAtMs when the app last left the foreground, or null if it has not
     *   been backgrounded in this process — a cold start, which should lock.
     */
    fun shouldRelock(
        nowMs: Long,
        backgroundedAtMs: Long?,
        graceMs: Long = DEFAULT_GRACE_MS,
    ): Boolean {
        if (backgroundedAtMs == null) return true
        if (graceMs <= 0L) return true
        return nowMs - backgroundedAtMs > graceMs
    }
}
