package com.feedback.safety.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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

    var failedAttempts: Int
        get() = prefs.getInt("failed_attempts", 0)
        private set(value) = prefs.edit().putInt("failed_attempts", value).apply()

    var lockoutEndTime: Long
        get() = prefs.getLong("lockout_end_time", 0L)
        private set(value) = prefs.edit().putLong("lockout_end_time", value).apply()

    fun hasPin(): Boolean {
        return prefs.getString("pin_hash", null) != null
    }

    fun setPin(pin: String) {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        val salt = saltBytes.joinToString("") { "%02x".format(it) }
        
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString("pin_salt", salt)
            .putString("pin_hash", hash)
            .putInt("failed_attempts", 0)
            .putLong("lockout_end_time", 0L)
            .apply()
    }
    
    fun disablePin() {
        prefs.edit().remove("pin_hash").remove("pin_salt").apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (System.currentTimeMillis() < lockoutEndTime) {
            return false // Currently locked out
        }

        val storedHash = prefs.getString("pin_hash", null) ?: return false
        val storedSalt = prefs.getString("pin_salt", null) ?: return false
        
        val isCorrect = hashPin(pin, storedSalt) == storedHash
        
        if (isCorrect) {
            failedAttempts = 0
            lockoutEndTime = 0
            return true
        } else {
            failedAttempts++
            if (failedAttempts >= 5) {
                lockoutEndTime = System.currentTimeMillis() + 30000 // 30 seconds lockout
                failedAttempts = 0
            }
            return false
        }
    }
    
    fun getLockoutRemainingSeconds(): Int {
        val remaining = (lockoutEndTime - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining.toInt() else 0
    }

    private fun hashPin(pin: String, saltStr: String): String {
        val salt = saltStr.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(pin.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    fun shouldLock(): Boolean {
        if (!hasPin()) return false
        if (lockImmediately) return true
        val inactiveTime = System.currentTimeMillis() - lastActiveTime
        return inactiveTime > 30000 // 30 seconds timeout
    }
}
