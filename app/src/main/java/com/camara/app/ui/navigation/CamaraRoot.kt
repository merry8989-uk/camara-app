package com.camara.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.camara.app.camera.CameraScreen
import com.camara.app.content.ContentStore
import com.camara.app.filters.UserFilterStore
import com.camara.app.editor.image.ImageEditorScreen
import com.camara.app.editor.video.VideoEditorScreen
import com.camara.app.diagnostics.DiagnosticsLogger
import com.camara.app.gallery.GalleryScreen
import com.camara.app.security.AppLock
import com.camara.app.security.LockPolicy
import com.camara.app.settings.SettingsRepository
import com.camara.app.settings.SettingsScreen
import com.camara.app.settings.SettingsState
import com.camara.app.teleprompter.TeleprompterScreen

/** Every navigation route in the app, in one place. */
object Routes {
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
    const val GALLERY = "gallery"
    const val TELEPROMPTER = "teleprompter"
    const val IMAGE_EDITOR = "image_editor"
    const val VIDEO_EDITOR = "video_editor"

    const val ARG_URI = "uri"

    fun imageEditor(uri: Uri) = "$IMAGE_EDITOR?$ARG_URI=${Uri.encode(uri.toString())}"
    fun videoEditor(uri: Uri) = "$VIDEO_EDITOR?$ARG_URI=${Uri.encode(uri.toString())}"
}

@Composable
fun CamaraRoot() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Bring in the cached content catalogue at startup so remotely-synced filters,
    // sticker packs and lenses are available without waiting for a network call.
    LaunchedEffect(Unit) {
        ContentStore.loadCache(context)
        // Filters the user saved themselves, so the strip has them from the first frame.
        UserFilterStore.load(context)
    }

    // ------------------------------------------------------------------ App lock
    //
    // The gate only ever engages when the device can actually authenticate. If it cannot,
    // there is nothing to enforce and gating would only strand the user.
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settings.collectAsState(initial = SettingsState())
    val activity = context as? FragmentActivity
    val lockAvailable = remember { AppLock.isAvailable(context) }
    val lockOn = settings.appLockEnabled && lockAvailable && activity != null

    var locked by remember { mutableStateOf(false) }
    var lockError by remember { mutableStateOf<String?>(null) }
    var backgroundedAt by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(lockOn) {
        locked = lockOn
        lockError = null
        if (lockOn) DiagnosticsLogger.log("AppLock", "gate engaged")
    }

    // Re-lock when the app comes back after being away longer than the grace period.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, lockOn) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    if (lockOn && LockPolicy.shouldRelock(System.currentTimeMillis(), backgroundedAt)) {
                        locked = true
                    }
                    backgroundedAt = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun unlock() {
        locked = false
        lockError = null
        DiagnosticsLogger.log("AppLock", "unlocked")
    }

    fun onAuthFailed(message: String) {
        // If the device no longer has anything to authenticate with — the PIN was removed
        // while the lock was on — the lock can no longer be enforced. Letting the user in
        // is the only safe option; refusing would strand them out of their own app.
        if (!AppLock.isAvailable(context)) {
            DiagnosticsLogger.log("AppLock", "device credential gone; gate released")
            unlock()
        } else {
            DiagnosticsLogger.log("AppLock", "authentication failed")
            lockError = message
        }
    }

    // Ask as soon as the gate is up.
    LaunchedEffect(locked, activity) {
        val host = activity
        if (locked && host != null) {
            AppLock.prompt(host, onUnlocked = ::unlock, onFailed = ::onAuthFailed)
        }
    }

    Box(Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = Routes.CAMERA
    ) {
        composable(Routes.CAMERA) {
            CameraScreen(
                onOpenGallery = { navController.navigate(Routes.GALLERY) },
                onEditPhoto = { uri -> navController.navigate(Routes.imageEditor(uri)) },
                onEditVideo = { uri -> navController.navigate(Routes.videoEditor(uri)) },
                onOpenTeleprompter = { navController.navigate(Routes.TELEPROMPTER) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.GALLERY) {
            GalleryScreen(
                onBack = { navController.popBackStack() },
                onEditPhoto = { uri -> navController.navigate(Routes.imageEditor(uri)) },
                onEditVideo = { uri -> navController.navigate(Routes.videoEditor(uri)) }
            )
        }

        composable(Routes.TELEPROMPTER) {
            TeleprompterScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "${Routes.IMAGE_EDITOR}?${Routes.ARG_URI}={${Routes.ARG_URI}}",
            arguments = listOf(navArgument(Routes.ARG_URI) { type = NavType.StringType })
        ) { entry ->
            val uri = entry.arguments?.getString(Routes.ARG_URI)?.let(Uri::parse)
            ImageEditorScreen(
                sourceUri = uri,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.VIDEO_EDITOR}?${Routes.ARG_URI}={${Routes.ARG_URI}}",
            arguments = listOf(navArgument(Routes.ARG_URI) { type = NavType.StringType })
        ) { entry ->
            val uri = entry.arguments?.getString(Routes.ARG_URI)?.let(Uri::parse)
            VideoEditorScreen(
                sourceUri = uri,
                onBack = { navController.popBackStack() }
            )
        }
    }

        if (locked) {
            LockGate(
                message = lockError,
                onUnlock = {
                    val host = activity
                    if (host != null) {
                        AppLock.prompt(host, onUnlocked = ::unlock, onFailed = ::onAuthFailed)
                    }
                }
            )
        }
    }
}

/** The screen shown while the app is locked. */
@Composable
private fun LockGate(message: String?, onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Camara is locked",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your photos and recordings are private.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            if (message != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    message,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onUnlock) { Text("Unlock") }
        }
    }
}
