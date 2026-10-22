package com.camara.app.core

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Reports whether the on-device ML models can actually run.
 *
 * Face lenses, face-only beauty and portrait depth all use ML Kit's **unbundled**
 * models: they live in Google Play Services and are fetched on first use (that's what
 * keeps the APK ~5 MB instead of ~80 MB). If Play Services is missing, disabled or too
 * old, those models can never arrive — and the features would otherwise fail silently,
 * looking like a bug rather than a missing dependency.
 *
 * A behavioural check isn't reliable here: ML Kit simply returns no results until the
 * model lands, which is indistinguishable from "no face in frame". Checking Play
 * Services availability is the honest signal.
 */
object ModelSupport {

    enum class Status { READY, PLAY_SERVICES_MISSING, PLAY_SERVICES_OUTDATED, UNKNOWN }

    fun status(context: Context): Status = try {
        when (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)) {
            ConnectionResult.SUCCESS -> Status.READY
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> Status.PLAY_SERVICES_OUTDATED
            ConnectionResult.SERVICE_MISSING,
            ConnectionResult.SERVICE_DISABLED,
            ConnectionResult.SERVICE_INVALID -> Status.PLAY_SERVICES_MISSING
            else -> Status.UNKNOWN
        }
    } catch (t: Throwable) {
        Status.UNKNOWN
    }

    /** A user-facing explanation, or null when everything is fine. */
    fun message(status: Status): String? = when (status) {
        Status.READY -> null
        Status.PLAY_SERVICES_MISSING ->
            "Face lenses, face beauty and portrait depth need Google Play Services, which " +
                "isn't available here. Filters, stickers, editing and everything else still work."
        Status.PLAY_SERVICES_OUTDATED ->
            "Update Google Play Services to use face lenses, face beauty and portrait depth."
        Status.UNKNOWN ->
            "Face lenses and portrait depth may be unavailable — Google Play Services " +
                "couldn't be checked."
    }
}
