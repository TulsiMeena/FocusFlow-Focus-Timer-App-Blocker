package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_focus_blocks")
data class ScheduledFocusEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val title: String,
  val startTime: String,
  val endTime: String,
  val daysOfWeek: String,
  val isEnabled: Boolean = true,
  val isStrictMode: Boolean = true
)
