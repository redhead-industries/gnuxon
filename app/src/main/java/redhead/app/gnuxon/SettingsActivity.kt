package redhead.app.gnuxon

import android.content.Context
import android.os.Bundle
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }


    class SettingsFragment : PreferenceFragmentCompat() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Setup resolution preference with detected values
            setupResolutionPreference()

            // Setup custom bitrate validation
            setupCustomBitrateValidation()

            // Setup preference change listeners for storage estimate
            setupStorageEstimate()

            // Setup custom bitrate visibility based on quality selection
            setupCustomBitrateVisibility()
        }

        private fun setupResolutionPreference() {
            val resolutionPref = findPreference<ListPreference>("resolution")

            scope.launch {
                val resolutions = detectAvailableResolutions(requireContext())

                resolutionPref?.entries = resolutions.map { it.toString() }.toTypedArray()
                resolutionPref?.entryValues = resolutions.map { "${it.width}x${it.height}" }.toTypedArray()

                // Set current value or default
                val currentResolution = RecordingPreferences.getResolution(requireContext())
                if (resolutions.any { "${it.width}x${it.height}" == currentResolution }) {
                    resolutionPref?.value = currentResolution
                } else {
                    // Set to first available resolution
                    resolutions.firstOrNull()?.let {
                        resolutionPref?.value = "${it.width}x${it.height}"
                        RecordingPreferences.setResolution(requireContext(), "${it.width}x${it.height}")
                    }
                }

                resolutionPref?.setOnPreferenceChangeListener { _, newValue ->
                    RecordingPreferences.setResolution(requireContext(), newValue.toString())
                    updateStorageEstimate()
                    true
                }
            }
        }

        private fun detectAvailableResolutions(context: Context): List<Size> {
            val resolutions = mutableSetOf<Size>()

            try {
                // Get screen resolution for screen recording
                val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
                val windowMetrics = windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                val screenWidth = bounds.width()
                val screenHeight = bounds.height()

                // Add screen resolution
                resolutions.add(Size(screenWidth, screenHeight))

                // Add common resolutions that fit within screen dimensions
                val commonResolutions = listOf(
                    Size(3840, 2160), // 4K
                    Size(2560, 1440), // 1440p
                    Size(1920, 1080), // 1080p
                    Size(1280, 720),  // 720p
                    Size(854, 480)    // 480p
                )

                // Add resolutions that fit within screen dimensions
                commonResolutions.forEach { size ->
                    if (size.width <= screenWidth && size.height <= screenHeight) {
                        resolutions.add(size)
                    }
                }

                // Try to get CameraX supported resolutions
                try {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    val cameraProvider = cameraProviderFuture.get()

                    // Get available camera infos
                    cameraProvider.availableCameraInfos.forEach { _ ->
                        // CameraX will handle resolution selection internally
                        // We just add common video recording resolutions
                    }
                } catch (_: Exception) {
                    // CameraX not available or failed, use common resolutions only
                }

            } catch (_: Exception) {
                // Fallback to common resolutions
                resolutions.addAll(listOf(
                    Size(1920, 1080),
                    Size(1280, 720),
                    Size(854, 480)
                ))
            }

            // Sort by total pixels (descending)
            return resolutions.sortedByDescending { it.width * it.height }
        }

        private fun setupCustomBitrateValidation() {
            val customBitratePref = findPreference<EditTextPreference>("custom_bitrate")

            customBitratePref?.setOnPreferenceChangeListener { _, newValue ->
                val bitrateText = newValue.toString()
                val bitrate = bitrateText.toIntOrNull()

                if (bitrate == null) {
                    Toast.makeText(requireContext(), R.string.invalid_bitrate, Toast.LENGTH_SHORT).show()
                    false
                } else {
                    // Convert Mbps to kbps
                    val bitrateKbps = bitrate * 1000

                    if (RecordingPreferences.validateBitrate(bitrateKbps)) {
                        RecordingPreferences.setCustomBitrate(requireContext(), bitrateKbps)
                        updateStorageEstimate()
                        true
                    } else {
                        Toast.makeText(requireContext(), R.string.bitrate_validation_error, Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            }

            // Set current value in Mbps
            val currentBitrateKbps = RecordingPreferences.getCustomBitrate(requireContext())
            customBitratePref?.text = (currentBitrateKbps / 1000).toString()
        }

        private fun setupCustomBitrateVisibility() {
            val qualityPref = findPreference<ListPreference>("video_quality")
            val customBitratePref = findPreference<EditTextPreference>("custom_bitrate")

            // Update visibility based on current selection
            val currentQuality = qualityPref?.value
            customBitratePref?.isVisible = currentQuality == "custom"

            qualityPref?.setOnPreferenceChangeListener { _, newValue ->
                val quality = newValue.toString()
                customBitratePref?.isVisible = quality == "custom"

                // Update RecordingPreferences
                try {
                    RecordingPreferences.setVideoQuality(
                        requireContext(),
                        RecordingPreferences.VideoQuality.valueOf(quality.uppercase())
                    )
                } catch (_: Exception) {
                    // Invalid quality value
                }

                updateStorageEstimate()
                true
            }
        }

        private fun setupStorageEstimate() {
            // Update estimate initially
            updateStorageEstimate()

            // Add listeners to all relevant preferences
            findPreference<ListPreference>("video_quality")?.setOnPreferenceChangeListener { _, _ ->
                updateStorageEstimate()
                true
            }
            findPreference<ListPreference>("frame_rate")?.setOnPreferenceChangeListener { _, newValue ->
                RecordingPreferences.setFrameRate(requireContext(), newValue.toString().toInt())
                updateStorageEstimate()
                true
            }
            findPreference<ListPreference>("audio_bitrate")?.setOnPreferenceChangeListener { _, newValue ->
                RecordingPreferences.setAudioBitrate(requireContext(), newValue.toString().toInt())
                updateStorageEstimate()
                true
            }
            findPreference<ListPreference>("audio_sample_rate")?.setOnPreferenceChangeListener { _, newValue ->
                RecordingPreferences.setAudioSampleRate(requireContext(), newValue.toString().toInt())
                updateStorageEstimate()
                true
            }
        }

        private fun updateStorageEstimate() {
            val storageEstimatePref = findPreference<Preference>("storage_estimate")
            val estimatedSize = RecordingPreferences.getEstimatedFileSizePerMinute(requireContext())
            storageEstimatePref?.summary = getString(R.string.storage_estimate_summary, estimatedSize)
        }
    }
}
