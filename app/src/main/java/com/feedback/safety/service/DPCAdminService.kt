package com.feedback.safety.service

import android.app.admin.DeviceAdminService
import android.util.Log

/**
 * Android system binds to this service to keep the Device Policy Controller (DPC) process alive.
 * This is the official, supported mechanism for device owner persistence.
 */
class DPCAdminService : DeviceAdminService() {
    override fun onCreate() {
        super.onCreate()
        Log.d("DPCAdminService", "DeviceAdminService created - system is maintaining DPC process")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DPCAdminService", "DeviceAdminService destroyed")
    }
}
