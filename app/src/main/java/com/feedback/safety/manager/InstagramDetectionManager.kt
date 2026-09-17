package com.feedback.safety.manager

import android.app.usage.UsageStatsManager
import android.content.Context

class InstagramDetectionManager(private val context: Context) {
    fun isInstagramForeground(): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 1000 * 60, time)
        val event = android.app.usage.UsageEvents.Event()
        var currentForeground: String? = null
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentForeground = event.packageName
            } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (event.packageName == currentForeground) {
                    currentForeground = null
                }
            }
        }
        return currentForeground == "com.instagram.android"
    }
}
