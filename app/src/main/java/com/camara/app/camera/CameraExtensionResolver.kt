package com.camara.app.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider

/**
 * Resolves a [CameraSelector] that has a vendor extension enabled.
 *
 * Returns the base selector if the extension is AUTO, unsupported, or anything
 * goes wrong — extensions are a nice-to-have, never a hard dependency.
 */
object CameraExtensionResolver {

    fun resolve(
        context: Context,
        provider: ProcessCameraProvider,
        base: CameraSelector,
        extension: CamaraExtension
    ): CameraSelector {
        if (extension == CamaraExtension.AUTO) return base
        return try {
            val manager = ExtensionsManager.getInstanceAsync(context, provider).get()
            val available = manager.isExtensionAvailable(base, extension.extensionMode)
            if (!available) return base
            manager.getExtensionEnabledCameraSelector(base, extension.extensionMode) ?: base
        } catch (t: Throwable) {
            base
        }
    }
}
