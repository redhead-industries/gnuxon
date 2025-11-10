package redhead.app.gnuxon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import redhead.app.gnuxon.service.BackgroundRecordingService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camera : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var recordButton: Button
    private lateinit var tvControls: TextView

    private var videoCapture: VideoCapture<Recorder>? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var backgroundRecordingService: BackgroundRecordingService

    private var activeRecording: Recording? = null
    private var isRecording = false

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        recordButton = findViewById(R.id.btn_record)
        tvControls = findViewById(R.id.tv_controls)

        backgroundRecordingService = BackgroundRecordingService(this)

        recordButton.setOnClickListener {
            if (isRecording) stopVideoRecording() else startVideoRecording()
        }

        if (hasAllPermissions()) startCamera() else requestPermissions()
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        requestPermissions(requiredPermissions, 100)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("GNUXON requires camera and microphone permissions to record videos.")
            .setPositiveButton("Retry") { _, _ -> requestPermissions() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture as UseCase)
            } catch (e: Exception) {
                Toast.makeText(this, "Camera init failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> if (!isRecording) startVideoRecording()
            KeyEvent.KEYCODE_VOLUME_DOWN -> if (isRecording) stopVideoRecording()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startVideoRecording() {
        val capture = videoCapture ?: return
        try {
            val outputOptions = FileOutputOptions.Builder(getOutputFile()).build()
            val recordingSetup = capture.output.prepareRecording(this, outputOptions).apply {
                if (ContextCompat.checkSelfPermission(this@Camera, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    withAudioEnabled()
            }

            val recording = recordingSetup.start(ContextCompat.getMainExecutor(this)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        hideUI()
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        backgroundRecordingService.showRecordingNotification()
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        showUI()
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        backgroundRecordingService.hideRecordingNotification()
                        Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            activeRecording = recording
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVideoRecording() {
        try {
            activeRecording?.stop()
            activeRecording = null
        } catch (e: Exception) {
            Toast.makeText(this, "Error stopping recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getOutputFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val moviesDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "GNUXON")
        if (!moviesDir.exists()) moviesDir.mkdirs()
        return File(moviesDir, "GNUXON-$timestamp.mp4")
    }

    private fun hideUI() {
        recordButton.visibility = View.GONE
        tvControls.visibility = View.VISIBLE
        supportActionBar?.hide()
    }

    private fun showUI() {
        recordButton.visibility = View.VISIBLE
        tvControls.visibility = View.GONE
        supportActionBar?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        backgroundRecordingService.cleanup()
    }
}
