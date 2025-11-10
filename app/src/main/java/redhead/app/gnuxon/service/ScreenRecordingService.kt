package redhead.app.gnuxon.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import redhead.app.gnuxon.Camera
import redhead.app.gnuxon.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ScreenRecordingService : Service() {

    companion object {
        private const val TAG = "ScreenRecordingService"
        private const val CHANNEL_ID = "gnuxon_screen_recording_channel"
        private const val NOTIFICATION_ID = 3
        const val ACTION_START_RECORDING = "redhead.app.gnuxon.action.START_SCREEN_RECORDING"
        const val ACTION_STOP_RECORDING = "redhead.app.gnuxon.action.STOP_SCREEN_RECORDING"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_INTENT = "result_intent"
    }

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var screenDensity: Int = 0
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private var isRecording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
        getScreenDimensions()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                if (!isRecording) {
                    val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                    val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_INTENT)
                    if (resultData != null) {
                        startScreenRecording(resultCode, resultData)
                    } else {
                        Log.e(TAG, "No result data provided for screen recording")
                        stopSelf()
                    }
                }
            }
            ACTION_STOP_RECORDING -> {
                if (isRecording) {
                    stopScreenRecording()
                }
            }
        }
        return START_STICKY
    }

    private fun getScreenDimensions() {
        try {
            val metrics = DisplayMetrics()
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(metrics)
            screenDensity = metrics.densityDpi
            displayWidth = metrics.widthPixels
            displayHeight = metrics.heightPixels

            // Limit dimensions for performance and compatibility
            if (displayWidth > 1920) displayWidth = 1920
            if (displayHeight > 1080) displayHeight = 1080

            Log.d(TAG, "Screen dimensions: ${displayWidth}x${displayHeight}, density: $screenDensity")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting screen dimensions", e)
            // Fallback to reasonable defaults
            displayWidth = 1080
            displayHeight = 1920
            screenDensity = DisplayMetrics.DENSITY_HIGH
        }
    }

    private fun startScreenRecording(resultCode: Int, resultData: Intent) {
        try {
            Log.d(TAG, "Starting screen recording...")

            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)

            // Set up callback for media projection
            mediaProjection?.registerCallback(mediaProjectionCallback, null)

            setupMediaRecorder()

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "GNUXON_ScreenRecording",
                displayWidth, displayHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface, null, null
            )

            mediaRecorder?.start()
            isRecording = true

            startForeground(NOTIFICATION_ID, buildRecordingNotification())

            Log.d(TAG, "Screen recording started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen recording", e)
            cleanupResources()
            stopSelf()
        }
    }

    private fun setupMediaRecorder() {
        try {
            mediaRecorder = MediaRecorder().apply {
                // Audio configuration
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)

                // Video configuration
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(5000000) // 5 Mbps
                setVideoFrameRate(30)
                setVideoSize(displayWidth, displayHeight)

                // Output configuration
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                val outputFile = getOutputFile()
                setOutputFile(outputFile.absolutePath)

                Log.d(TAG, "Output file: ${outputFile.absolutePath}")

                prepare()
            }
        } catch (e: IOException) {
            Log.e(TAG, "MediaRecorder preparation failed", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder setup failed", e)
            throw e
        }
    }

    private fun getOutputFile(): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(Date())
        val moviesDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "GNUXON")
        }

        moviesDir?.mkdirs()
        return File(moviesDir, "GNUXON-Screen-$timestamp.mp4")
    }

    private fun stopScreenRecording() {
        try {
            Log.d(TAG, "Stopping screen recording...")

            if (isRecording) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping MediaRecorder", e)
                    }
                    release()
                }
                mediaRecorder = null
            }

            cleanupResources()

            stopForeground(true)
            stopSelf()

            Log.d(TAG, "Screen recording stopped successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping screen recording", e)
            cleanupResources()
            stopSelf()
        }
    }

    private fun cleanupResources() {
        isRecording = false

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Log.w(TAG, "MediaProjection was stopped unexpectedly")
            if (isRecording) {
                stopScreenRecording()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen recording for body camera functionality"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildRecordingNotification(): Notification {
        val intent = Intent(this, Camera::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1, stopIntent, // Different request code
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GNUXON - Screen Recording")
            .setContentText("Screen recording active - works when locked")
            .setSmallIcon(R.drawable.ic_recording_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop_recording, "Stop Recording", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopScreenRecording()
        }
    }
}