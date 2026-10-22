package com.example.ui.screens.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald
import kotlinx.coroutines.delay

@Composable
fun StrictModeExplanationDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        tint = RadiantViolet,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Enable Strict Focus Mode?",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Strict Mode creates intentional friction to protect your concentration and prevent impulsive session cancellation.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "• Requires an intentional hold confirmation to cancel early.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• Disables easy duration shortening mid-session.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• An Emergency Exit is always available to ensure you are never trapped.",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        Text(
          text = "You can disable Strict Mode anytime when the timer is idle.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
        modifier = Modifier.testTag("confirm_enable_strict_mode")
      ) {
        Text("Enable Strict Mode")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dismiss_enable_strict_mode")
      ) {
        Text("Not Now")
      }
    }
  )
}

@Composable
fun StrictModeCancelDialog(
  onDismiss: () -> Unit,
  onConfirmedCancel: () -> Unit,
  onEmergencyExit: () -> Unit
) {
  var isHolding by remember { mutableStateOf(false) }
  var holdProgress by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(isHolding) {
    if (isHolding) {
      val durationMs = 2000L
      val steps = 20
      val stepTime = durationMs / steps
      for (i in 1..steps) {
        delay(stepTime)
        holdProgress = i.toFloat() / steps.toFloat()
      }
      onConfirmedCancel()
    } else {
      holdProgress = 0f
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.WarningAmber,
        contentDescription = null,
        tint = AmberStreak,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Strict Mode Active",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Strict Mode is enabled to guard against giving up impulsively. Press and hold below for 2 seconds to confirm cancellation, or use Emergency Exit.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Hold to Cancel Button with progress indicator
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            LinearProgressIndicator(
              progress = { holdProgress },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = AmberStreak,
              trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                  detectTapGestures(
                    onPress = {
                      isHolding = true
                      tryAwaitRelease()
                      isHolding = false
                    }
                  )
                }
                .testTag("hold_to_cancel_button"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (isHolding) "Keep holding (${(holdProgress * 100).toInt()}%)..." else "Press & Hold for 2s to Cancel",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isHolding) AmberStreak else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        // Emergency Exit Section
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Emergency,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Urgent situation? Emergency exit bypass is always available.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
              )
            }

            Button(
              onClick = onEmergencyExit,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("emergency_exit_button")
            ) {
              Text(
                text = "Exit Now",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dismiss_strict_cancel")
      ) {
        Text("Continue Focusing")
      }
    }
  )
}

@Composable
fun DistractionInterventionDialog(
  appName: String,
  isStrictMode: Boolean = false,
  onResumeFocus: () -> Unit,
  onRequestCancel: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onResumeFocus,
    icon = {
      Icon(
        imageVector = Icons.Default.Shield,
        contentDescription = null,
        tint = RadiantViolet,
        modifier = Modifier.size(44.dp)
      )
    },
    title = {
      Text(
        text = if (isStrictMode) "Strict Shield Barrier" else "Focus Shield Alert",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = RadiantViolet.copy(alpha = 0.12f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Restricted App Opened",
              style = MaterialTheme.typography.labelMedium,
              color = RadiantViolet,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = appName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Text(
          text = if (isStrictMode) {
            "Strict Mode is active to safeguard your focus flow. Android security prevents silent background app termination, but Advanced Focus provides immediate return redirection to keep your session intact."
          } else {
            "This app was configured for restriction during deep work. Take a deep breath, resist the distraction impulse, and resume your session."
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = SuccessEmerald,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "High-priority notification posted with one-tap return shortcut.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onResumeFocus,
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
        modifier = Modifier.testTag("resume_from_distraction_button")
      ) {
        Text("Return to Focus")
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onRequestCancel,
        modifier = Modifier.testTag("end_session_from_distraction_button")
      ) {
        Text(if (isStrictMode) "Cancel Session" else "End Session")
      }
    }
  )
}
