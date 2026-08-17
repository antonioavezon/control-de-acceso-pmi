package cl.controlacceso.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    private var cameraExecutor: ExecutorService? = null
    private var barcodeAnalyzer: BarcodeAnalyzer? = null

    fun bind(
        previewView: PreviewView,
        onQrDetected: (String) -> Unit
    ) {
        unbind()

        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeAnalyzer = BarcodeAnalyzer(onQrDetected)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        cameraExecutor!!,
                        barcodeAnalyzer!!
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (_: Exception) {
                // Camera binding failed; UI remains without preview.
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setPaused(paused: Boolean) {
        barcodeAnalyzer?.isPaused = paused
    }

    fun unbind() {
        barcodeAnalyzer?.close()
        barcodeAnalyzer = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
        try {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        } catch (_: Exception) {
            // Provider may not be ready yet.
        }
    }
}
