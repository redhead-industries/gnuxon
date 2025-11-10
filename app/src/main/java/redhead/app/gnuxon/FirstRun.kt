package redhead.app.gnuxon

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import redhead.app.gnuxon.service.GnuxonDeviceAdminReceiver

class FirstRun : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.FOREGROUND_SERVICE_CAMERA,
        Manifest.permission.WAKE_LOCK
    )

    private val storagePermissions = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else emptyArray()

    private val allPermissions = requiredPermissions + storagePermissions

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            startActivity(Intent(this, Camera::class.java))
            finish()
        } else {
            showPermissionExplanation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_run)

        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, GnuxonDeviceAdminReceiver::class.java)

        val grantButton: Button = findViewById(R.id.btn_grant_permissions)
        val legalText: TextView = findViewById(R.id.tv_legal)

        // Show the RedHead + GPLv3 welcome dialog first
        showWelcomeDialog {
            // After user presses “Continue”, proceed normally
            legalText.text = getString(R.string.first_run_legal_text)
            grantButton.setOnClickListener { requestPermissionsAndAdmin() }

            if (hasAllPermissions() && devicePolicyManager.isAdminActive(adminComponent)) {
                startActivity(Intent(this, Camera::class.java))
                finish()
            } else if (hasAllPermissions() && !devicePolicyManager.isAdminActive(adminComponent)) {
                requestDeviceAdmin()
            }
        }
    }

    private fun showWelcomeDialog(onContinue: () -> Unit) {
        val message = """
            Welcome to GNUXON

            GNUXON is a Free and Open-Source bodycam application developed by RedHead (RIIDF Branch), a division of RedHead Industries.

            RedHead Industries was founded in 2015 in Canada with a mission to protect privacy, promote freedom, and provide transparent and ethical technology for everyone.

            GNUXON is licensed under the GNU General Public License v3 (GPLv3).
            You are free to use, modify, and share this software under its terms.

            Product of RedHead Industries — By the people, for the people.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Welcome to GNUXON")
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ -> onContinue() }
            .setCancelable(false)
            .show()
    }

    private fun hasAllPermissions(): Boolean {
        return allPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissionsAndAdmin() {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            requestDeviceAdmin()
        } else {
            requestPermissions()
        }
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "GNUXON requires device administrator privileges to maintain background recording for body camera functionality.")
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    private fun requestPermissions() {
        permissionLauncher.launch(allPermissions)
    }

    private fun showPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("GNUXON requires camera, microphone, and storage permissions to function properly.")
            .setPositiveButton("Grant Again") { _, _ -> requestPermissions() }
            .setNegativeButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setCancelable(false)
            .show()
    }

    private fun showAdminExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Device Admin Required")
            .setMessage("GNUXON requires device administrator privileges to continue recording when locked.")
            .setPositiveButton("Enable Device Admin") { _, _ -> requestDeviceAdmin() }
            .setNegativeButton("Continue Anyway") { _, _ -> requestPermissions() }
            .setCancelable(false)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Device admin enabled", Toast.LENGTH_SHORT).show()
                requestPermissions()
            } else {
                showAdminExplanation()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1001
    }
}
