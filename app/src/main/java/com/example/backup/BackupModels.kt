package com.example.backup

import com.example.data.local.entity.BlockedAppEntity
import com.example.data.local.entity.RewardTransactionEntity
import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TimerSettingsEntity

data class BackupSummary(
  val exportTimestamp: Long,
  val version: Int,
  val sessionsCount: Int,
  val tasksCount: Int,
  val rewardsCount: Int,
  val blockedAppsCount: Int,
  val hasSettings: Boolean
)

data class BackupData(
  val version: Int = 1,
  val format: String = "ADVANCED_FOCUS_BACKUP",
  val exportTimestamp: Long = System.currentTimeMillis(),
  val settings: TimerSettingsEntity?,
  val sessions: List<SessionRecord>,
  val tasks: List<TaskEntity>,
  val rewards: List<RewardTransactionEntity>,
  val blockedApps: List<BlockedAppEntity>
)

enum class RestoreMode {
  MERGE,
  REPLACE
}

sealed class BackupValidationResult {
  data class Success(val backupData: BackupData, val summary: BackupSummary) : BackupValidationResult()
  data class Error(val message: String) : BackupValidationResult()
}

sealed class RestoreResult {
  data class Success(
    val sessionsRestored: Int,
    val tasksRestored: Int,
    val rewardsRestored: Int,
    val blockedAppsRestored: Int,
    val mode: RestoreMode
  ) : RestoreResult()

  data class Failure(val error: String) : RestoreResult()
}
