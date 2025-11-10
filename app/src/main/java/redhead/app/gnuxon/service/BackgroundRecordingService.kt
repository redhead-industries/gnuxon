package redhead.app.gnuxon.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import redhead.app.gnuxon.Camera
import redhead.app.gnuxon.R  // Fixed: Added import for R

class BackgroundRecordingService(private val context: Context) {

    companion object {
        private const val TAG = "BackgroundRecordingService"
    }

    private val notificationService = NotificationService(context)
    private var isBackgroundRecording = false

    fun startBackgroundRecording() {
        Log.d(TAG, "Starting background recording service")
        try {
            val intent = Intent(context, BackgroundRecordingForegroundService::class.java).apply {
                action = BackgroundRecordingForegroundService.ACTION_START_RECORDING
            }

            context.startForegroundService(intent)

            isBackgroundRecording = true
            // Also show the regular notification
            showRecordingNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background recording", e)
        }
    }

    fun stopBackgroundRecording() {
        Log.d(TAG, "Stopping background recording service")
        try {
            val intent = Intent(context, BackgroundRecordingForegroundService::class.java).apply {
                action = BackgroundRecordingForegroundService.ACTION_STOP_RECORDING
            }
            context.startService(intent)

            isBackgroundRecording = false
            // Hide the regular notification
            hideRecordingNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop background recording", e)
        }
    }

    // Remove the unused function since we're not using it currently
    // fun isBackgroundRecordingActive(): Boolean {
    //     return isBackgroundRecording
    // }

    fun showRecordingNotification() {
        notificationService.showRecordingNotification()
    }

    fun hideRecordingNotification() {
        notificationService.hideRecordingNotification()
    }

    fun cleanup() {
        if (isBackgroundRecording) {
            stopBackgroundRecording()
        }
        hideRecordingNotification()
    }
}

// Separate notification service for better organization
class NotificationService(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "gnuxon_recording_channel"
        private const val NOTIFICATION_ID = 2 // Different ID from foreground service
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.recording_channel_name),
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.recording_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showRecordingNotification() {
        val intent = Intent(context, Camera::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.recording_notification_title))
            .setContentText(context.getString(R.string.recording_notification_text))
            .setSmallIcon(R.drawable.ic_recording_notification)
            .setContentIntent(pendingIntent) // Fixed: This is correct - called on NotificationCompat.Builder
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun hideRecordingNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}