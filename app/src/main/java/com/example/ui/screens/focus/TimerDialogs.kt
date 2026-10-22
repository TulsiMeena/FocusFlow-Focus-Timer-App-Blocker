package com.example.ui.screens.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SessionRecord
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

@Composable
fun CustomDurationDialog(
  initialMinutes: Int,
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit
) {
  var selectedMinutes by remember { mutableFloatStateOf(initialMinutes.coerceIn(1, 120).toFloat()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Custom Duration",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "${selectedMinutes.toInt()} minutes",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilledTonalIconButton(
            onClick = {
              if (selectedMinutes > 1) selectedMinutes -= 1
            },
            modifier = Modifier.testTag("decrease_custom_duration")
          ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
          }

          Slider(
            value = selectedMinutes,
            onValueChange = { selectedMinutes = it },
            valueRange = 1f..120f,
            steps = 118,
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 8.dp)
              .testTag("custom_duration_slider"),
            colors = SliderDefaults.colors(
              thumbColor = RadiantViolet,
              activeTrackColor = RadiantViolet
            )
          )

          FilledTonalIconButton(
            onClick = {
              if (selectedMinutes < 120) selectedMinutes += 1
            },
            modifier = Modifier.testTag("increase_custom_duration")
          ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
          }
        }

        Text(
          text = "Select any duration between 1 and 120 minutes.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedMinutes.toInt()) },
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
        modifier = Modifier.testTag("confirm_custom_duration")
      ) {
        Text("Set Duration")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_custom_duration")
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun CancelConfirmationDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.WarningAmber,
        contentDescription = null,
        tint = AmberStreak,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Cancel Active Session?",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Text(
        text = "This will halt your current focus countdown. The elapsed portion of this session will be logged as interrupted in your history.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError
        ),
        modifier = Modifier.testTag("confirm_cancel_button")
      ) {
        Text("Yes, Cancel Session")
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dismiss_cancel_button")
      ) {
        Text("Keep Focusing")
      }
    }
  )
}

@Composable
fun SessionCompletedDialog(
  session: SessionRecord?,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = SuccessEmerald,
        modifier = Modifier.size(40.dp)
      )
    },
    title = {
      Text(
        text = "Session Completed! 🎉",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val minutes = (session?.actualDurationSeconds ?: 0) / 60
        Text(
          text = "$minutes Minutes Completed",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Outstanding deep work! This session has been saved permanently to your local history.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
        modifier = Modifier.testTag("dismiss_completed_dialog")
      ) {
        Text("Done")
      }
    }
  )
}
