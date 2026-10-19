package com.camara.app.editor.video

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.camara.app.filters.Filter
import com.camara.app.filters.FilterPresets
import com.camara.app.ui.components.FilterStrip
import com.camara.app.ui.theme.Accent
import com.camara.app.util.MediaStoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoEditorScreen(
    sourceUri: Uri?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exo = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) {
        onDispose { exo.release() }
    }

    var durationMs by remember { mutableFloatStateOf(0f) }
    var range by remember { mutableStateOf(0f..0f) }
    var filter by remember { mutableStateOf<Filter>(FilterPresets.NONE) }
    var exporting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var transformerRef by remember { mutableStateOf<Transformer?>(null) }

    LaunchedEffect(sourceUri) {
        if (sourceUri == null) return@LaunchedEffect
        val dur = withContext(Dispatchers.IO) {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(context, sourceUri)
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            } catch (t: Throwable) {
                0L
            } finally {
                try { r.release() } catch (_: Throwable) { }
            }
        }
        durationMs = dur.toFloat()
        range = 0f..dur.toFloat()
        exo.setMediaItem(MediaItem.fromUri(sourceUri))
        exo.prepare()
    }

    LaunchedEffect(exporting) {
        while (exporting) {
            transformerRef?.let { t ->
                val holder = ProgressHolder()
                if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    progress = holder.progress / 100f
                }
            }
            delay(200)
        }
    }

    fun export() {
        val uri = sourceUri ?: return
        exporting = true
        progress = 0f

        val output = File(context.cacheDir, "camara/export_${System.currentTimeMillis()}.mp4")
        output.parentFile?.mkdirs()

        val startMs = range.start.toLong()
        val endMs = if (range.endInclusive <= 0f) C.TIME_END_OF_SOURCE else range.endInclusive.toLong()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()

        val edited = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), VideoEffects.forFilter(filter)))
            .build()

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            try {
                                val dest = MediaStoreUtils.createExportVideoUri(context)
                                context.contentResolver.openOutputStream(dest)?.use { out ->
                                    output.inputStream().use { it.copyTo(out) }
                                }
                                true
                            } catch (t: Throwable) {
                                false
                            }
                        }
                        exporting = false
                        Toast.makeText(
                            context,
                            if (ok) "Saved to gallery" else "Export failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (ok) onBack()
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    exporting = false
                    Toast.makeText(context, "Export error: ${exportException.message}", Toast.LENGTH_LONG).show()
                }
            })
            .build()

        transformerRef = transformer
        transformer.start(edited, output.absolutePath)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
    ) {
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
            Text(
                "Video editor",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { export() }, enabled = !exporting && sourceUri != null) {
                Text(if (exporting) "Exporting…" else "Export", color = Accent, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exo
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (exporting) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Exporting…", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Trim
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ContentCut, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                Text(
                    "Trim",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${fmt(range.start)} – ${fmt(range.endInclusive)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            if (durationMs > 0f) {
                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = 0f..durationMs
                )
            }
        }

        // Filters
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            FilterStrip(selected = filter, onSelect = { filter = it })
        }
    }
}

private fun fmt(ms: Float): String {
    val totalSeconds = (ms / 1000f).toInt()
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
