package redhead.app.gnuxon.service

import android.app.*
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import redhead.app.gnuxon.Camera
import redhead.app.gnuxon.R
import redhead.app.gnuxon.RecordingPreferences
import redhead.app.gnuxon.VideoHashManager
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
    private var currentOutputFile: File? = null

    // Structured coroutine scope for the service
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
        getScreenDimensions()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                if (!isRecording) {
                    val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                    val resultData = intent.getParcelableExtra(EXTRA_RESULT_INTENT, Intent::class.java)
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
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds

            displayWidth = bounds.width()
            displayHeight = bounds.height()
            screenDensity = resources.displayMetrics.densityDpi

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
            // Get user preferences
            val videoBitrate = RecordingPreferences.getVideoBitrate(this) * 1000 // Convert kbps to bps
            val audioBitrate = RecordingPreferences.getAudioBitrate(this) * 1000 // Convert kbps to bps
            val audioSampleRate = RecordingPreferences.getAudioSampleRate(this)
            val frameRate = RecordingPreferences.getFrameRate(this)
            val width = RecordingPreferences.getResolutionWidth(this)
            val height = RecordingPreferences.getResolutionHeight(this)

            // Constrain resolution to screen dimensions
            val finalWidth = minOf(width, displayWidth)
            val finalHeight = minOf(height, displayHeight)

            mediaRecorder = MediaRecorder(this).apply {
                // Audio configuration
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(audioSampleRate)
                setAudioEncodingBitRate(audioBitrate)

                // Video configuration
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(videoBitrate)
                setVideoFrameRate(frameRate)
                setVideoSize(finalWidth, finalHeight)

                // Output configuration
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                val outputFile = getOutputFile()
                currentOutputFile = outputFile
                setOutputFile(outputFile.absolutePath)

                Log.d(TAG, "Output file: ${outputFile.absolutePath}")
                Log.d(TAG, "Recording settings - Resolution: ${finalWidth}x${finalHeight}, " +
                        "Video Bitrate: ${videoBitrate/1000}kbps, Frame Rate: ${frameRate}fps, " +
                        "Audio Bitrate: ${audioBitrate/1000}kbps, Sample Rate: ${audioSampleRate}Hz")

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
        val moviesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)

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

                // Generate MD5 hash for the recorded video
                currentOutputFile?.let { file ->
                    if (file.exists()) {
                        serviceScope.launch {
                            VideoHashManager.computeAndSaveHash(this@ScreenRecordingService, file)
                            Log.d(TAG, "MD5 hash generated for ${file.name}")
                        }
                    }
                }
            }

            cleanupResources()

            stopForeground(STOP_FOREGROUND_REMOVE)
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Screen recording for body camera functionality"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
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
        // Cancel coroutines when service is destroyed
        serviceScope.cancel()
    }
}