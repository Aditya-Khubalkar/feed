package com.feedback.safety.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.feedback.safety.manager.DeviceManagementManager
import com.feedback.safety.manager.SecurityManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit
) {
    val context = LocalContext.current
    val deviceManager = remember { DeviceManagementManager(context) }
    val securityManager = remember { SecurityManager(context) }
    
    var isAdmin by remember { mutableStateOf(deviceManager.isAdminActive()) }
    var isOwner by remember { mutableStateOf(deviceManager.isDeviceOwner() || deviceManager.isProfileOwner()) }
    var isIgnoringBatteryOpt by remember { mutableStateOf(false) }
    
    var showChangePin by remember { mutableStateOf(false) }
    var showNewPin by remember { mutableStateOf(false) }
    var hasPin by remember { mutableStateOf(securityManager.hasPin()) }

    LaunchedEffect(Unit) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        while (true) {
            isAdmin = deviceManager.isAdminActive()
            isOwner = deviceManager.isDeviceOwner() || deviceManager.isProfileOwner()
            isIgnoringBatteryOpt = pm.isIgnoringBatteryOptimizations(context.packageName)
            hasPin = securityManager.hasPin()
            delay(1000)
        }
    }

    if (showChangePin) {
        PinScreen(
            mode = PinMode.CHANGE,
            onSuccess = { pin ->
                if (securityManager.verifyPin(pin)) {
                    showChangePin = false
                    showNewPin = true
                }
            },
            onCancel = { showChangePin = false }
        )
        return
    }

    if (showNewPin) {
        PinScreen(
            mode = PinMode.CREATE,
            onSuccess = { pin ->
                securityManager.setPin(pin)
                showNewPin = false
            },
            onCancel = { showNewPin = false }
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Feedback Personal Safety") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Original Screen Capture Controls
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Screen Capture", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onStartCapture) { Text("START") }
                        Button(onClick = onStopCapture) { Text("STOP") }
                    }
                }
            }

            // Management Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Management", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Device Admin")
                        Text(if (isAdmin) "Enabled" else "Disabled")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Device Owner")
                        Text(if (isOwner) "Enabled" else "Disabled")
                    }
                }
            }

            // Persistence Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Persistence", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Unrestricted battery usage improves persistence but does not guarantee infinite uptime.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Battery optimization")
                        Text(if (isIgnoringBatteryOpt) "Unrestricted" else "Optimized")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Start after reboot")
                        Text("Enabled")
                    }
                }
            }

            // Controls Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Controls", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!isAdmin) {
                        Button(onClick = {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceManager.adminComponent)
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Need admin rights for persistence")
                            }
                            context.startActivity(intent)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Enable Device Admin")
                        }
                    } else {
                        Button(onClick = {
                            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            dpm.removeActiveAdmin(deviceManager.adminComponent)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Disable Device Admin")
                        }
                    }
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Battery Settings")
                    }
                }
            }

            // Security Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Security", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("App Passcode")
                        Text(if (hasPin) "Enabled" else "Disabled")
                    }
                    Button(onClick = { showChangePin = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Change Passcode")
                    }
                    if (hasPin) {
                        Button(onClick = { 
                            securityManager.disablePin()
                            hasPin = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Disable Passcode")
                        }
                    }
                    var lockImmediately by remember { mutableStateOf(securityManager.lockImmediately) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lock immediately")
                        Switch(
                            checked = lockImmediately,
                            onCheckedChange = {
                                lockImmediately = it
                                securityManager.lockImmediately = it
                            }
                        )
                    }
                }
            }
        }
    }
}
