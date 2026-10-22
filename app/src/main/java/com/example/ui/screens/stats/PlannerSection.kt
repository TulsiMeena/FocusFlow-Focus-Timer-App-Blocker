package com.example.ui.screens.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TaskEntity
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

@Composable
fun PlannerSection(
  todayTasks: List<TaskEntity>,
  allTasks: List<TaskEntity>,
  onToggleTask: (TaskEntity) -> Unit,
  onDeleteTask: (Long) -> Unit,
  onSaveTask: (title: String, notes: String, targetDate: String, plannedDuration: Int) -> Unit,
  onUpdateTask: (TaskEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
  var viewAllTasks by remember { mutableStateOf(false) }

  val displayedTasks = if (viewAllTasks) allTasks else todayTasks
  val completedCount = todayTasks.count { it.isCompleted }
  val totalCount = todayTasks.size
  val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

  if (showAddDialog) {
    TaskDialog(
      existingTask = null,
      onDismiss = { showAddDialog = false },
      onConfirm = { title, notes, date, duration ->
        onSaveTask(title, notes, date, duration)
        showAddDialog = false
      }
    )
  }

  if (taskToEdit != null) {
    TaskDialog(
      existingTask = taskToEdit,
      onDismiss = { taskToEdit = null },
      onConfirm = { title, notes, date, duration ->
        val updated = taskToEdit!!.copy(
          title = title,
          notes = notes,
          targetDate = date,
          plannedDurationMinutes = duration
        )
        onUpdateTask(updated)
        taskToEdit = null
      }
    )
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header & Today Progress
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("planner_progress_card"),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Daily Focus Planner",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "$completedCount of $totalCount today's priorities completed",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("add_task_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Task", style = MaterialTheme.typography.labelMedium)
          }
        }

        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
          color = SuccessEmerald,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
      }
    }

    // Filter Switcher
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (viewAllTasks) "All Planned Tasks (${allTasks.size})" else "Today's Priorities (${todayTasks.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = !viewAllTasks,
          onClick = { viewAllTasks = false },
          label = { Text("Today") },
          modifier = Modifier.testTag("filter_today_tasks")
        )
        FilterChip(
          selected = viewAllTasks,
          onClick = { viewAllTasks = true },
          label = { Text("All") },
          modifier = Modifier.testTag("filter_all_tasks")
        )
      }
    }

    if (displayedTasks.isEmpty()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("empty_planner_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.TaskAlt,
            contentDescription = null,
            tint = RadiantViolet,
            modifier = Modifier.size(40.dp)
          )
          Text(
            text = if (viewAllTasks) "No tasks created yet" else "No tasks scheduled for today",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Create focused tasks with target durations to structure your deep work sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Create Your First Task")
          }
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        displayedTasks.forEach { task ->
          TaskItemCard(
            task = task,
            onToggle = { onToggleTask(task) },
            onEdit = { taskToEdit = task },
            onDelete = { onDeleteTask(task.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun TaskItemCard(
  task: TaskEntity,
  onToggle: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onToggle,
        modifier = Modifier.testTag("toggle_task_${task.id}")
      ) {
        Icon(
          imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
          contentDescription = if (task.isCompleted) "Mark incomplete" else "Mark complete",
          tint = if (task.isCompleted) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(24.dp)
        )
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 8.dp)
      ) {
        Text(
          text = task.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )

        if (task.notes.isNotBlank()) {
          Text(
            text = task.notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
          ) {
            Text(
              text = "${task.plannedDurationMinutes}m plan",
              style = MaterialTheme.typography.labelSmall,
              color = RadiantViolet,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Text(
            text = "Due ${task.targetDate}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row {
        IconButton(onClick = onEdit) {
          Icon(Icons.Default.Edit, contentDescription = "Edit Task", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete) {
          Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun TaskDialog(
  existingTask: TaskEntity?,
  onDismiss: () -> Unit,
  onConfirm: (title: String, notes: String, targetDate: String, plannedDuration: Int) -> Unit
) {
  var title by remember { mutableStateOf(existingTask?.title ?: "") }
  var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
  var targetDate by remember { mutableStateOf(existingTask?.targetDate ?: "") }
  var plannedDuration by remember { mutableIntStateOf(existingTask?.plannedDurationMinutes ?: 25) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existingTask == null) "New Focus Task" else "Edit Task",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            error = null
          },
          label = { Text("Task Title *") },
          placeholder = { Text("e.g., Complete Chapter 3 summary") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("task_title_input")
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Optional Notes") },
          placeholder = { Text("References, links, sub-steps") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )

        Text(
          text = "Planned Duration: ${plannedDuration}m",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(15, 25, 45, 60).forEach { mins ->
            FilterChip(
              selected = plannedDuration == mins,
              onClick = { plannedDuration = mins },
              label = { Text("${mins}m") }
            )
          }
        }

        if (error != null) {
          Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) {
            error = "Please enter a task title."
          } else {
            onConfirm(title, notes, targetDate, plannedDuration)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
        modifier = Modifier.testTag("save_task_button")
      ) {
        Text("Save Task")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
