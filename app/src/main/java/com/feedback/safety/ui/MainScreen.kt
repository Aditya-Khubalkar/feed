package com.feedback.safety.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.feedback.safety.manager.CaptureStorageManager
import com.feedback.safety.manager.InstagramDetectionManager
import com.feedback.safety.manager.UsageAccessManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit
) {
    val context = LocalContext.current
    val usageManager = remember { UsageAccessManager(context) }
    val instagramDetector = remember { InstagramDetectionManager(context) }
    val storageManager = remember { CaptureStorageManager(context) }

    var hasUsageAccess by remember { mutableStateOf(usageManager.hasUsageAccess()) }
    var isInstagramForeground by remember { mutableStateOf(false) }
    var screenshotCount by remember { mutableStateOf(0) }
    var storageUsed by remember { mutableStateOf(0L) }
    
    // Auto refresh stats
    LaunchedEffect(Unit) {
        while (true) {
            hasUsageAccess = usageManager.hasUsageAccess()
            if (hasUsageAccess) {
                isInstagramForeground = instagramDetector.isInstagramForeground()
            }
            screenshotCount = storageManager.getScreenshotCount()
            storageUsed = storageManager.getStorageUsedBytes()
            delay(1000)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Feedback Personal Safety") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Usage Access", style = MaterialTheme.typography.titleMedium)
                    Text("Status: ${if (hasUsageAccess) "Enabled" else "Not Enabled"}")
                    if (!hasUsageAccess) {
                        Button(onClick = { usageManager.requestUsageAccess() }) {
                            Text("Grant Usage Access")
                        }
                    }
                }
            }

            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Safety Session", style = MaterialTheme.typography.titleMedium)
                    Text("Instagram: ${if (isInstagramForeground) "OPEN" else "CLOSED"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onStartCapture) { Text("START") }
                        Button(onClick = onStopCapture) { Text("STOP") }
                    }
                }
            }

            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Storage", style = MaterialTheme.typography.titleMedium)
                    Text("Screenshots: $screenshotCount")
                    Text("Storage: ${storageUsed / (1024 * 1024)} MB")
                    Button(onClick = { 
                        storageManager.deleteAll()
                        screenshotCount = storageManager.getScreenshotCount()
                        storageUsed = storageManager.getStorageUsedBytes()
                    }) {
                        Text("DELETE ALL")
                    }
                }
            }
        }
    }
}
