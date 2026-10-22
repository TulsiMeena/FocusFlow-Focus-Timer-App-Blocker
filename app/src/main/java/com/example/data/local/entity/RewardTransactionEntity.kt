package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "reward_transactions",
  indices = [Index(value = ["sourceId", "sourceType"], unique = true)]
)
data class RewardTransactionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val sourceId: String,
  val sourceType: String, // "SESSION_COMPLETED", "TASK_COMPLETED", "STREAK_MILESTONE", "MINUTES_MILESTONE"
  val points: Int,
  val description: String,
  val timestamp: Long = System.currentTimeMillis()
)
