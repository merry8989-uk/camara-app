package com.camara.app.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.camara.app.beauty.BeautyRenderer
import com.camara.app.filters.FilterRenderer
import com.camara.app.lenses.LensOverlay
import com.camara.app.lenses.LensPicker
import com.camara.app.lenses.LensRenderer
import com.camara.app.lenses.NoFaceHint
import com.camara.app.stickers.PlacedSticker
import com.camara.app.stickers.StickerLayer
import com.camara.app.stickers.StickerRenderer
import com.camara.app.ui.components.BeautyPanel
import com.camara.app.ui.components.CircleIconButton
import com.camara.app.ui.components.FilterStrip
import com.camara.app.ui.components.GridOverlay
import com.camara.app.ui.components.PillButton
import com.camara.app.ui.components.StickerPanel
import com.camara.app.ui.theme.Accent
import com.camara.app.util.BitmapUtils
import com.camara.app.util.MediaStoreUtils
import com.camara.app.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class CameraPanel { NONE, FILTERS, STICKERS, BEAUTY, LENSES }

@Composable
fun CameraScreen(
    onOpenGallery: () -> Unit,
    onEditPhoto: (Uri) -> Unit,
    onEditVideo: (Uri) -> Unit,
    onOpenTeleprompter: () -> Unit
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Permissions.CAMERA_AND_MIC)
    }

    if (!granted) {
        PermissionRationale(onGrant = { launcher.launch(Permissions.CAMERA_AND_MIC) })
        return
    }

    CameraContent(
        onOpenGallery = onOpenGallery,
        onEditPhoto = onEditPhoto,
        onEditVideo = onEditVideo,
        onOpenTeleprompter = onOpenTeleprompter
    )
}

@Composable
private fun CameraContent(
    onOpenGallery: () -> Unit,
    onEditPhoto: (Uri) -> Unit,
    onEditVideo: (Uri) -> Unit,
    onOpenTeleprompter: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val controller = remember { CameraController(context) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    var panel by remember { mutableStateOf(CameraPanel.NONE) }
    var stickers by remember { mutableStateOf(listOf<PlacedSticker>()) }
    var countdown by remember { mutableIntStateOf(0) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }

    DisposableEffect(Unit) {
        controller.attach(lifecycleOwner, previewView)
        onDispose { controller.close() }
    }

    fun processAndOpenPhoto(uri: Uri, capturedStickers: List<PlacedSticker>) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val bmp = BitmapUtils.decodeUri(context, uri) ?: return@withContext
                var out = bmp
                if (!controller.beauty.isDefault) out = BeautyRenderer.apply(out, controller.beauty)
                out = FilterRenderer.apply(out, controller.filter)
                out = StickerRenderer.bake(out, capturedStickers)
                val lensNow = controller.lens
                val faceNow = controller.faceState
                if (lensNow != null && lensNow.art.isNotEmpty() && faceNow != null) {
                    out = LensRenderer.bake(out, lensNow, faceNow)
                }
                MediaStoreUtils.writeImage(context, uri, BitmapUtils.toJpegBytes(out))
            }
            withContext(Dispatchers.Main) { onEditPhoto(uri) }
        }
    }

    fun capture() {
        if (controller.mode == CaptureMode.PHOTO) {
            val captured = stickers
            if (controller.timerSeconds > 0) {
                scope.launch {
                    for (i in controller.timerSeconds downTo 1) {
                        countdown = i
                        delay(1000)
                    }
                    countdown = 0
                    controller.takePhoto { uri -> processAndOpenPhoto(uri, captured) }
                }
            } else {
                controller.takePhoto { uri -> processAndOpenPhoto(uri, captured) }
            }
        } else {
            if (controller.isRecording) {
                controller.stopRecording()
            } else {
                controller.startRecording { uri -> onEditVideo(uri) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- Live preview ---
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        controller.setZoomRatio(controller.zoom * zoomChange)
                    }
                }
        )

        // --- AR lens overlay (under stickers) ---
        LensOverlay(
            lens = controller.lens,
            face = controller.faceState,
            mirrored = controller.lensFacing == CameraSelector.LENS_FACING_FRONT,
            modifier = Modifier.fillMaxSize()
        )

        // --- Sticker overlay ---
        StickerLayer(
            stickers = stickers,
            onUpdate = { updated ->
                stickers = stickers.map { if (it.id == updated.id) updated else it }
            },
            onDelete = { id -> stickers = stickers.filterNot { it.id == id } },
            modifier = Modifier.fillMaxSize()
        )

        // --- Grid ---
        if (controller.showGrid) {
            GridOverlay(Modifier.fillMaxSize())
        }

        // --- Countdown ---
        if (countdown > 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = countdown.toString(),
                    color = Color.White,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- Top bar ---
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(
                icon = when (controller.flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                    else -> Icons.Filled.FlashOff
                },
                contentDescription = "Flash",
                onClick = { controller.cycleFlash() }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircleIconButton(
                    icon = Icons.Filled.Timer,
                    contentDescription = "Timer",
                    selected = controller.timerSeconds > 0,
                    onClick = { controller.cycleTimer() }
                )
                CircleIconButton(
                    icon = if (controller.showGrid) Icons.Filled.GridOn else Icons.Filled.GridOff,
                    contentDescription = "Grid",
                    onClick = { controller.toggleGrid() }
                )
                CircleIconButton(
                    icon = Icons.Filled.Subtitles,
                    contentDescription = "Teleprompter",
                    onClick = onOpenTeleprompter
                )
            }
        }

        // --- Right-edge vertical tool rail ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircleIconButton(
                icon = Icons.Filled.AutoAwesome,
                contentDescription = "Filters",
                selected = panel == CameraPanel.FILTERS,
                onClick = { panel = if (panel == CameraPanel.FILTERS) CameraPanel.NONE else CameraPanel.FILTERS }
            )
            CircleIconButton(
                icon = Icons.Filled.EmojiEmotions,
                contentDescription = "Stickers",
                selected = panel == CameraPanel.STICKERS,
                onClick = { panel = if (panel == CameraPanel.STICKERS) CameraPanel.NONE else CameraPanel.STICKERS }
            )
            CircleIconButton(
                icon = Icons.Filled.FaceRetouchingNatural,
                contentDescription = "Beauty",
                selected = panel == CameraPanel.BEAUTY,
                onClick = { panel = if (panel == CameraPanel.BEAUTY) CameraPanel.NONE else CameraPanel.BEAUTY }
            )
            CircleIconButton(
                icon = Icons.Filled.Face,
                contentDescription = "Lenses",
                selected = panel == CameraPanel.LENSES,
                onClick = { panel = if (panel == CameraPanel.LENSES) CameraPanel.NONE else CameraPanel.LENSES }
            )
        }

        // --- Bottom stack: mode strip, extension strip, filter strip, controls ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            when (panel) {
                CameraPanel.FILTERS -> FilterStrip(
                    selected = controller.filter,
                    onSelect = { controller.setFilter(it) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CameraPanel.STICKERS -> StickerPanel(
                    onPick = { glyph ->
                        stickers = stickers + PlacedSticker(
                            id = System.currentTimeMillis(),
                            emoji = glyph,
                            x = 0.5f,
                            y = 0.4f
                        )
                    }
                )
                CameraPanel.BEAUTY -> BeautyPanel(
                    settings = controller.beauty,
                    onChange = { controller.setBeauty(it) }
                )
                CameraPanel.LENSES -> Column {
                    LensPicker(
                        selected = controller.lens,
                        onSelect = { controller.setLens(it) },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    if (!controller.lensesSupported) {
                        Text(
                            "Face lenses aren't supported alongside all camera modes on this device.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    } else if (controller.lensesEnabled && controller.faceState == null) {
                        NoFaceHint(
                            Modifier.padding(start = 16.dp, top = 2.dp, bottom = 6.dp)
                        )
                    }
                }
                CameraPanel.NONE -> Unit
            }

            // Mode strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x66000000))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CaptureMode.entries.forEach { m ->
                        PillButton(
                            text = m.label,
                            selected = controller.mode == m,
                            onClick = { controller.setMode(m) }
                        )
                    }
                }
            }

            // Extension strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x44000000))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CamaraExtension.entries.forEach { ext ->
                        PillButton(
                            text = ext.label,
                            selected = controller.extension == ext,
                            onClick = { controller.setExtension(ext) }
                        )
                    }
                }
            }

            // Zoom indicator
            Text(
                text = "${"%.1f".format(controller.zoom)}x",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 4.dp)
            )

            // Main controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery thumbnail
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onOpenGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                }

                ShutterButton(
                    isVideo = controller.mode == CaptureMode.VIDEO,
                    isRecording = controller.isRecording,
                    onClick = { capture() }
                )

                CircleIconButton(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch camera",
                    onClick = { controller.switchCamera() },
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(
    isVideo: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 30.dp else 60.dp)
                .clip(if (isVideo) RoundedCornerShape(if (isRecording) 8.dp else 30.dp) else CircleShape)
                .background(if (isRecording) Color(0xFFE53935) else Color.White)
        )
    }
}

@Composable
private fun PermissionRationale(onGrant: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Camera access needed",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Camara needs the camera and microphone to capture photos and videos.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onGrant) {
                Text("Grant permission")
            }
        }
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
