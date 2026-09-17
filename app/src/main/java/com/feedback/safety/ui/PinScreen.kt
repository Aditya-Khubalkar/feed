package com.feedback.safety.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinScreen(
    mode: PinMode,
    onSuccess: (String) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val title = when (mode) {
        PinMode.CREATE -> if (isConfirming) "Confirm Passcode" else "Create 4-Digit Passcode"
        PinMode.ENTER -> "Enter Passcode"
        PinMode.CHANGE -> "Enter Current Passcode"
    }

    val currentInput = if (isConfirming) confirmPin else pin

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 until 4) {
                val char = currentInput.getOrNull(i)?.toString() ?: "_"
                Text(
                    text = if (char == "_") "_" else "*",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "<")
        )

        for (row in keys) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (key in row) {
                    Button(
                        onClick = {
                            errorMsg = null
                            when (key) {
                                "C" -> {
                                    if (isConfirming) confirmPin = "" else pin = ""
                                }
                                "<" -> {
                                    if (isConfirming && confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                                    else if (!isConfirming && pin.isNotEmpty()) pin = pin.dropLast(1)
                                }
                                else -> {
                                    if (isConfirming && confirmPin.length < 4) {
                                        confirmPin += key
                                        if (confirmPin.length == 4) {
                                            if (confirmPin == pin) {
                                                onSuccess(pin)
                                            } else {
                                                errorMsg = "Pins do not match"
                                                confirmPin = ""
                                                pin = ""
                                                isConfirming = false
                                            }
                                        }
                                    } else if (!isConfirming && pin.length < 4) {
                                        pin += key
                                        if (pin.length == 4) {
                                            if (mode == PinMode.CREATE) {
                                                isConfirming = true
                                            } else {
                                                onSuccess(pin)
                                                coroutineScope.launch {
                                                    delay(500)
                                                    pin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Text(key, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (onCancel != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

enum class PinMode {
    CREATE, ENTER, CHANGE
}
