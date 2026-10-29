package com.camara.app.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import java.io.File
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.camara.app.beauty.BeautySettings
import com.camara.app.filters.Filter
import com.camara.app.filters.FilterPresets
import com.camara.app.filters.LiveFilterEffect
import com.camara.app.lenses.FaceAnalyzer
import com.camara.app.lenses.FaceLook
import com.camara.app.lenses.FaceState
import androidx.compose.ui.geometry.Offset
import com.camara.app.lenses.Lens
import com.camara.app.lenses.LensCatalog
import com.camara.app.portrait.PortraitSettings
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Wraps CameraX: owns the use cases, exposes Compose-observable state, handles
 * photo/video capture and AR face detection.
 *
 * Only the use cases needed for the current mode are bound (Preview + ImageCapture
 * *or* VideoCapture, plus ImageAnalysis only when a lens is active). Binding four
 * use cases at once exceeds the guaranteed combination on many devices, which is
 * why face lenses previously reported themselves as unsupported.
 */
class CameraController(private val context: Context) {

    var lensFacing by mutableStateOf(CameraSelector.LENS_FACING_BACK); private set
    var flashMode by mutableStateOf(ImageCapture.FLASH_MODE_OFF); private set
    var mode by mutableStateOf(CaptureMode.PHOTO); private set
    var zoom by mutableFloatStateOf(1f); private set
    var maxZoom by mutableFloatStateOf(1f); private set
    var isRecording by mutableStateOf(false); private set
    var filter by mutableStateOf(FilterPresets.NONE); private set
    var filterIntensity by mutableFloatStateOf(1f); private set
    var beauty by mutableStateOf(BeautySettings.DEFAULT); private set
    var portrait by mutableStateOf(PortraitSettings()); private set
    var timerSeconds by mutableIntStateOf(0); private set
    var showGrid by mutableStateOf(false); private set
    var extension by mutableStateOf(CamaraExtension.AUTO); private set
    var errorMessage by mutableStateOf<String?>(null); private set

    // --- AR lenses ---
    var lens by mutableStateOf<Lens?>(LensCatalog.NONE); private set
    var faceState by mutableStateOf<FaceState?>(null); private set
    var lensesEnabled by mutableStateOf(false); private set
    var lensesSupported by mutableStateOf(true); private set

    /**
     * The face-only effect layer, drawn over the preview.
     *
     * Frame-sized with everything outside the face transparent, so the hardware preview
     * shows through wherever this is blank. Null means "apply nothing" — which is also
     * the safe fallback if compositing ever fails.
     */
    var faceLayer by mutableStateOf<Bitmap?>(null); private set

    // Layers handed to the UI but not yet certainly finished with. See publishLayer.
    private val recentLayers = ArrayDeque<Bitmap?>()

    private companion object {
        /** How many recent face layers to keep alive before releasing the oldest. */
        const val LAYER_HOLD = 3
    }

    private var provider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var recording: Recording? = null
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null

    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private val analysisExecutor: Executor = Executors.newSingleThreadExecutor()

    fun attach(owner: LifecycleOwner, view: PreviewView) {
        lifecycleOwner = owner
        previewView = view
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                provider = future.get()
                rebind()
            } catch (t: Throwable) {
                errorMessage = t.message ?: "Camera failed to start"
            }
        }, executor)
    }

    /**
     * Attaches the face analyzer. It is only attached while a lens is active, so frames
     * aren't analysed (and the model isn't run) when nothing needs them.
     */
    private fun attachAnalyzer(analysis: ImageAnalysis) {
        analysis.setAnalyzer(
            analysisExecutor,
            FaceAnalyzer(
                look = { liveLook() },
                onFace = { face ->
                    // Hop to the main thread before writing Compose state.
                    executor.execute { if (lensesEnabled) faceState = face }
                },
                onLayer = ::publishLayer
            )
        )
    }

    /** The look the live face compositor should apply, or null when there is none. */
    private fun liveLook(): FaceLook? {
        val look = FaceLook(filter, filterIntensity, beauty)
        return if (look.isActive) look else null
    }

    /**
     * Hands the newest face layer to the UI.
     *
     * The previous layers are kept for a couple of frames before being released: Compose
     * may still be drawing the one it was last given, and recycling that under the canvas
     * is a hard crash. Anything three frames old is certainly finished with.
     */
    private fun publishLayer(layer: Bitmap?) {
        executor.execute {
            recentLayers.add(layer)
            faceLayer = layer
            while (recentLayers.size > LAYER_HOLD) {
                val oldest = recentLayers.removeAt(0)
                if (oldest != null && oldest !== faceLayer) oldest.recycle()
            }
        }
    }

    /** Attaches the analyzer when either a lens or a live face effect needs frames. */
    private fun syncAnalyzer() {
        val analysis = imageAnalysis ?: return
        if (!lensesSupported) return
        val needed = lensesEnabled || liveLook() != null
        if (needed && analysis.analyzer == null) {
            attachAnalyzer(analysis)
        } else if (!needed && analysis.analyzer != null) {
            analysis.clearAnalyzer()
            publishLayer(null)
            faceState = null
        }
    }

    private fun rebind() {
        val p = provider ?: return
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return
        try {
            p.unbindAll()

            val baseSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val selector = CameraExtensionResolver.resolve(context, p, baseSelector, extension)

            // Preview and analysis share the same aspect ratio so face coordinates
            // map onto the preview consistently.
            val previewUseCase = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(view.surfaceProvider) }

            val ic = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.HD,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                    )
                )
                .build()
            val vc = VideoCapture.withOutput(recorder)

            // RGBA output and a small bound size on purpose: the face compositor needs
            // real pixels every frame, and YUV conversion plus a full-resolution buffer
            // would make that unaffordable. 480x640 is ample for detection and cheap to
            // composite. The 4:3 bound keeps it matching the preview.
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(480, 640),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                )
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val useCases = mutableListOf<UseCase>(previewUseCase)
            when (mode) {
                CaptureMode.PHOTO -> useCases += ic
                CaptureMode.VIDEO -> useCases += vc
            }
            // Analysis is bound even when no lens is active. It is cheap, and it means
            // switching a lens on or off only attaches or detaches the analyzer instead
            // of rebinding the camera — a rebind restarts the preview and flickers.
            useCases += analysis

            lensesSupported = true
            camera = try {
                p.bindToLifecycle(owner, selector, *useCases.toTypedArray())
            } catch (t: Throwable) {
                // Some devices cap concurrent use cases; retry without analysis.
                analysis.clearAnalyzer()
                lensesSupported = false
                val reduced = useCases.filter { it !== analysis }.toTypedArray()
                p.bindToLifecycle(owner, selector, *reduced)
            }

            imageCapture = ic
            videoCapture = vc
            imageAnalysis = analysis
            if (!lensesSupported) faceState = null
            // Analysis is bound either way; whether an analyzer is attached depends on
            // whether a lens or a live face effect actually needs frames.
            syncAnalyzer()

            camera?.cameraInfo?.zoomState?.observe(owner) { state: ZoomState ->
                maxZoom = state.maxZoomRatio
            }
            applyZoom()
            // Colour only. The blur used to be applied to the whole view here, which
            // blurred the entire preview instead of the face - see FaceCompositor.
            LiveFilterEffect.applyColour(view, filter, filterIntensity, beauty)
        } catch (t: Throwable) {
            errorMessage = t.message ?: "Camera failed to bind"
        }
    }

    // Note: names are deliberately not setX() — the mutableStateOf properties above
    // already generate JVM setX() setters, which would clash.
    fun applyMode(newMode: CaptureMode) {
        if (mode == newMode) return
        if (isRecording) stopRecording()
        mode = newMode
        // Rebind so only the use cases this mode needs stay active.
        rebind()
    }

    fun switchCamera() {
        if (isRecording) return
        lensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
            else CameraSelector.LENS_FACING_BACK
        rebind()
    }

    fun cycleFlash() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = flashMode
    }

    fun cycleTimer() {
        timerSeconds = when (timerSeconds) {
            0 -> 3
            3 -> 10
            else -> 0
        }
    }

    fun toggleGrid() { showGrid = !showGrid }

    fun applyFilter(newFilter: Filter) {
        filter = newFilter
        refreshPreviewEffect()
    }

    fun applyFilterIntensity(value: Float) {
        filterIntensity = value.coerceIn(0f, 1f)
        refreshPreviewEffect()
    }

    fun applyBeauty(settings: BeautySettings) {
        beauty = settings
        refreshPreviewEffect()
    }

    /** Portrait depth is baked into the capture; the preview just records the settings. */
    fun applyPortrait(settings: PortraitSettings) {
        portrait = settings
    }

    fun setSubjectPoint(point: Offset?) {
        portrait = portrait.copy(subjectPoint = point)
    }

    fun applyLens(newLens: Lens) {
        lens = newLens
        lensesEnabled = newLens.art.isNotEmpty()
        if (!lensesEnabled) faceState = null

        // Attach or detach the analyzer rather than rebinding: analysis is already bound,
        // and a rebind would restart the preview for no reason. A live face effect needs
        // frames too, so the decision is made in one place.
        if (imageAnalysis != null && lensesSupported) {
            syncAnalyzer()
        } else if (lensesEnabled) {
            // Analysis isn't bound on this device — a rebind is the only way to try.
            rebind()
        }
    }

    fun applyExtension(ext: CamaraExtension) {
        if (extension == ext) return
        extension = ext
        rebind()
    }

    fun setZoomRatio(ratio: Float) {
        zoom = ratio.coerceIn(1f, if (maxZoom > 1f) maxZoom else 1f)
        applyZoom()
    }

    private fun refreshPreviewEffect() {
        previewView?.let { LiveFilterEffect.applyColour(it, filter, filterIntensity, beauty) }
        // Turning smoothing or a filter on is what starts the face compositor; turning
        // them off is what stops it and its frames.
        syncAnalyzer()
    }

    private fun applyZoom() {
        try {
            camera?.cameraControl?.setZoomRatio(zoom)
        } catch (_: Throwable) {
        }
    }

    /** Captures a photo and reports the saved Uri (filter/beauty/lens applied by caller). */
    fun takePhoto(onSaved: (Uri) -> Unit) {
        val ic = imageCapture ?: return
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "CAMARA_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        ic.takePicture(
            options,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri ?: return
                    onSaved(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    errorMessage = exception.message ?: "Capture failed"
                }
            }
        )
    }

    /**
     * Records to a temporary file and reports it once finalised.
     *
     * Not to the gallery directly: the look has to be applied to the clip first, and the
     * camera can only see the filter it was given. [RecordedVideoProcessor] takes it from
     * here.
     */
    fun startRecording(onRecorded: (File) -> Unit) {
        val vc = videoCapture ?: return
        if (isRecording) return

        val target = RecordedVideoProcessor.newRecordingFile(context)
        val options = FileOutputOptions.Builder(target).build()

        var pending = vc.output.prepareRecording(context, options)
        if (hasAudioPermission()) pending = pending.withAudioEnabled()

        recording = pending.start(executor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> isRecording = true
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    if (event.hasError()) {
                        errorMessage = "Recording error (${event.error})"
                        target.delete()
                    } else {
                        onRecorded(target)
                    }
                }
                else -> Unit
            }
        }
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    fun close() {
        try {
            recording?.stop()
        } catch (_: Throwable) {
        }
        recording = null
        imageAnalysis?.clearAnalyzer()
        provider?.unbindAll()
        analysisExecutor.let { if (it is java.util.concurrent.ExecutorService) it.shutdown() }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
}
