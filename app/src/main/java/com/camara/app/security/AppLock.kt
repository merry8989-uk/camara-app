package com.camara.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * The app lock.
 *
 * It accepts a biometric **or the device credential**. That is deliberate: the device PIN
 * or pattern always works, so switching the lock on can never shut someone out of their
 * own app. A lock that can strand you is worse than no lock.
 *
 * If the device has nothing set up at all, [isAvailable] is false and the caller must not
 * gate anything — see `CamaraRoot`, which checks it before ever showing the lock screen.
 */
object AppLock {

    private val ALLOWED = BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /** True when this device can actually gate the app (biometric enrolled, or a PIN set). */
    fun isAvailable(context: Context): Boolean = try {
        BiometricManager.from(context).canAuthenticate(ALLOWED) == BiometricManager.BIOMETRIC_SUCCESS
    } catch (t: Throwable) {
        false
    }

    /**
     * Shows the system prompt.
     *
     * @param onFailed called when the prompt is dismissed or errors; the caller should keep
     *   the gate up and let the user try again.
     */
    fun prompt(
        activity: FragmentActivity,
        onUnlocked: () -> Unit,
        onFailed: (String) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailed(errString.toString())
                }
            }
        )

        // No negative button: with DEVICE_CREDENTIAL among the allowed authenticators the
        // library rejects a prompt that also sets one.
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Camara")
            .setSubtitle("Your photos and recordings are private")
            .setAllowedAuthenticators(ALLOWED)
            .build()

        prompt.authenticate(info)
    }
}
