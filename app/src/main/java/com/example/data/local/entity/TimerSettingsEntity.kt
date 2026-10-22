package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timer_settings")
data class TimerSettingsEntity(
  @PrimaryKey
  val id: Int = 1,
  val focusDurationMinutes: Int = 25,
  val shortBreakMinutes: Int = 5,
  val longBreakMinutes: Int = 15,
  val autoStartBreaks: Boolean = false,
  val autoStartFocus: Boolean = false,
  val isStrictModeEnabled: Boolean = false,
  // Notification Shield Settings
  val notifyOnSessionComplete: Boolean = true,
  val notifyOnShieldAlert: Boolean = true,
  val soundAlertsEnabled: Boolean = true,
  val vibrationAlertsEnabled: Boolean = true,
  val focusRemindersEnabled: Boolean = true,
  // Settings Protection
  val isSettingsProtectionEnabled: Boolean = false,
  val hashedPin: String = "",
  val salt: String = "",
  val recoveryHint: String = "",
  // Master switch to turn OFF all app blocking
  val isMasterShieldEnabled: Boolean = true
)
