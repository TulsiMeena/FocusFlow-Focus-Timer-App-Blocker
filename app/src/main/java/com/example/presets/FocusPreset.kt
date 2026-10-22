package com.example.presets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

data class FocusPreset(
  val id: String,
  val name: String,
  val focusMinutes: Int,
  val breakMinutes: Int,
  val description: String,
  val icon: ImageVector,
  val isStrictMode: Boolean = false
) {
  companion object {
    val ALL_PRESETS = listOf(
      FocusPreset(
        id = "pomodoro",
        name = "Pomodoro",
        focusMinutes = 25,
        breakMinutes = 5,
        description = "Classic productivity rhythm (25m focus / 5m break)",
        icon = Icons.Default.HourglassBottom
      ),
      FocusPreset(
        id = "deep_work",
        name = "Deep Work",
        focusMinutes = 50,
        breakMinutes = 10,
        description = "Intense distraction-free focus block",
        icon = Icons.Default.Psychology,
        isStrictMode = true
      ),
      FocusPreset(
        id = "study_sprint",
        name = "Study Sprint",
        focusMinutes = 45,
        breakMinutes = 15,
        description = "Ideal for reading, problem solving & revisions",
        icon = Icons.Default.School
      ),
      FocusPreset(
        id = "quick_sprint",
        name = "Quick Sprint",
        focusMinutes = 15,
        breakMinutes = 3,
        description = "Short high-energy burst to clear pending tasks",
        icon = Icons.Default.Bolt
      ),
      FocusPreset(
        id = "monk_mode",
        name = "Monk Mode",
        focusMinutes = 90,
        breakMinutes = 20,
        description = "Uncompromising lockdown session for critical projects",
        icon = Icons.Default.Lock,
        isStrictMode = true
      )
    )
  }
}
