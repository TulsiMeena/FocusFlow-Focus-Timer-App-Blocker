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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.ScheduledFocusEntity
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet

@Composable
fun ScheduledFocusDialog(
  scheduledBlocks: List<ScheduledFocusEntity>,
  onAddBlock: (title: String, start: String, end: String, days: String) -> Unit,
  onToggleBlock: (Long, Boolean) -> Unit,
  onDeleteBlock: (ScheduledFocusEntity) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddForm by remember { mutableStateOf(false) }
  var newTitle by remember { mutableStateOf("Morning Focus") }
  var newStart by remember { mutableStateOf("09:00") }
  var newEnd by remember { mutableStateOf("10:30") }
  var newDays by remember { mutableStateOf("Mon, Tue, Wed, Thu, Fri") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = modifier
        .fillMaxWidth()
        .testTag("dialog_scheduled_focus"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = ElectricBlue.copy(alpha = 0.15f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Schedule,
                  contentDescription = null,
                  tint = ElectricBlue,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Text(
              text = "Scheduled Focus",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Text(
          text = "Set daily recurring time windows for automatic focus discipline and reminder shield.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showAddForm) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text("New Scheduled Block", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Routine Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = newStart,
                  onValueChange = { newStart = it },
                  label = { Text("Start (e.g. 09:00)") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                  value = newEnd,
                  onValueChange = { newEnd = it },
                  label = { Text("End (e.g. 10:30)") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
              }
              OutlinedTextField(
                value = newDays,
                onValueChange = { newDays = it },
                label = { Text("Days (e.g. Mon-Fri)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                Button(
                  onClick = {
                    if (newTitle.isNotBlank()) {
                      onAddBlock(newTitle, newStart, newEnd, newDays)
                      showAddForm = false
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet)
                ) {
                  Text("Save Routine")
                }
              }
            }
          }
        } else {
          Button(
            onClick = { showAddForm = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Add Focus Routine", fontWeight = FontWeight.Bold)
          }
        }

        // List of blocks
        if (scheduledBlocks.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(100.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No scheduled focus routines configured yet.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            scheduledBlocks.forEach { block ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = block.title,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "${block.startTime} - ${block.endTime} • ${block.daysOfWeek}",
                      style = MaterialTheme.typography.bodySmall,
                      color = ElectricBlue,
                      fontSize = 12.sp
                    )
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                      checked = block.isEnabled,
                      onCheckedChange = { onToggleBlock(block.id, it) },
                      colors = SwitchDefaults.colors(checkedThumbColor = RadiantViolet, checkedTrackColor = RadiantViolet.copy(alpha = 0.5f))
                    )
                    IconButton(onClick = { onDeleteBlock(block) }) {
                      Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
