package com.camara.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.camara.app.camera.CameraScreen
import com.camara.app.content.ContentStore
import com.camara.app.editor.image.ImageEditorScreen
import com.camara.app.editor.video.VideoEditorScreen
import com.camara.app.gallery.GalleryScreen
import com.camara.app.teleprompter.TeleprompterScreen

/** Every navigation route in the app, in one place. */
object Routes {
    const val CAMERA = "camera"
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
    LaunchedEffect(Unit) { ContentStore.loadCache(context) }

    NavHost(
        navController = navController,
        startDestination = Routes.CAMERA
    ) {
        composable(Routes.CAMERA) {
            CameraScreen(
                onOpenGallery = { navController.navigate(Routes.GALLERY) },
                onEditPhoto = { uri -> navController.navigate(Routes.imageEditor(uri)) },
                onEditVideo = { uri -> navController.navigate(Routes.videoEditor(uri)) },
                onOpenTeleprompter = { navController.navigate(Routes.TELEPROMPTER) }
            )
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
}
