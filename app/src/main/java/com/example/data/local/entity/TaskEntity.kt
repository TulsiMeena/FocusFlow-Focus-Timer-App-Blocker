package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val notes: String = "",
  val targetDate: String, // "yyyy-MM-dd"
  val plannedDurationMinutes: Int = 25,
  val isCompleted: Boolean = false,
  val completedTimestamp: Long? = null,
  val createdTimestamp: Long = System.currentTimeMillis(),
  val associatedSessionId: String? = null
)
