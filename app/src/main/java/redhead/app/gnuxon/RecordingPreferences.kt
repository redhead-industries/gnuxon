package redhead.app.gnuxon

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Helper class for managing recording preferences
 * Provides unified settings for both camera and screen recording
 */
object RecordingPreferences {
    private const val PREFS_NAME = "gnuxon_prefs"

    // Preference keys
    private const val KEY_VIDEO_QUALITY = "video_quality"
    private const val KEY_CUSTOM_BITRATE = "custom_bitrate"
    private const val KEY_RESOLUTION = "resolution"
    private const val KEY_FRAME_RATE = "frame_rate"
    private const val KEY_AUDIO_BITRATE = "audio_bitrate"
    private const val KEY_AUDIO_SAMPLE_RATE = "audio_sample_rate"

    // Default values
    private const val DEFAULT_VIDEO_QUALITY = "medium"
    private const val DEFAULT_CUSTOM_BITRATE = 8000 // 8 Mbps in kbps
    private const val DEFAULT_RESOLUTION = "1920x1080"
    private const val DEFAULT_FRAME_RATE = 30
    private const val DEFAULT_AUDIO_BITRATE = 128 // kbps
    private const val DEFAULT_AUDIO_SAMPLE_RATE = 44100 // Hz

    // Quality presets (in kbps)
    enum class VideoQuality(val bitrate: Int) {
        LOW(2000),      // 2 Mbps
        MEDIUM(5000),   // 5 Mbps
        HIGH(10000),    // 10 Mbps
        ULTRA(15000),   // 15 Mbps
        CUSTOM(-1)      // User-defined
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Video Quality
    fun getVideoQuality(context: Context): VideoQuality {
        val value = getPrefs(context).getString(KEY_VIDEO_QUALITY, DEFAULT_VIDEO_QUALITY)
        return try {
            VideoQuality.valueOf(value!!.uppercase())
        } catch (_: Exception) {
            VideoQuality.MEDIUM
        }
    }

    fun setVideoQuality(context: Context, quality: VideoQuality) {
        getPrefs(context).edit {
            putString(KEY_VIDEO_QUALITY, quality.name.lowercase())
        }
    }

    // Custom Bitrate (in kbps)
    fun getCustomBitrate(context: Context): Int {
        return getPrefs(context).getInt(KEY_CUSTOM_BITRATE, DEFAULT_CUSTOM_BITRATE)
    }

    fun setCustomBitrate(context: Context, bitrate: Int) {
        if (validateBitrate(bitrate)) {
            getPrefs(context).edit {
                putInt(KEY_CUSTOM_BITRATE, bitrate)
            }
        }
    }

    // Get effective bitrate based on quality setting
    fun getVideoBitrate(context: Context): Int {
        val quality = getVideoQuality(context)
        return if (quality == VideoQuality.CUSTOM) {
            getCustomBitrate(context)
        } else {
            quality.bitrate
        }
    }

    // Resolution
    fun getResolution(context: Context): String {
        return getPrefs(context).getString(KEY_RESOLUTION, DEFAULT_RESOLUTION) ?: DEFAULT_RESOLUTION
    }

    fun setResolution(context: Context, resolution: String) {
        getPrefs(context).edit {
            putString(KEY_RESOLUTION, resolution)
        }
    }

    fun getResolutionWidth(context: Context): Int {
        return getResolution(context).split("x")[0].toIntOrNull() ?: 1920
    }

    fun getResolutionHeight(context: Context): Int {
        return getResolution(context).split("x")[1].toIntOrNull() ?: 1080
    }

    // Frame Rate
    fun getFrameRate(context: Context): Int {
        return getPrefs(context).getInt(KEY_FRAME_RATE, DEFAULT_FRAME_RATE)
    }

    fun setFrameRate(context: Context, frameRate: Int) {
        getPrefs(context).edit {
            putInt(KEY_FRAME_RATE, frameRate)
        }
    }

    // Audio Bitrate (in kbps)
    fun getAudioBitrate(context: Context): Int {
        return getPrefs(context).getInt(KEY_AUDIO_BITRATE, DEFAULT_AUDIO_BITRATE)
    }

    fun setAudioBitrate(context: Context, bitrate: Int) {
        getPrefs(context).edit {
            putInt(KEY_AUDIO_BITRATE, bitrate)
        }
    }

    // Audio Sample Rate (in Hz)
    fun getAudioSampleRate(context: Context): Int {
        return getPrefs(context).getInt(KEY_AUDIO_SAMPLE_RATE, DEFAULT_AUDIO_SAMPLE_RATE)
    }

    fun setAudioSampleRate(context: Context, sampleRate: Int) {
        getPrefs(context).edit {
            putInt(KEY_AUDIO_SAMPLE_RATE, sampleRate)
        }
    }

    // Validation
    fun validateBitrate(bitrate: Int): Boolean {
        return bitrate in 1000..50000 // 1-50 Mbps in kbps
    }

    /**
     * Calculate estimated file size per minute in MB
     * Formula: (video_bitrate + audio_bitrate) * 60 seconds / 8 bits per byte / 1024 KB per MB
     */
    fun getEstimatedFileSizePerMinute(context: Context): Int {
        val videoBitrate = getVideoBitrate(context) // kbps
        val audioBitrate = getAudioBitrate(context) // kbps
        val totalBitrate = videoBitrate + audioBitrate

        // Convert kbps to MB per minute
        // kbps * 60 seconds / 8 / 1024 = MB per minute
        return (totalBitrate * 60) / (8 * 1024)
    }
}
