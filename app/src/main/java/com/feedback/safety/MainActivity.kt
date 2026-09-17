package com.feedback.safety

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.feedback.safety.manager.SecurityManager
import com.feedback.safety.service.ScreenCaptureService
import com.feedback.safety.ui.MainScreen
import com.feedback.safety.ui.PinMode
import com.feedback.safety.ui.PinScreen
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
                val securityManager = remember { SecurityManager(this) }
                var isUnlocked by remember { mutableStateOf(!securityManager.shouldLock()) }
                var showCreatePin by remember { mutableStateOf(!securityManager.hasPin()) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            if (securityManager.shouldLock()) {
                                isUnlocked = false
                            }
                        } else if (event == Lifecycle.Event.ON_PAUSE) {
                            securityManager.lastActiveTime = System.currentTimeMillis()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (showCreatePin) {
                    PinScreen(
                        mode = PinMode.CREATE,
                        onSuccess = { pin ->
                            securityManager.setPin(pin)
                            showCreatePin = false
                            isUnlocked = true
                        }
                    )
                } else if (!isUnlocked) {
                    PinScreen(
                        mode = PinMode.ENTER,
                        onSuccess = { pin ->
                            if (securityManager.verifyPin(pin)) {
                                isUnlocked = true
                                securityManager.lockImmediately = false
                                securityManager.lastActiveTime = System.currentTimeMillis()
                            }
                        }
                    )
                } else {
                    MainScreen(
                        onStartCapture = { startCaptureFlow() },
                        onStopCapture = { stopCaptureFlow() }
                    )
                }
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
