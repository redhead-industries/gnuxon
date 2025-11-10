package redhead.app.gnuxon.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class GnuxonDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {}
    override fun onDisabled(context: Context, intent: Intent) {}
}
