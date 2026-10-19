package com.camara.app.camera

import androidx.camera.extensions.ExtensionMode

/**
 * CameraX vendor extensions, exposed as friendly modes. Availability depends on
 * the device/OEM; [CameraController] falls back to the plain selector when an
 * extension is not supported.
 */
enum class CamaraExtension(val label: String, val extensionMode: Int) {
    AUTO("Auto", ExtensionMode.NONE),
    PORTRAIT("Portrait", ExtensionMode.BOKEH),
    HDR("HDR", ExtensionMode.HDR),
    NIGHT("Night", ExtensionMode.NIGHT),
    RETOUCH("Retouch", ExtensionMode.FACE_RETOUCH)
}
