import os

files = {
    'app/src/main/java/com/feedback/safety/FeedbackApp.kt': '''package com.feedback.safety

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class FeedbackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("safety_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
''',

    'app/src/main/java/com/feedback/safety/manager/UsageAccessManager.kt': '''package com.feedback.safety.manager

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

class UsageAccessManager(private val context: Context) {
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageAccess() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
''',

    'app/src/main/java/com/feedback/safety/manager/InstagramDetectionManager.kt': '''package com.feedback.safety.manager

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
''',

    'app/src/main/java/com/feedback/safety/manager/CaptureStorageManager.kt': '''package com.feedback.safety.manager

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaptureStorageManager(private val context: Context) {
    
    private val rootDir: File by lazy {
        val dir = File(context.filesDir, "SafetyCapture")
        if (!dir.exists()) dir.mkdirs()
        File(dir, ".nomedia").apply { if (!exists()) createNewFile() }
        dir
    }

    fun saveBitmap(bitmap: Bitmap): File? {
        val date = Date()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        val timeStr = SimpleDateFormat("HH-mm-ss", Locale.US).format(date)
        
        val dateDir = File(rootDir, dateStr)
        if (!dateDir.exists()) dateDir.mkdirs()
        
        val file = File(dateDir, "$timeStr.jpg")
        return try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            out.flush()
            out.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getStorageUsedBytes(): Long {
        return calculateDirSize(rootDir)
    }
    
    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) calculateDirSize(f) else f.length()
        }
        return size
    }
    
    fun getScreenshotCount(): Int {
        var count = 0
        val dirs = rootDir.listFiles { f -> f.isDirectory } ?: return 0
        for (d in dirs) {
            count += (d.listFiles { f -> f.extension == "jpg" }?.size ?: 0)
        }
        return count
    }
    
    fun deleteAll() {
        val dirs = rootDir.listFiles { f -> f.isDirectory } ?: return
        for (d in dirs) {
            d.deleteRecursively()
        }
    }
}
''',

    'app/src/main/java/com/feedback/safety/manager/RetentionManager.kt': '''package com.feedback.safety.manager

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
''',

    'app/src/main/java/com/feedback/safety/service/ScreenCaptureService.kt': '''package com.feedback.safety.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.feedback.safety.MainActivity
import com.feedback.safety.R
import com.feedback.safety.manager.CaptureStorageManager
import com.feedback.safety.manager.InstagramDetectionManager
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private lateinit var instagramDetector: InstagramDetectionManager
    private lateinit var storageManager: CaptureStorageManager

    override fun onCreate() {
        super.onCreate()
        instagramDetector = InstagramDetectionManager(this)
        storageManager = CaptureStorageManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)
                if (resultCode != 0 && resultData != null) {
                    startForeground(1, createNotification())
                    startCapture(resultCode, resultData)
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "safety_channel")
            .setContentTitle(getString(R.string.notification_title))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentPendingIntent)
            .addAction(0, getString(R.string.action_stop), stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopSelf()
            }
        }, null)
        
        setupVirtualDisplay()
        startCaptureLoop()
    }

    private fun setupVirtualDisplay() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val density = metrics.densityDpi
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun startCaptureLoop() {
        serviceScope.launch {
            while (isActive) {
                captureFrame()
                delay(2000)
            }
        }
    }

    private fun captureFrame() {
        val image: Image? = imageReader?.acquireLatestImage()
        image?.let {
            val planes = it.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * it.width
            
            val bitmap = Bitmap.createBitmap(
                it.width + rowPadding / pixelStride,
                it.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, it.width, it.height)
            storageManager.saveBitmap(finalBitmap)
            
            it.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
''',

    'app/src/main/java/com/feedback/safety/MainActivity.kt': '''package com.feedback.safety

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.feedback.safety.service.ScreenCaptureService
import com.feedback.safety.ui.MainScreen
import com.feedback.safety.ui.theme.FeedbackTheme

class MainActivity : ComponentActivity() {

    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeedbackTheme {
                MainScreen(
                    onStartCapture = { startCaptureFlow() },
                    onStopCapture = { stopCaptureFlow() }
                )
            }
        }
    }

    private fun startCaptureFlow() {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun stopCaptureFlow() {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        startService(intent)
    }
}
''',

    'app/src/main/java/com/feedback/safety/ui/theme/Theme.kt': '''package com.feedback.safety.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD32F2F),
    secondary = Color(0xFF1976D2),
    background = Color(0xFFF5F5F5),
    surface = Color.White
)

@Composable
fun FeedbackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
''',

    'app/src/main/java/com/feedback/safety/ui/MainScreen.kt': '''package com.feedback.safety.ui

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
'''
}

for filepath, content in files.items():
    os.makedirs(os.path.dirname(filepath) if os.path.dirname(filepath) else '.', exist_ok=True)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
        
print("Code files created.")
