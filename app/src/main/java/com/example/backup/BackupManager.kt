package com.example.backup

import com.example.data.local.AppDatabase
import com.example.data.local.entity.BlockedAppEntity
import com.example.data.local.entity.RewardTransactionEntity
import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TimerSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

  const val BACKUP_FORMAT = "ADVANCED_FOCUS_BACKUP"
  const val BACKUP_CURRENT_VERSION = 1

  suspend fun createBackupJson(database: AppDatabase): String = withContext(Dispatchers.IO) {
    val root = JSONObject()

    root.put("format", BACKUP_FORMAT)
    root.put("version", BACKUP_CURRENT_VERSION)
    root.put("exportTimestamp", System.currentTimeMillis())

    // 1. Settings (Without secrets / salts)
    val settings = database.timerSettingsDao().getSettingsDirect()
    if (settings != null) {
      val settingsObj = JSONObject().apply {
        put("focusDurationMinutes", settings.focusDurationMinutes)
        put("shortBreakMinutes", settings.shortBreakMinutes)
        put("longBreakMinutes", settings.longBreakMinutes)
        put("autoStartBreaks", settings.autoStartBreaks)
        put("autoStartFocus", settings.autoStartFocus)
        put("isStrictModeEnabled", settings.isStrictModeEnabled)
        put("notifyOnSessionComplete", settings.notifyOnSessionComplete)
        put("notifyOnShieldAlert", settings.notifyOnShieldAlert)
        put("soundAlertsEnabled", settings.soundAlertsEnabled)
        put("vibrationAlertsEnabled", settings.vibrationAlertsEnabled)
        put("focusRemindersEnabled", settings.focusRemindersEnabled)
      }
      root.put("settings", settingsObj)
    }

    // 2. Sessions
    val sessions = database.sessionDao().getAllSessionsDirect()
    val sessionsArray = JSONArray()
    for (s in sessions) {
      val obj = JSONObject().apply {
        put("sessionId", s.sessionId)
        put("sessionType", s.sessionType)
        put("plannedDurationSeconds", s.plannedDurationSeconds)
        put("actualDurationSeconds", s.actualDurationSeconds)
        put("startTimestamp", s.startTimestamp)
        put("endTimestamp", s.endTimestamp)
        put("status", s.status)
        put("dateFormatted", s.dateFormatted)
      }
      sessionsArray.put(obj)
    }
    root.put("sessions", sessionsArray)

    // 3. Tasks
    val tasks = database.taskDao().getAllTasksDirect()
    val tasksArray = JSONArray()
    for (t in tasks) {
      val obj = JSONObject().apply {
        put("title", t.title)
        put("notes", t.notes)
        put("targetDate", t.targetDate)
        put("plannedDurationMinutes", t.plannedDurationMinutes)
        put("isCompleted", t.isCompleted)
        put("completedTimestamp", t.completedTimestamp ?: JSONObject.NULL)
        put("createdTimestamp", t.createdTimestamp)
        put("associatedSessionId", t.associatedSessionId ?: JSONObject.NULL)
      }
      tasksArray.put(obj)
    }
    root.put("tasks", tasksArray)

    // 4. Rewards
    val rewards = database.rewardDao().getAllRewardsDirect()
    val rewardsArray = JSONArray()
    for (r in rewards) {
      val obj = JSONObject().apply {
        put("sourceId", r.sourceId)
        put("sourceType", r.sourceType)
        put("points", r.points)
        put("description", r.description)
        put("timestamp", r.timestamp)
      }
      rewardsArray.put(obj)
    }
    root.put("rewards", rewardsArray)

    // 5. Blocked Apps
    val blockedApps = database.blockedAppDao().getAllBlockedAppsDirect()
    val blockedAppsArray = JSONArray()
    for (b in blockedApps) {
      val obj = JSONObject().apply {
        put("packageName", b.packageName)
        put("appName", b.appName)
      }
      blockedAppsArray.put(obj)
    }
    root.put("blockedApps", blockedAppsArray)

    root.toString(2)
  }

  fun parseAndValidateBackupJson(jsonString: String): BackupValidationResult {
    if (jsonString.isBlank()) {
      return BackupValidationResult.Error("Selected backup file is empty.")
    }

    val root = try {
      JSONObject(jsonString)
    } catch (e: Exception) {
      return BackupValidationResult.Error("Corrupted or malformed JSON file: ${e.message}")
    }

    val format = root.optString("format")
    if (format != BACKUP_FORMAT) {
      return BackupValidationResult.Error("Incompatible file format. Expected $BACKUP_FORMAT, found '$format'.")
    }

    val version = root.optInt("version", -1)
    if (version <= 0 || version > BACKUP_CURRENT_VERSION) {
      return BackupValidationResult.Error("Unsupported backup version ($version). This app supports up to version $BACKUP_CURRENT_VERSION.")
    }

    val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

    // Parse Settings
    val settingsObj = root.optJSONObject("settings")
    val settings = if (settingsObj != null) {
      TimerSettingsEntity(
        focusDurationMinutes = settingsObj.optInt("focusDurationMinutes", 25),
        shortBreakMinutes = settingsObj.optInt("shortBreakMinutes", 5),
        longBreakMinutes = settingsObj.optInt("longBreakMinutes", 15),
        autoStartBreaks = settingsObj.optBoolean("autoStartBreaks", false),
        autoStartFocus = settingsObj.optBoolean("autoStartFocus", false),
        isStrictModeEnabled = settingsObj.optBoolean("isStrictModeEnabled", false),
        notifyOnSessionComplete = settingsObj.optBoolean("notifyOnSessionComplete", true),
        notifyOnShieldAlert = settingsObj.optBoolean("notifyOnShieldAlert", true),
        soundAlertsEnabled = settingsObj.optBoolean("soundAlertsEnabled", true),
        vibrationAlertsEnabled = settingsObj.optBoolean("vibrationAlertsEnabled", true),
        focusRemindersEnabled = settingsObj.optBoolean("focusRemindersEnabled", true)
      )
    } else null

    // Parse Sessions
    val sessions = mutableListOf<SessionRecord>()
    val sessionsArray = root.optJSONArray("sessions")
    if (sessionsArray != null) {
      for (i in 0 until sessionsArray.length()) {
        val sObj = sessionsArray.optJSONObject(i) ?: continue
        val sessionId = sObj.optString("sessionId")
        if (sessionId.isBlank()) continue
        sessions.add(
          SessionRecord(
            sessionId = sessionId,
            sessionType = sObj.optString("sessionType", "FOCUS"),
            plannedDurationSeconds = sObj.optInt("plannedDurationSeconds", 1500),
            actualDurationSeconds = sObj.optInt("actualDurationSeconds", 1500),
            startTimestamp = sObj.optLong("startTimestamp", 0L),
            endTimestamp = sObj.optLong("endTimestamp", 0L),
            status = sObj.optString("status", "COMPLETED"),
            dateFormatted = sObj.optString("dateFormatted", "")
          )
        )
      }
    }

    // Parse Tasks
    val tasks = mutableListOf<TaskEntity>()
    val tasksArray = root.optJSONArray("tasks")
    if (tasksArray != null) {
      for (i in 0 until tasksArray.length()) {
        val tObj = tasksArray.optJSONObject(i) ?: continue
        val title = tObj.optString("title")
        if (title.isBlank()) continue
        val completedTimestamp = if (tObj.has("completedTimestamp") && !tObj.isNull("completedTimestamp")) {
          tObj.optLong("completedTimestamp")
        } else null
        val associatedSessionId = if (tObj.has("associatedSessionId") && !tObj.isNull("associatedSessionId")) {
          tObj.optString("associatedSessionId")
        } else null

        tasks.add(
          TaskEntity(
            title = title,
            notes = tObj.optString("notes", ""),
            targetDate = tObj.optString("targetDate", ""),
            plannedDurationMinutes = tObj.optInt("plannedDurationMinutes", 25),
            isCompleted = tObj.optBoolean("isCompleted", false),
            completedTimestamp = completedTimestamp,
            createdTimestamp = tObj.optLong("createdTimestamp", System.currentTimeMillis()),
            associatedSessionId = associatedSessionId
          )
        )
      }
    }

    // Parse Rewards
    val rewards = mutableListOf<RewardTransactionEntity>()
    val rewardsArray = root.optJSONArray("rewards")
    if (rewardsArray != null) {
      for (i in 0 until rewardsArray.length()) {
        val rObj = rewardsArray.optJSONObject(i) ?: continue
        val sourceId = rObj.optString("sourceId")
        val sourceType = rObj.optString("sourceType")
        if (sourceId.isBlank() || sourceType.isBlank()) continue
        rewards.add(
          RewardTransactionEntity(
            sourceId = sourceId,
            sourceType = sourceType,
            points = rObj.optInt("points", 0),
            description = rObj.optString("description", ""),
            timestamp = rObj.optLong("timestamp", System.currentTimeMillis())
          )
        )
      }
    }

    // Parse Blocked Apps
    val blockedApps = mutableListOf<BlockedAppEntity>()
    val blockedAppsArray = root.optJSONArray("blockedApps")
    if (blockedAppsArray != null) {
      for (i in 0 until blockedAppsArray.length()) {
        val bObj = blockedAppsArray.optJSONObject(i) ?: continue
        val pkg = bObj.optString("packageName")
        val name = bObj.optString("appName")
        if (pkg.isBlank()) continue
        blockedApps.add(BlockedAppEntity(packageName = pkg, appName = name))
      }
    }

    val backupData = BackupData(
      version = version,
      format = format,
      exportTimestamp = exportTimestamp,
      settings = settings,
      sessions = sessions,
      tasks = tasks,
      rewards = rewards,
      blockedApps = blockedApps
    )

    val summary = BackupSummary(
      exportTimestamp = exportTimestamp,
      version = version,
      sessionsCount = sessions.size,
      tasksCount = tasks.size,
      rewardsCount = rewards.size,
      blockedAppsCount = blockedApps.size,
      hasSettings = settings != null
    )

    return BackupValidationResult.Success(backupData, summary)
  }

  suspend fun restoreBackupData(
    database: AppDatabase,
    data: BackupData,
    mode: RestoreMode
  ): RestoreResult = withContext(Dispatchers.IO) {
    try {
      var sessionsRestored = 0
      var tasksRestored = 0
      var rewardsRestored = 0
      var blockedRestored = 0

      // Execute within database transaction to guarantee rollback on any failure
      database.runInTransaction {
        if (mode == RestoreMode.REPLACE) {
          kotlinx.coroutines.runBlocking {
            database.sessionDao().clearAllSessions()
            database.taskDao().clearAllTasks()
            database.rewardDao().clearAllRewards()
            database.blockedAppDao().clearAllBlockedApps()
          }
        }

        kotlinx.coroutines.runBlocking {
          // 1. Settings: Apply user configuration while preserving existing local PIN security
          if (data.settings != null) {
            val currentSettings = database.timerSettingsDao().getSettingsDirect()
            val mergedSettings = if (currentSettings != null) {
              data.settings.copy(
                isSettingsProtectionEnabled = currentSettings.isSettingsProtectionEnabled,
                hashedPin = currentSettings.hashedPin,
                salt = currentSettings.salt,
                recoveryHint = currentSettings.recoveryHint
              )
            } else {
              data.settings
            }
            database.timerSettingsDao().saveSettings(mergedSettings)
          }

          // 2. Sessions: Safe insertion with OnConflictStrategy.IGNORE
          if (data.sessions.isNotEmpty()) {
            val inserted = database.sessionDao().insertSessions(data.sessions)
            sessionsRestored = inserted.count { it > 0 }
          }

          // 3. Tasks: Deduplicated insertion
          if (data.tasks.isNotEmpty()) {
            val existingTasks = database.taskDao().getAllTasksDirect()
            val existingKeys = existingTasks.map { "${it.title}|${it.createdTimestamp}" }.toSet()
            val toInsert = if (mode == RestoreMode.REPLACE) {
              data.tasks
            } else {
              data.tasks.filter { "${it.title}|${it.createdTimestamp}" !in existingKeys }
            }
            if (toInsert.isNotEmpty()) {
              val inserted = database.taskDao().insertTasks(toInsert)
              tasksRestored = inserted.count { it > 0 }
            }
          }

          // 4. Rewards: Safe insertion with OnConflictStrategy.IGNORE on unique index
          if (data.rewards.isNotEmpty()) {
            val inserted = database.rewardDao().insertRewards(data.rewards)
            rewardsRestored = inserted.count { it > 0 }
          }

          // 5. Blocked Apps
          if (data.blockedApps.isNotEmpty()) {
            database.blockedAppDao().insertAll(data.blockedApps)
            blockedRestored = data.blockedApps.size
          }
        }
      }

      RestoreResult.Success(
        sessionsRestored = sessionsRestored,
        tasksRestored = tasksRestored,
        rewardsRestored = rewardsRestored,
        blockedAppsRestored = blockedRestored,
        mode = mode
      )
    } catch (e: Exception) {
      RestoreResult.Failure("Failed to restore backup: ${e.message}")
    }
  }
}
