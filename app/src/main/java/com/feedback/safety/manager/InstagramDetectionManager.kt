package com.feedback.safety.manager

import android.app.usage.UsageStatsManager
import android.content.Context

class InstagramDetectionManager(private val context: Context) {
    fun isInstagramForeground(): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 10, // Check last 10 seconds
            time
        )
        
        var foregroundApp: String? = null
        var lastTimeUsed = 0L
        
        if (stats != null) {
            for (usageStats in stats) {
                if (usageStats.lastTimeUsed > lastTimeUsed) {
                    foregroundApp = usageStats.packageName
                    lastTimeUsed = usageStats.lastTimeUsed
                }
            }
        }
        return foregroundApp == "com.instagram.android"
    }
}
