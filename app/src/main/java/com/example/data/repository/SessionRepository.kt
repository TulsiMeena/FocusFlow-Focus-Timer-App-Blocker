package com.example.data.repository

import com.example.data.local.dao.ActiveTimerDao
import com.example.data.local.dao.BlockedAppDao
import com.example.data.local.dao.SessionDao
import com.example.data.local.dao.TimerSettingsDao
import com.example.data.local.entity.ActiveTimerStateEntity
import com.example.data.local.entity.BlockedAppEntity
import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TimerSettingsEntity
import com.example.security.SecurityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.data.local.dao.RewardDao
import com.example.data.local.entity.RewardTransactionEntity
import kotlinx.coroutines.withContext

class SessionRepository(
  private val sessionDao: SessionDao,
  private val timerSettingsDao: TimerSettingsDao,
  private val activeTimerDao: ActiveTimerDao,
  private val blockedAppDao: BlockedAppDao,
  private val rewardDao: RewardDao? = null
) {
  val allSessions: Flow<List<SessionRecord>> = sessionDao.getAllSessions()

  fun getRecentSessions(limit: Int = 10): Flow<List<SessionRecord>> =
    sessionDao.getRecentSessions(limit)

  val timerSettings: Flow<TimerSettingsEntity> = timerSettingsDao.getSettings()
    .map { it ?: TimerSettingsEntity() }

  val blockedApps: Flow<List<BlockedAppEntity>> = blockedAppDao.getAllBlockedApps()

  val blockedPackageNames: Flow<List<String>> = blockedAppDao.getBlockedPackageNames()

  suspend fun recordSession(session: SessionRecord): Boolean = withContext(Dispatchers.IO) {
    val existing = sessionDao.getSessionBySessionId(session.sessionId)
    if (existing != null) {
      return@withContext false
    }
    val insertId = sessionDao.insertSession(session)
    if (insertId > 0 && session.sessionType == "FOCUS" && session.status == "COMPLETED") {
      val points = (session.actualDurationSeconds / 300) * 10
      if (points > 0 && rewardDao != null) {
        rewardDao.insertReward(
          RewardTransactionEntity(
            sourceId = session.sessionId,
            sourceType = "SESSION_COMPLETED",
            points = points,
            description = "Completed ${session.actualDurationSeconds / 60}m Focus Session"
          )
        )
      }
    }
    insertId > 0
  }

  suspend fun deleteSession(id: Long) = withContext(Dispatchers.IO) {
    sessionDao.deleteSession(id)
  }

  suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
    sessionDao.clearAllSessions()
  }

  suspend fun saveSettings(settings: TimerSettingsEntity) = withContext(Dispatchers.IO) {
    timerSettingsDao.saveSettings(settings)
  }

  suspend fun getSettings(): TimerSettingsEntity = withContext(Dispatchers.IO) {
    timerSettingsDao.getSettingsDirect() ?: TimerSettingsEntity()
  }

  suspend fun setStrictModeEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    val current = getSettings()
    saveSettings(current.copy(isStrictModeEnabled = enabled))
  }

  suspend fun setAppBlocked(packageName: String, appName: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
    if (isBlocked) {
      blockedAppDao.insertBlockedApp(BlockedAppEntity(packageName, appName))
    } else {
      blockedAppDao.deleteByPackageName(packageName)
    }
  }

  suspend fun getBlockedAppsDirect(): List<BlockedAppEntity> = withContext(Dispatchers.IO) {
    blockedAppDao.getAllBlockedAppsDirect()
  }

  suspend fun getBlockedPackageNamesDirect(): List<String> = withContext(Dispatchers.IO) {
    blockedAppDao.getBlockedPackageNamesDirect()
  }

  suspend fun clearAllBlockedApps() = withContext(Dispatchers.IO) {
    blockedAppDao.clearAllBlockedApps()
  }

  suspend fun updateNotificationSettings(
    notifySessionComplete: Boolean,
    notifyShieldAlert: Boolean,
    soundAlertsEnabled: Boolean,
    vibrationAlertsEnabled: Boolean,
    focusRemindersEnabled: Boolean
  ) = withContext(Dispatchers.IO) {
    val current = getSettings()
    val updated = current.copy(
      notifyOnSessionComplete = notifySessionComplete,
      notifyOnShieldAlert = notifyShieldAlert,
      soundAlertsEnabled = soundAlertsEnabled,
      vibrationAlertsEnabled = vibrationAlertsEnabled,
      focusRemindersEnabled = focusRemindersEnabled
    )
    saveSettings(updated)
  }

  suspend fun enableSettingsProtection(pin: String, recoveryHint: String) = withContext(Dispatchers.IO) {
    val current = getSettings()
    val salt = SecurityHelper.generateSalt()
    val hashed = SecurityHelper.hashPin(pin, salt)
    val updated = current.copy(
      isSettingsProtectionEnabled = true,
      hashedPin = hashed,
      salt = salt,
      recoveryHint = recoveryHint.trim()
    )
    saveSettings(updated)
  }

  suspend fun disableSettingsProtection() = withContext(Dispatchers.IO) {
    val current = getSettings()
    val updated = current.copy(
      isSettingsProtectionEnabled = false,
      hashedPin = "",
      salt = "",
      recoveryHint = ""
    )
    saveSettings(updated)
  }

  suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.IO) {
    val current = getSettings()
    if (!current.isSettingsProtectionEnabled) return@withContext true
    SecurityHelper.verifyPin(pin, current.salt, current.hashedPin)
  }

  suspend fun saveActiveTimerState(state: ActiveTimerStateEntity) = withContext(Dispatchers.IO) {
    activeTimerDao.saveActiveTimerState(state)
  }

  suspend fun getActiveTimerState(): ActiveTimerStateEntity? = withContext(Dispatchers.IO) {
    activeTimerDao.getActiveTimerStateDirect()
  }

  suspend fun clearActiveTimerState() = withContext(Dispatchers.IO) {
    activeTimerDao.clearActiveTimerState()
  }

  suspend fun setMasterShieldEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    val current = getSettings()
    saveSettings(current.copy(isMasterShieldEnabled = enabled))
  }

  suspend fun turnOffAllAlerts() = withContext(Dispatchers.IO) {
    val current = getSettings()
    saveSettings(
      current.copy(
        notifyOnSessionComplete = false,
        notifyOnShieldAlert = false,
        soundAlertsEnabled = false,
        vibrationAlertsEnabled = false,
        focusRemindersEnabled = false
      )
    )
  }

  suspend fun turnOnAllAlerts() = withContext(Dispatchers.IO) {
    val current = getSettings()
    saveSettings(
      current.copy(
        notifyOnSessionComplete = true,
        notifyOnShieldAlert = true,
        soundAlertsEnabled = true,
        vibrationAlertsEnabled = true,
        focusRemindersEnabled = true
      )
    )
  }

  suspend fun resetAllSettingsToDefaults() = withContext(Dispatchers.IO) {
    saveSettings(TimerSettingsEntity())
  }
}
