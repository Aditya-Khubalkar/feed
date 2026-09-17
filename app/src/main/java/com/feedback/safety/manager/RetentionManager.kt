package com.feedback.safety.manager

import android.content.Context
import java.io.File

class RetentionManager(private val context: Context) {
    fun cleanupOldCaptures(retentionHours: Long) {
        val rootDir = File(context.filesDir, "SafetyCapture")
        if (!rootDir.exists()) return
        
        val dirs = rootDir.listFiles { f -> f.isDirectory } ?: return
        val thresholdTime = System.currentTimeMillis() - (retentionHours * 60 * 60 * 1000)
        
        for (d in dirs) {
            val files = d.listFiles { f -> f.extension == "jpg" } ?: continue
            var allDeleted = true
            for (f in files) {
                if (f.lastModified() < thresholdTime) {
                    f.delete()
                } else {
                    allDeleted = false
                }
            }
            if (allDeleted) {
                d.delete()
            }
        }
    }
}
