package com.example.ui.screens.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SessionRecord
import com.example.timer.TimerMode
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

@Composable
fun SessionHistorySection(
  sessions: List<SessionRecord>,
  onDeleteSession: (Long) -> Unit,
  onClearAllHistory: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showClearConfirmDialog by remember { mutableStateOf(false) }

  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text("Clear All Session History?") },
      text = { Text("This will permanently remove all saved session records from your local storage.") },
      confirmButton = {
        Button(
          onClick = {
            onClearAllHistory()
            showClearConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.History,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "Session History",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Text(
            text = "${sessions.size}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      if (sessions.isNotEmpty()) {
        TextButton(
          onClick = { showClearConfirmDialog = true },
          modifier = Modifier.testTag("clear_history_button")
        ) {
          Text(
            text = "Clear All",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    }

    // List or Empty State
    if (sessions.isEmpty()) {
      EmptyHistoryCard()
    } else {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        sessions.forEach { session ->
          SessionItemCard(
            session = session,
            onDelete = { onDeleteSession(session.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun SessionItemCard(
  session: SessionRecord,
  onDelete: () -> Unit
) {
  val isCompleted = session.status == "COMPLETED"
  val isFocus = session.sessionType == TimerMode.FOCUS.name
  val minutes = session.actualDurationSeconds / 60
  val seconds = session.actualDurationSeconds % 60
  val durationLabel = if (minutes > 0) "${minutes}m ${if (seconds > 0) "${seconds}s" else ""}" else "${seconds}s"

  val modeName = when (session.sessionType) {
    TimerMode.FOCUS.name -> "Deep Focus"
    TimerMode.SHORT_BREAK.name -> "Short Break"
    TimerMode.LONG_BREAK.name -> "Long Break"
    else -> session.sessionType
  }

  val icon = if (isFocus) Icons.Default.HourglassBottom else Icons.Default.Coffee
  val iconTint = if (isFocus) RadiantViolet else ElectricBlue

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("session_item_${session.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier.size(38.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = iconTint,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = modeName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )

            // Status Pill
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isCompleted) SuccessEmerald.copy(alpha = 0.15f) else AmberStreak.copy(alpha = 0.15f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Close,
                  contentDescription = null,
                  tint = if (isCompleted) SuccessEmerald else AmberStreak,
                  modifier = Modifier.size(10.dp)
                )
                Text(
                  text = if (isCompleted) "Completed" else "Cancelled",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isCompleted) SuccessEmerald else AmberStreak
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "${session.dateFormatted} • $durationLabel logged",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.size(32.dp).testTag("delete_session_${session.id}")
      ) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Delete record",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
private fun EmptyHistoryCard() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("empty_history_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = Icons.Default.History,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(36.dp)
      )
      Text(
        text = "No Sessions Recorded Yet",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Start and complete a focus session above to record your deep work history locally.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
