package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.security.SecurityHelper
import com.example.ui.theme.RadiantViolet

@Composable
fun PinSetupDialog(
  onDismiss: () -> Unit,
  onPinConfigured: (pin: String, recoveryHint: String) -> Unit
) {
  var pin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var recoveryHint by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Lock, contentDescription = null, tint = RadiantViolet, modifier = Modifier.size(36.dp))
    },
    title = {
      Text(
        text = "Set Security PIN",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Protect sensitive settings (Strict Mode, blocked apps, notification preferences) with an in-app PIN.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = pin,
          onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
          label = { Text("4–6 Digit PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("setup_pin_input")
        )

        OutlinedTextField(
          value = confirmPin,
          onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
          label = { Text("Confirm PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("confirm_pin_input")
        )

        OutlinedTextField(
          value = recoveryHint,
          onValueChange = { recoveryHint = it },
          label = { Text("Recovery Hint (e.g., Mom's birth year)") },
          placeholder = { Text("Hint in case you forget") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("recovery_hint_input")
        )

        if (error != null) {
          Text(
            text = error ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (pin.length < 4) {
            error = "PIN must be at least 4 digits."
          } else if (pin != confirmPin) {
            error = "PINs do not match."
          } else if (recoveryHint.isBlank()) {
            error = "Please provide a recovery hint."
          } else {
            onPinConfigured(pin, recoveryHint)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
        modifier = Modifier.testTag("save_pin_button")
      ) {
        Text("Save & Enable")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun PinVerifyDialog(
  expectedHash: String,
  salt: String,
  onDismiss: () -> Unit,
  onVerified: () -> Unit,
  onForgotPinClicked: () -> Unit
) {
  var enteredPin by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Password, contentDescription = null, tint = RadiantViolet, modifier = Modifier.size(36.dp))
    },
    title = {
      Text(
        text = "Security Verification",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Enter your in-app PIN to modify protected settings.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = enteredPin,
          onValueChange = {
            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
              enteredPin = it
              isError = false
            }
          },
          label = { Text("Enter PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          isError = isError,
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("verify_pin_input")
        )

        if (isError) {
          Text(
            text = "Incorrect PIN. Try again or use Recovery Hint.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }

        TextButton(
          onClick = onForgotPinClicked,
          modifier = Modifier.align(Alignment.End)
        ) {
          Text("Forgot PIN / Recovery", style = MaterialTheme.typography.labelSmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (SecurityHelper.verifyPin(enteredPin, salt, expectedHash)) {
            onVerified()
          } else {
            isError = true
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
        modifier = Modifier.testTag("submit_verify_pin_button")
      ) {
        Text("Unlock")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun PinRecoveryDialog(
  recoveryHint: String,
  onDismiss: () -> Unit,
  onPinReset: (newPin: String, newHint: String) -> Unit
) {
  var newPin by remember { mutableStateOf("") }
  var confirmNewPin by remember { mutableStateOf("") }
  var newHint by remember { mutableStateOf(recoveryHint) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Security, contentDescription = null, tint = RadiantViolet, modifier = Modifier.size(36.dp))
    },
    title = {
      Text(
        text = "PIN Recovery & Reset",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Your Recovery Hint:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = recoveryHint.ifBlank { "No hint stored" },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Text(
          text = "Set a new PIN to regain access to protected settings:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = newPin,
          onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
          label = { Text("New 4–6 Digit PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = confirmNewPin,
          onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmNewPin = it },
          label = { Text("Confirm New PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = newHint,
          onValueChange = { newHint = it },
          label = { Text("Updated Recovery Hint") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
          Text(text = error ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (newPin.length < 4) {
            error = "New PIN must be at least 4 digits."
          } else if (newPin != confirmNewPin) {
            error = "PINs do not match."
          } else {
            onPinReset(newPin, newHint)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet)
      ) {
        Text("Reset PIN")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun UninstallSafeguardsDialog(
  onDismiss: () -> Unit,
  onOpenAppInfoSettings: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Info, contentDescription = null, tint = RadiantViolet, modifier = Modifier.size(36.dp))
    },
    title = {
      Text(
        text = "Uninstall Safeguards Transparency",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Android Platform Safeguard Architecture",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Text(
          text = "On standard Android, applications cannot reliably prevent their own uninstallation. Android's operating system deliberately guarantees that device owners can always remove third-party apps through System Settings.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "What Advanced Focus provides:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "• In-app PIN protection to prevent altering focus goals or turning off Strict Mode.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• Strict Mode friction hurdles during focus sessions.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• Transparent design that never misuses Accessibility or Device Admin.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Text(
          text = "Tip: For device-level application locking, use Android's native Parental Controls or Digital Wellbeing settings.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onOpenAppInfoSettings,
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet)
      ) {
        Text("View App Info Settings")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Understood")
      }
    }
  )
}

@Composable
fun DndExplanationDialog(
  onDismiss: () -> Unit,
  onOpenDndSettings: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Shield, contentDescription = null, tint = RadiantViolet, modifier = Modifier.size(36.dp))
    },
    title = {
      Text(
        text = "Do Not Disturb (DND) Integration",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Android OS Native Distraction Silencing",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Text(
          text = "Android Do Not Disturb is the operating system's built-in mechanism to silence notifications, visual banners, and ringing during deep focus sessions.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Why we prefer native DND:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "• Zero privacy intrusion: We do not read or process your messages.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• System-wide suppression across all installed apps.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• Preserves emergency contacts and critical safety calls.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Text(
          text = "Tap below to manage Android DND access permissions in System Settings.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onOpenDndSettings,
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet)
      ) {
        Text("Open DND Settings")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun NotificationListenerExplanationDialog(
  onDismiss: () -> Unit,
  onOpenSystemNotificationSettings: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Info, contentDescription = null, tint = RadiantViolet, modifier = Modifier.size(36.dp))
    },
    title = {
      Text(
        text = "Notification Privacy & Access",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Why We Don't Silently Intercept Messages",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Text(
          text = "Direct notification reading requires Android's sensitive Notification Listener permission, which can read the full text, sender identity, and attachments of every incoming message.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Our Privacy-First Approach:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "• 100% Offline: Zero telemetry, zero cloud data transfer.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• We never inspect your private chat contents or notifications.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• Use native Android Do Not Disturb for system-level quietude.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onOpenSystemNotificationSettings,
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet)
      ) {
        Text("Open App Notification Settings")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Understood")
      }
    }
  )
}
