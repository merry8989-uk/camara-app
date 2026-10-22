package com.camara.app.editor.image

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.camara.app.core.PhotoProcessor
import com.camara.app.filters.ColorMatrixMath
import com.camara.app.portrait.PortraitBokeh
import com.camara.app.portrait.PortraitSettings
import com.camara.app.portrait.SubjectSegmenter
import com.camara.app.stickers.PlacedSticker
import com.camara.app.stickers.StickerLayer
import com.camara.app.ui.components.FilterStrip
import com.camara.app.ui.components.StickerPanel
import com.camara.app.ui.theme.Accent
import com.camara.app.util.BitmapUtils
import com.camara.app.util.MediaStoreUtils
import com.camara.app.util.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class EditorTool { NONE, CROP, ROTATE, ADJUST, FILTER, STICKER, TEXT, DRAW, FRAME, PORTRAIT }

@Composable
fun ImageEditorScreen(
    sourceUri: Uri?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var base by remember { mutableStateOf<Bitmap?>(null) }
    var previewSource by remember { mutableStateOf<Bitmap?>(null) }
    var edit by remember { mutableStateOf(ImageEditState()) }

    // Undo/redo. `historyBaseline` always holds the state from *before* the latest change,
    // and undo/redo write it in the same event as `edit` — so the effect below sees them
    // equal and doesn't record the undo itself as a fresh edit.
    val history = remember { EditHistory() }
    var historyBaseline by remember { mutableStateOf(ImageEditState()) }

    LaunchedEffect(edit) {
        if (edit != historyBaseline) {
            history.record(historyBaseline)
            historyBaseline = edit
        }
    }

    fun undoEdit() {
        val target = history.undo(edit) ?: return
        historyBaseline = target
        edit = target
    }

    fun redoEdit() {
        val target = history.redo(edit) ?: return
        historyBaseline = target
        edit = target
    }
    var tool by remember { mutableStateOf(EditorTool.NONE) }
    var textDialog by remember { mutableStateOf(false) }
    var drawColor by remember { mutableIntStateOf(0xFFFF2D78.toInt()) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(sourceUri) {
        if (sourceUri == null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) { BitmapUtils.decodeUri(context, sourceUri, 4096) }
        base = loaded
        previewSource = loaded?.let {
            val maxDim = 1080
            val largest = maxOf(it.width, it.height)
            if (largest > maxDim) {
                val scale = maxDim.toFloat() / largest
                Bitmap.createScaledBitmap(
                    it,
                    (it.width * scale).toInt().coerceAtLeast(1),
                    (it.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else it
        }
    }

    val cropping = tool == EditorTool.CROP
    // Geometry preview (colour is applied via a Compose ColorFilter below). While the
    // crop tool is active we show the whole frame so the handles make sense.
    val previewBitmap: Bitmap? = remember(previewSource, edit.rotationDeg, edit.flipH, edit.flipV, edit.crop, cropping) {
        previewSource?.let { src ->
            var b = src
            if (edit.rotationDeg != 0f) b = BitmapUtils.rotate(b, edit.rotationDeg)
            if (edit.flipH) b = BitmapUtils.flip(b, true)
            if (edit.flipV) b = BitmapUtils.flip(b, false)
            if (!cropping && edit.crop != CropRect.FULL) b = ImageExporter.cropTo(b, edit.crop)
            b
        }
    }

    val colorMatrix = remember(edit.filter, edit.filterIntensity, edit.adjustments) {
        ColorMatrix(
            ColorMatrixMath.compose(
                edit.filter.matrixAt(edit.filterIntensity),
                edit.adjustments.matrix()
            )
        )
    }
    val needsColor = !edit.filter.isIdentity || !edit.adjustments.isDefault

    // Portrait depth gets a real preview here (unlike the camera, we can afford to
    // segment a single still image).
    //
    // Segmentation costs ~200 ms, so it is split from the blur and debounced: the mask is
    // recomputed only when the subject or the geometry changes, while dragging the blur
    // slider just re-runs the (cheap) blur. Previously the whole pipeline re-ran on every
    // slider tick, which made the panel unusable.
    var portraitMask by remember { mutableStateOf<Bitmap?>(null) }
    var portraitPreview by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(
        edit.portrait.enabled, edit.portrait.subjectPoint,
        previewSource, edit.rotationDeg, edit.flipH, edit.flipV, edit.crop
    ) {
        if (!edit.portrait.enabled) {
            portraitMask = null
            return@LaunchedEffect
        }
        delay(220) // let taps and drags settle before running inference
        val src = previewBitmap ?: return@LaunchedEffect
        portraitMask = SubjectSegmenter.subjectMask(context, src, edit.portrait.subjectPoint)
    }

    LaunchedEffect(
        portraitMask, edit.portrait.enabled, edit.portrait.strength,
        previewSource, edit.rotationDeg, edit.flipH, edit.flipV, edit.crop
    ) {
        val src = previewBitmap
        val mask = portraitMask
        portraitPreview = if (edit.portrait.enabled && src != null && mask != null) {
            PortraitBokeh.apply(src, mask, edit.portrait.strength)
        } else {
            null
        }
    }

    fun exportToGallery(onDone: (Uri?) -> Unit) {
        val b = base ?: return
        saving = true
        scope.launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    val out = PhotoProcessor.process(context, b, edit)
                    val bytes = BitmapUtils.toJpegBytes(out)
                    val u = MediaStoreUtils.createExportImageUri(context, "edited")
                    MediaStoreUtils.writeImage(context, u, bytes)
                    u
                } catch (t: Throwable) {
                    null
                }
            }
            saving = false
            Toast.makeText(
                context,
                if (uri != null) "Saved to gallery" else "Save failed",
                Toast.LENGTH_SHORT
            ).show()
            onDone(uri)
        }
    }

    fun save() = exportToGallery { if (it != null) onBack() }

    fun share() = exportToGallery { if (it != null) ShareUtils.shareMedia(context, it, "image/jpeg") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { undoEdit() }, enabled = history.canUndo) {
                Icon(
                    Icons.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (history.canUndo) Color.White else Color.White.copy(alpha = 0.28f)
                )
            }
            IconButton(onClick = { redoEdit() }, enabled = history.canRedo) {
                Icon(
                    Icons.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (history.canRedo) Color.White else Color.White.copy(alpha = 0.28f)
                )
            }
            IconButton(onClick = { share() }, enabled = !saving && base != null) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
            }
            TextButton(onClick = { save() }, enabled = !saving && base != null) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Accent)
                Text(if (saving) "Saving…" else "Save", color = Accent, fontWeight = FontWeight.SemiBold)
            }
        }

        // Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(previewBitmap.width.toFloat() / previewBitmap.height.toFloat())
                            .fillMaxWidth()
                            .pointerInput(tool) {
                                detectTapGestures { offset ->
                                    if (tool != EditorTool.PORTRAIT) return@detectTapGestures
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    if (w <= 0f || h <= 0f) return@detectTapGestures
                                    edit = edit.copy(
                                        portrait = edit.portrait.copy(
                                            subjectPoint = Offset(
                                                (offset.x / w).coerceIn(0f, 1f),
                                                (offset.y / h).coerceIn(0f, 1f)
                                            ),
                                            enabled = true
                                        )
                                    )
                                }
                            }
                    ) {
                        Image(
                            bitmap = (portraitPreview ?: previewBitmap).asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = if (needsColor) ColorFilter.colorMatrix(colorMatrix) else null,
                            modifier = Modifier.fillMaxSize()
                        )
                        DrawLayer(
                            strokes = edit.strokes,
                            active = tool == EditorTool.DRAW,
                            colorArgb = drawColor,
                            widthFraction = 0.012f,
                            onAddStroke = { edit = edit.copy(strokes = edit.strokes + it) },
                            modifier = Modifier.fillMaxSize()
                        )
                        StickerLayer(
                            stickers = edit.stickers,
                            onUpdate = { updated ->
                                edit = edit.copy(stickers = edit.stickers.map { if (it.id == updated.id) updated else it })
                            },
                            onDelete = { id -> edit = edit.copy(stickers = edit.stickers.filterNot { it.id == id }) },
                            modifier = Modifier.fillMaxSize()
                        )
                        TextItemLayer(
                            texts = edit.texts,
                            onUpdate = { updated ->
                                edit = edit.copy(texts = edit.texts.map { if (it.id == updated.id) updated else it })
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        FrameOverlay(edit.frame, Modifier.fillMaxSize())
                        if (cropping) {
                            CropOverlay(
                                crop = edit.crop,
                                aspectLock = edit.cropAspectLock,
                                onCropChange = { edit = edit.copy(crop = it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                Text("Loading…", color = Color.White.copy(alpha = 0.6f))
            }
        }

        // Tool panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF14141A))
                .navigationBarsPadding()
        ) {
            when (tool) {
                EditorTool.NONE -> Unit
                EditorTool.CROP -> CropPanel(edit.cropAspectLock) { aspect ->
                    edit = edit.copy(
                        crop = if (aspect == null) CropRect.FULL else CropRect.centered(aspect),
                        cropAspectLock = aspect
                    )
                }
                EditorTool.ROTATE -> RotatePanel(
                    onRotate = { edit = edit.copy(rotationDeg = (edit.rotationDeg + 90f) % 360f) },
                    onFlipH = { edit = edit.copy(flipH = !edit.flipH) },
                    onFlipV = { edit = edit.copy(flipV = !edit.flipV) }
                )
                EditorTool.ADJUST -> AdjustPanel(edit.adjustments) { edit = edit.copy(adjustments = it) }
                EditorTool.FILTER -> Box(Modifier.padding(vertical = 10.dp)) {
                    FilterStrip(
                        selected = edit.filter,
                        onSelect = { edit = edit.copy(filter = it) },
                        intensity = edit.filterIntensity,
                        onIntensityChange = { edit = edit.copy(filterIntensity = it) }
                    )
                }
                EditorTool.PORTRAIT -> PortraitEditorPanel(
                    settings = edit.portrait,
                    onChange = { edit = edit.copy(portrait = it) }
                )
                EditorTool.STICKER -> StickerPanel(
                    onPick = { glyph ->
                        edit = edit.copy(
                            stickers = edit.stickers + PlacedSticker(
                                id = System.currentTimeMillis(),
                                emoji = glyph,
                                x = 0.5f,
                                y = 0.4f
                            )
                        )
                    }
                )
                EditorTool.TEXT -> TextPanel(onAdd = { textDialog = true })
                EditorTool.DRAW -> DrawPanel(drawColor) { drawColor = it }
                EditorTool.FRAME -> FramePanel(edit.frame) { edit = edit.copy(frame = it) }
            }
        }

        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0B0F))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EditorToolButton(Icons.Filled.Crop, "Crop", tool == EditorTool.CROP) { tool = toggle(tool, EditorTool.CROP) }
            EditorToolButton(Icons.Filled.RotateRight, "Rotate", tool == EditorTool.ROTATE) { tool = toggle(tool, EditorTool.ROTATE) }
            EditorToolButton(Icons.Filled.Tune, "Adjust", tool == EditorTool.ADJUST) { tool = toggle(tool, EditorTool.ADJUST) }
            EditorToolButton(Icons.Filled.AutoAwesome, "Filter", tool == EditorTool.FILTER) { tool = toggle(tool, EditorTool.FILTER) }
            EditorToolButton(Icons.Filled.EmojiEmotions, "Sticker", tool == EditorTool.STICKER) { tool = toggle(tool, EditorTool.STICKER) }
            EditorToolButton(Icons.Filled.TextFields, "Text", tool == EditorTool.TEXT) { tool = toggle(tool, EditorTool.TEXT) }
            EditorToolButton(Icons.Filled.Brush, "Draw", tool == EditorTool.DRAW) { tool = toggle(tool, EditorTool.DRAW) }
            EditorToolButton(Icons.Filled.FilterFrames, "Frame", tool == EditorTool.FRAME) { tool = toggle(tool, EditorTool.FRAME) }
            EditorToolButton(Icons.Filled.Portrait, "Portrait", tool == EditorTool.PORTRAIT) { tool = toggle(tool, EditorTool.PORTRAIT) }
        }
    }

    if (textDialog) {
        AddTextDialog(
            onDismiss = { textDialog = false },
            onAdd = { value ->
                edit = edit.copy(
                    texts = edit.texts + TextItem(
                        id = System.currentTimeMillis(),
                        text = value,
                        x = 0.5f,
                        y = 0.5f
                    )
                )
                textDialog = false
            }
        )
    }
}

private fun toggle(current: EditorTool, target: EditorTool): EditorTool =
    if (current == target) EditorTool.NONE else target

@Composable
private fun EditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) Accent else Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            label,
            color = if (selected) Accent else Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp
        )
    }
}

@Composable
private fun CropPanel(current: Float?, onPick: (Float?) -> Unit) {
    val options = listOf(
        "Free" to null,
        "1:1" to 1f,
        "4:5" to 4f / 5f,
        "3:4" to 3f / 4f,
        "16:9" to 16f / 9f,
        "9:16" to 9f / 16f
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { (label, value) ->
            val selected = (value == null && current == null) ||
                (value != null && current != null && kotlin.math.abs(value - current) < 0.001f)
            Text(
                label,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Accent else Color.White.copy(alpha = 0.08f))
                    .clickable { onPick(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun RotatePanel(onRotate: () -> Unit, onFlipH: () -> Unit, onFlipV: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Rotate 90°",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onRotate)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
        Text(
            "Flip H",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onFlipH)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
        Text(
            "Flip V",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onFlipV)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AdjustPanel(adjustments: Adjustments, onChange: (Adjustments) -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        AdjustSlider("Brightness", adjustments.brightness, -1f, 1f) { onChange(adjustments.copy(brightness = it)) }
        AdjustSlider("Contrast", adjustments.contrast, 0.5f, 2f) { onChange(adjustments.copy(contrast = it)) }
        AdjustSlider("Saturation", adjustments.saturation, 0f, 2f) { onChange(adjustments.copy(saturation = it)) }
        AdjustSlider("Warmth", adjustments.warmth, -1f, 1f) { onChange(adjustments.copy(warmth = it)) }
    }
}

@Composable
private fun AdjustSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = min..max, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TextPanel(onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Accent)
        Text(
            "Add text",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onAdd)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DrawPanel(selected: Int, onSelect: (Int) -> Unit) {
    val colors = listOf(
        0xFFFF2D78.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(),
        0xFFFFC107.toInt(), 0xFF00E5FF.toInt(), 0xFF7C3AED.toInt(),
        0xFF4CAF50.toInt(), 0xFFFF5722.toInt()
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(colors) { c ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(
                        width = if (c == selected) 3.dp else 1.dp,
                        color = if (c == selected) Accent else Color.White.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(c) }
            )
        }
    }
}

@Composable
private fun FramePanel(selected: FrameStyle, onSelect: (FrameStyle) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(FrameStyle.entries.toList()) { frame ->
            Text(
                frame.label,
                color = if (frame == selected) Color.White else Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (frame == selected) Accent else Color.White.copy(alpha = 0.08f))
                    .clickable { onSelect(frame) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/** Compose preview of the frame so what you see matches the export. */
@Composable
private fun FrameOverlay(frame: FrameStyle, modifier: Modifier = Modifier) {
    when (frame) {
        FrameStyle.NONE, FrameStyle.VIGNETTE -> Unit
        FrameStyle.WHITE -> Box(modifier.border(6.dp, Color.White))
        FrameStyle.ROUNDED -> Box(modifier.border(5.dp, Color.White, RoundedCornerShape(10.dp)))
        FrameStyle.FILM -> Column(modifier) {
            Box(Modifier.fillMaxWidth().weight(0.075f).background(Color.Black))
            Spacer(Modifier.weight(0.85f))
            Box(Modifier.fillMaxWidth().weight(0.075f).background(Color.Black))
        }
    }
}

@Composable
private fun PortraitEditorPanel(
    settings: PortraitSettings,
    onChange: (PortraitSettings) -> Unit
) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Portrait depth", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (settings.enabled) "On" else "Off",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (settings.enabled) Accent else Color.White.copy(alpha = 0.12f))
                    .clickable { onChange(settings.copy(enabled = !settings.enabled)) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Blur", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.width(52.dp))
            Slider(
                value = settings.strength,
                onValueChange = { onChange(settings.copy(strength = it, enabled = true)) },
                valueRange = 0.05f..1f,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            "Tap the photo to choose what stays sharp. Everything else is blurred.",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 10.sp
        )
    }
}

/**
 * Undo/redo stack for the editor.
 *
 * Rapid changes are coalesced: a new entry is only pushed when more than [COALESCE_MS]
 * has passed since the last one, so dragging a slider undoes in a single step instead of
 * filling the stack with one entry per frame. `canUndo`/`canRedo` are Compose state, so
 * the toolbar buttons enable and disable themselves.
 */
private class EditHistory {
    private val past = ArrayDeque<ImageEditState>()
    private val future = ArrayDeque<ImageEditState>()
    private var lastRecordedAt = 0L

    var canUndo by mutableStateOf(false); private set
    var canRedo by mutableStateOf(false); private set

    fun record(previous: ImageEditState) {
        val now = System.currentTimeMillis()
        if (now - lastRecordedAt > COALESCE_MS) {
            past.addLast(previous)
            while (past.size > MAX_ENTRIES) past.removeFirst()
        }
        future.clear()
        lastRecordedAt = now
        sync()
    }

    fun undo(current: ImageEditState): ImageEditState? {
        val previous = past.removeLastOrNull() ?: return null
        future.addLast(current)
        lastRecordedAt = 0L
        sync()
        return previous
    }

    fun redo(current: ImageEditState): ImageEditState? {
        val next = future.removeLastOrNull() ?: return null
        past.addLast(current)
        lastRecordedAt = 0L
        sync()
        return next
    }

    private fun sync() {
        canUndo = past.isNotEmpty()
        canRedo = future.isNotEmpty()
    }

    private companion object {
        const val COALESCE_MS = 700L
        const val MAX_ENTRIES = 60
    }
}

@Composable
private fun AddTextDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add text") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = false,
                placeholder = { Text("Type something…") }
            )
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onAdd(value) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
