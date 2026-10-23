package com.camara.app.camera

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.camara.app.editor.video.VideoEffects
import com.camara.app.filters.Filter
import com.camara.app.util.MediaStoreUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bakes the chosen look into a finished recording.
 *
 * The camera used to write straight to the gallery, so the filter you saw on screen was
 * never in the file. Now the clip is recorded to a temporary file and the look is applied
 * to it before it is filed away.
 *
 * **This is a post-process, not a live effect.** The recording itself is unfiltered; the
 * look is applied in one pass when you stop. Doing it live would mean running the camera
 * frames through a GL pipeline via `CameraEffect`, which would also switch off Camera
 * Extensions (HDR, Night) — a trade this deliberately avoids until it can be tested on
 * real hardware.
 *
 * A recording is never lost to a failed filter: if the pass fails, the untouched clip is
 * saved instead.
 */
@OptIn(UnstableApi::class)
object RecordedVideoProcessor {

    private const val DIR = "recordings"

    /**
     * Applies [filter] to [source] and saves the result to the gallery.
     *
     * @param onProgress 0f..1f, for the "applying filter" indicator.
     * @return the gallery Uri, or null if even saving the raw clip failed.
     */
    suspend fun process(
        context: Context,
        source: File,
        filter: Filter,
        onProgress: (Float) -> Unit,
    ): Uri? {
        onProgress(0f)

        var toSave = source
        var baked: File? = null
        if (!filter.isIdentity) {
            baked = bake(context, source, filter, onProgress)
            if (baked != null) toSave = baked
        }

        onProgress(1f)
        val uri = withContext(Dispatchers.IO) { save(context, toSave) }

        baked?.delete()
        source.delete()
        return uri
    }

    /** Runs the filter over the clip. Returns null if the export fails. */
    private suspend fun bake(
        context: Context,
        source: File,
        filter: Filter,
        onProgress: (Float) -> Unit,
    ): File? = coroutineScope {
        val out = File(context.cacheDir, DIR).apply { mkdirs() }
            .resolve("baked_${System.currentTimeMillis()}.mp4")

        val done = CompletableDeferred<Boolean>()

        // Transformer is built and started on the main thread: it wants a Looper.
        val transformer = withContext(Dispatchers.Main) {
            Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        done.complete(true)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exception: ExportException,
                    ) {
                        done.complete(false)
                    }
                })
                .build()
        }

        val poller = launch(Dispatchers.Main) {
            val holder = ProgressHolder()
            while (isActive) {
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress / 100f)
                }
                delay(200)
            }
        }

        val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(source)))
            .setEffects(Effects(emptyList(), VideoEffects.forFilter(filter, height = null)))
            .build()

        try {
            withContext(Dispatchers.Main) { transformer.start(item, out.absolutePath) }
            val ok = done.await()
            poller.cancel()
            if (ok && out.exists() && out.length() > 0) out else null.also { out.delete() }
        } catch (t: Throwable) {
            poller.cancel()
            out.delete()
            null
        }
    }

    /** Copies [file] into the shared gallery. */
    private fun save(context: Context, file: File): Uri? = try {
        val dest = MediaStoreUtils.createVideoUri(context)
        context.contentResolver.openOutputStream(dest)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        dest
    } catch (t: Throwable) {
        null
    }

    /** Where the camera writes while recording, before the look is applied. */
    fun newRecordingFile(context: Context): File =
        File(context.cacheDir, DIR).apply { mkdirs() }
            .resolve("camara_${System.currentTimeMillis()}.mp4")
}
