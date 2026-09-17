package com.feedback.safety.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.feedback.safety.receiver.AdminReceiver

class DeviceManagementManager(private val context: Context) {
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, AdminReceiver::class.java)

    fun isAdminActive(): Boolean {
        return dpm.isAdminActive(adminComponent)
    }

    fun isProfileOwner(): Boolean {
        return dpm.isProfileOwnerApp(context.packageName)
    }

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }
    
    fun setAppHidden(hidden: Boolean): Boolean {
        if (isDeviceOwner() || isProfileOwner()) {
            return dpm.setApplicationHidden(adminComponent, context.packageName, hidden)
        }
        return false
    }

    fun isAppHidden(): Boolean {
        if (isDeviceOwner() || isProfileOwner()) {
            return dpm.isApplicationHidden(adminComponent, context.packageName)
        }
        return false
    }
}
