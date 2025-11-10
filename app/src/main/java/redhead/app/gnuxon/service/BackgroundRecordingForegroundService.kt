package redhead.app.gnuxon.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.Display
import androidx.core.app.NotificationCompat
import redhead.app.gnuxon.Camera
import redhead.app.gnuxon.R

class BackgroundRecordingForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "gnuxon_foreground_recording_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_RECORDING = "redhead.app.gnuxon.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "redhead.app.gnuxon.action.STOP_RECORDING"
    }

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var displayManager: DisplayManager
    private var isRecording = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                val display = displayManager.getDisplay(displayId)
                if (display.state == Display.STATE_OFF) {
                    Log.d("BackgroundRecording", "Screen turned off, but recording continues")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        setupDisplayListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                if (!isRecording) {
                    startForegroundRecording()
                    isRecording = true
                }
            }
            ACTION_STOP_RECORDING -> {
                if (isRecording) {
                    stopForegroundRecording()
                    isRecording = false
                }
            }
        }
        return START_STICKY // Important: Service will be restarted if killed
    }

    private fun startForegroundRecording() {
        val notification = buildRecordingNotification()

        // Use high priority for the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        Log.d("BackgroundRecording", "Foreground service started with wake lock")
    }

    private fun stopForegroundRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        releaseWakeLock()
        stopSelf()
        Log.d("BackgroundRecording", "Foreground service stopped")
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "GNUXON::BackgroundRecordingWakeLock"
        )
        wakeLock.setReferenceCounted(false)
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/) // Timeout for safety
        Log.d("BackgroundRecording", "Wake lock acquired")
    }

    private fun releaseWakeLock() {
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
            Log.d("BackgroundRecording", "Wake lock released")
        }
    }

    private fun setupDisplayListener() {
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.foreground_recording_channel_name),
                NotificationManager.IMPORTANCE_HIGH // Changed to HIGH priority
            ).apply {
                description = getString(R.string.foreground_recording_channel_description)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // Prevent dismissal
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isBlockable = false
                }
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

        val stopIntent = Intent(this, BackgroundRecordingForegroundService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1, stopIntent, // Different request code
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_recording_notification_title))
            .setContentText(getString(R.string.foreground_recording_notification_text))
            .setSmallIcon(R.drawable.ic_recording_notification)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stop_recording,
                "Stop Recording",
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Highest priority
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        displayManager.unregisterDisplayListener(displayListener)
        Log.d("BackgroundRecording", "Service destroyed")
    }
}