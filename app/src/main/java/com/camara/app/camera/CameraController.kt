package com.camara.app.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
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
import java.util.concurrent.Executor

/**
 * Wraps CameraX: owns the use cases, exposes Compose-observable state, and
 * handles photo/video capture. One instance per camera screen.
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

    private var provider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var recording: Recording? = null
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null

    private val executor: Executor = ContextCompat.getMainExecutor(context)

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

            val previewUseCase = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }

            val ic = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
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

            val useCases: Array<UseCase> = arrayOf(previewUseCase, ic, vc)
            camera = p.bindToLifecycle(owner, selector, *useCases)

            imageCapture = ic
            videoCapture = vc

            camera?.cameraInfo?.zoomState?.observe(owner) { state: ZoomState ->
                maxZoom = state.maxZoomRatio
            }
            applyZoom()
            LiveFilterEffect.apply(view, filter)
        } catch (t: Throwable) {
            errorMessage = t.message ?: "Camera failed to bind"
        }
    }

    fun setMode(newMode: CaptureMode) {
        if (mode == newMode) return
        if (isRecording) stopRecording()
        mode = newMode
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

    fun setFilter(newFilter: Filter) {
        filter = newFilter
        previewView?.let { LiveFilterEffect.apply(it, newFilter) }
    }

    fun setBeauty(settings: BeautySettings) { beauty = settings }

    fun setExtension(ext: CamaraExtension) {
        if (extension == ext) return
        extension = ext
        rebind()
    }

    fun setZoomRatio(ratio: Float) {
        zoom = ratio.coerceIn(1f, if (maxZoom > 1f) maxZoom else 1f)
        applyZoom()
    }

    private fun applyZoom() {
        try {
            camera?.cameraControl?.setZoomRatio(zoom)
        } catch (_: Throwable) {
        }
    }

    /** Captures a photo, bakes in the current filter + beauty, then reports the Uri. */
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
        provider?.unbindAll()
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
}
