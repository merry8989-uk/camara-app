package com.camara.app.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
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
import com.camara.app.lenses.FaceState
import com.camara.app.lenses.Lens
import com.camara.app.lenses.LensCatalog
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
    var beauty by mutableStateOf(BeautySettings.DEFAULT); private set
    var timerSeconds by mutableIntStateOf(0); private set
    var showGrid by mutableStateOf(false); private set
    var extension by mutableStateOf(CamaraExtension.AUTO); private set
    var errorMessage by mutableStateOf<String?>(null); private set

    // --- AR lenses ---
    var lens by mutableStateOf<Lens?>(LensCatalog.NONE); private set
    var faceState by mutableStateOf<FaceState?>(null); private set
    var lensesEnabled by mutableStateOf(false); private set
    var lensesSupported by mutableStateOf(true); private set

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

            val wantsAnalysis = lens?.art?.isNotEmpty() == true
            val analysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor, FaceAnalyzer { face ->
                // Hop to the main thread before writing Compose state.
                executor.execute { if (lensesEnabled) faceState = face }
            })

            val useCases = mutableListOf<UseCase>(previewUseCase)
            when (mode) {
                CaptureMode.PHOTO -> useCases += ic
                CaptureMode.VIDEO -> useCases += vc
            }
            if (wantsAnalysis) useCases += analysis

            lensesSupported = true
            camera = try {
                p.bindToLifecycle(owner, selector, *useCases.toTypedArray())
            } catch (t: Throwable) {
                if (wantsAnalysis) {
                    // Some devices cap concurrent use cases; retry without analysis.
                    analysis.clearAnalyzer()
                    lensesSupported = false
                    val reduced = useCases.filter { it !== analysis }.toTypedArray()
                    p.bindToLifecycle(owner, selector, *reduced)
                } else {
                    errorMessage = t.message ?: "Camera failed to bind"
                    return
                }
            }

            imageCapture = ic
            videoCapture = vc
            imageAnalysis = analysis
            if (!wantsAnalysis || !lensesSupported) faceState = null

            camera?.cameraInfo?.zoomState?.observe(owner) { state: ZoomState ->
                maxZoom = state.maxZoomRatio
            }
            applyZoom()
            LiveFilterEffect.apply(view, filter, beauty)
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

    fun applyBeauty(settings: BeautySettings) {
        beauty = settings
        refreshPreviewEffect()
    }

    fun applyLens(newLens: Lens) {
        val wasActive = lensesEnabled
        lens = newLens
        lensesEnabled = newLens.art.isNotEmpty()
        if (!lensesEnabled) faceState = null
        // Analysis is only bound when a lens is active, so rebind on that edge.
        if (wasActive != lensesEnabled) rebind()
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
        previewView?.let { LiveFilterEffect.apply(it, filter, beauty) }
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

    fun startRecording(onSaved: (Uri) -> Unit) {
        val vc = videoCapture ?: return
        if (isRecording) return

        val options = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).build()

        var pending = vc.output.prepareRecording(context, options)
        if (hasAudioPermission()) pending = pending.withAudioEnabled()

        recording = pending.start(executor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> isRecording = true
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    if (event.hasError()) {
                        errorMessage = "Recording error (${event.error})"
                    } else {
                        onSaved(event.outputResults.outputUri)
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
