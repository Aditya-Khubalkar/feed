package com.feedback.safety.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        prefs = EncryptedSharedPreferences.create(
            context,
            "security_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var lockImmediately: Boolean
        get() = prefs.getBoolean("lock_immediately", false)
        set(value) = prefs.edit().putBoolean("lock_immediately", value).apply()

    var lastActiveTime: Long
        get() = prefs.getLong("last_active_time", 0L)
        set(value) = prefs.edit().putLong("last_active_time", value).apply()

    fun hasPin(): Boolean {
        return prefs.getString("pin_hash", null) != null
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString("pin_hash", hash).apply()
    }
    
    fun disablePin() {
        prefs.edit().remove("pin_hash").apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", null) ?: return false
        return hashPin(pin) == storedHash
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    fun shouldLock(): Boolean {
        if (!hasPin()) return false
        if (lockImmediately) return true
        val inactiveTime = System.currentTimeMillis() - lastActiveTime
        return inactiveTime > 30000 // 30 seconds timeout
    }
}
