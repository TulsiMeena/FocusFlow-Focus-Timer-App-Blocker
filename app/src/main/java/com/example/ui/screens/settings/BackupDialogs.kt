package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backup.BackupSummary
import com.example.backup.RestoreMode
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RestoreConfirmationDialog(
  summary: BackupSummary,
  onDismiss: () -> Unit,
  onConfirmRestore: (RestoreMode) -> Unit
) {
  val dateFormatted = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    .format(Date(summary.exportTimestamp))

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Backup, contentDescription = null, tint = RadiantViolet, modifier = Modifier.size(36.dp))
    },
    title = {
      Text(
        text = "Restore Focus Backup",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Backup verified successfully. Choose how you want to restore:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Backup Details:",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Exported: $dateFormatted",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• ${summary.sessionsCount} focus session(s)\n• ${summary.tasksCount} planner task(s)\n• ${summary.rewardsCount} reward transaction(s)\n• ${summary.blockedAppsCount} blocked app rule(s)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Info, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
          Text(
            text = "Merge: Adds new records without duplicating existing data.\nReplace: Replaces existing tables with this backup.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = { onConfirmRestore(RestoreMode.MERGE) },
          colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
          modifier = Modifier.testTag("restore_merge_button")
        ) {
          Text("Merge (Safe)")
        }

        OutlinedButton(
          onClick = { onConfirmRestore(RestoreMode.REPLACE) },
          modifier = Modifier.testTag("restore_replace_button")
        ) {
          Text("Replace All")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
