package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "session_records",
  indices = [Index(value = ["sessionId"], unique = true)]
)
data class SessionRecord(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val sessionId: String,
  val sessionType: String, // "FOCUS", "SHORT_BREAK", "LONG_BREAK"
  val plannedDurationSeconds: Int,
  val actualDurationSeconds: Int,
  val startTimestamp: Long,
  val endTimestamp: Long,
  val status: String, // "COMPLETED", "CANCELLED", "INTERRUPTED"
  val dateFormatted: String = "" // e.g. "Oct 10, 2026"
)
