package pk.livecaster.app.streaming.capture

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCaptureManager(private val context: Context) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var currentLensFacing: Int = CameraSelector.LENS_FACING_BACK

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onCameraReady: () -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            startCameraPreview(lifecycleOwner, previewView)
            onCameraReady()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startCameraPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val provider = cameraProvider ?: return
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(currentLensFacing)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView): Boolean {
        currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCameraPreview(lifecycleOwner, previewView)
        return currentLensFacing == CameraSelector.LENS_FACING_FRONT
    }

    fun toggleTorch(enable: Boolean) {
        camera?.cameraControl?.enableTorch(enable)
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        camera = null
    }

    fun release() {
        unbind()
        cameraExecutor.shutdown()
    }
}
