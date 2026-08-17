package cl.controlacceso

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cl.controlacceso.camera.CameraController
import cl.controlacceso.data.EventRepository
import cl.controlacceso.databinding.ActivityMainBinding
import cl.controlacceso.model.ScanResult

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var eventRepository: EventRepository
    private var cameraController: CameraController? = null

    private var hasRequestedCameraPermission = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayHideRunnable: Runnable? = null

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequestedCameraPermission = true
        if (granted) {
            onCameraPermissionGranted()
        } else {
            handleCameraPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cl.controlacceso.util.UiHelpers.liftContentAboveNavBar(binding.root)

        eventRepository = (application as ControlAccesoApp).eventRepository

        binding.backHomeButton.setOnClickListener { finish() }
        binding.permissionActionButton.setOnClickListener {
            when (binding.permissionActionButton.tag as? PermissionAction) {
                PermissionAction.OPEN_SETTINGS -> openAppSettings()
                else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        updateCounter()
        checkCameraPermission()
    }

    override fun onResume() {
        super.onResume()
        updateCounter()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED &&
            binding.permissionContainer.visibility == View.VISIBLE &&
            hasRequestedCameraPermission
        ) {
            onCameraPermissionGranted()
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED &&
            binding.cameraContainer.visibility == View.VISIBLE
        ) {
            cameraController?.setPaused(false)
        }
    }

    override fun onPause() {
        cameraController?.setPaused(true)
        super.onPause()
    }

    override fun onDestroy() {
        overlayHideRunnable?.let { mainHandler.removeCallbacks(it) }
        cameraController?.unbind()
        super.onDestroy()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> onCameraPermissionGranted()
            else -> showPermissionUi(PermissionAction.REQUEST)
        }
    }

    private fun handleCameraPermissionDenied() {
        val showRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        when {
            showRationale -> showPermissionUi(PermissionAction.RETRY)
            hasRequestedCameraPermission -> showPermissionUi(PermissionAction.OPEN_SETTINGS)
            else -> showPermissionUi(PermissionAction.RETRY)
        }
    }

    private fun onCameraPermissionGranted() {
        binding.permissionContainer.visibility = View.GONE
        binding.cameraContainer.visibility = View.VISIBLE

        if (cameraController == null) {
            cameraController = CameraController(this, this)
        }
        try {
            cameraController?.bind(binding.previewView, ::onQrScanned)
        } catch (e: Exception) {
            Log.e(TAG, "Cámara no disponible", e)
            binding.permissionContainer.visibility = View.VISIBLE
            binding.cameraContainer.visibility = View.GONE
            binding.permissionMessage.text = "La cámara no está disponible en este dispositivo."
            binding.permissionActionButton.visibility = View.GONE
        }
    }

    private fun showPermissionUi(action: PermissionAction) {
        binding.permissionContainer.visibility = View.VISIBLE
        binding.cameraContainer.visibility = View.GONE
        cameraController?.unbind()

        binding.permissionActionButton.visibility = View.VISIBLE
        binding.permissionActionButton.tag = action
        when (action) {
            PermissionAction.REQUEST, PermissionAction.RETRY -> {
                binding.permissionMessage.text = getString(R.string.camera_permission_message)
                binding.permissionActionButton.text = getString(R.string.camera_permission_retry)
            }
            PermissionAction.OPEN_SETTINGS -> {
                binding.permissionMessage.text = getString(R.string.camera_permission_denied_permanent)
                binding.permissionActionButton.text = getString(R.string.camera_permission_settings)
            }
        }

        if (action == PermissionAction.REQUEST) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }

    private fun updateCounter() {
        binding.counterText.text = getString(
            R.string.entries_counter,
            eventRepository.totalAsistentes,
            eventRepository.totalInvitados
        )
    }

    private fun onQrScanned(rawValue: String) {
        runOnUiThread {
            try {
                val result = eventRepository.processScan(rawValue)
                when (result) {
                    is ScanResult.Permitido -> {
                        binding.pmiDetectedText.visibility = View.VISIBLE
                        binding.pmiDetectedText.text = getString(R.string.pmi_detected, result.pmiId)
                    }
                    is ScanResult.NoPermitido -> {
                        val detected = result.pmiIdDetectado
                        if (detected != null) {
                            binding.pmiDetectedText.visibility = View.VISIBLE
                            binding.pmiDetectedText.text = getString(R.string.pmi_detected, detected)
                        }
                    }
                }
                showScanResult(result)
                updateCounter()
                pauseScanning()
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando QR", e)
                showScanResult(
                    ScanResult.NoPermitido("Error al procesar el código QR. Intente nuevamente.")
                )
                pauseScanning()
            }
        }
    }

    private fun showScanResult(result: ScanResult) {
        overlayHideRunnable?.let { mainHandler.removeCallbacks(it) }
        binding.resultOverlay.visibility = View.VISIBLE

        when (result) {
            is ScanResult.Permitido -> {
                binding.resultOverlay.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.overlay_green)
                )
                binding.overlayStatusText.text = getString(R.string.status_permitido)
                binding.overlayDetailText.text =
                    "${result.nombreCompleto}\nPMI ID: ${result.pmiId}\n${result.email}"
                binding.overlaySecondaryText.visibility =
                    if (result.esReingreso) View.VISIBLE else View.GONE
                binding.overlaySecondaryText.text = if (result.esReingreso) {
                    getString(R.string.status_reingreso) + "\nPrimer ingreso: ${result.horaEntrada}"
                } else {
                    "Primer ingreso: ${result.horaEntrada}"
                }
                if (!result.esReingreso) {
                    binding.overlaySecondaryText.visibility = View.VISIBLE
                }
                vibrateAccepted()
            }
            is ScanResult.NoPermitido -> {
                binding.resultOverlay.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.overlay_red)
                )
                binding.overlayStatusText.text = getString(R.string.status_no_permitido)
                binding.overlayDetailText.text = result.motivo
                binding.overlaySecondaryText.visibility = View.GONE
                vibrateRejected()
            }
        }

        overlayHideRunnable = Runnable {
            binding.resultOverlay.visibility = View.GONE
        }
        mainHandler.postDelayed(overlayHideRunnable!!, OVERLAY_DURATION_MS)
    }

    private fun pauseScanning() {
        cameraController?.setPaused(true)
        mainHandler.postDelayed({
            cameraController?.setPaused(false)
        }, SCAN_COOLDOWN_MS)
    }

    private fun vibrateAccepted() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateRejected() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
    }

    private enum class PermissionAction {
        REQUEST,
        RETRY,
        OPEN_SETTINGS
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val SCAN_COOLDOWN_MS = 2500L
        private const val OVERLAY_DURATION_MS = 2500L
    }
}
