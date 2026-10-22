package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_timer_state")
data class ActiveTimerStateEntity(
  @PrimaryKey
  val id: Int = 1,
  val sessionId: String = "",
  val mode: String = "FOCUS", // "FOCUS", "SHORT_BREAK", "LONG_BREAK"
  val status: String = "IDLE", // "IDLE", "RUNNING", "PAUSED", "COMPLETED"
  val totalDurationSeconds: Int = 25 * 60,
  val remainingSeconds: Int = 25 * 60,
  val startTimeMillis: Long = 0L,
  val targetEndTimeMillis: Long = 0L,
  val lastUpdatedMillis: Long = 0L,
  val hasBeenRecorded: Boolean = false
)
