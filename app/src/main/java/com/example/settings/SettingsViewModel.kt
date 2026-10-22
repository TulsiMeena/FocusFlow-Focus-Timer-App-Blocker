package com.example.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.TimerSettingsEntity
import com.example.data.repository.SessionRepository
import com.example.notifications.DndHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.map

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: SessionRepository

  init {
    val database = AppDatabase.getInstance(application)
    repository = SessionRepository(
      database.sessionDao(),
      database.timerSettingsDao(),
      database.activeTimerDao(),
      database.blockedAppDao()
    )
  }

  val timerSettings: StateFlow<TimerSettingsEntity> = repository.timerSettings
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = TimerSettingsEntity()
    )

  val isMasterShieldEnabled: StateFlow<Boolean> = timerSettings
    .map { it.isMasterShieldEnabled }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = true
    )

  val isShieldServiceRunning: StateFlow<Boolean> = com.example.shield.FocusShieldService.isServiceRunning

  val blockedAppCount: StateFlow<Int> = repository.blockedApps
    .map { it.size }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

  private val _isDndAccessGranted = MutableStateFlow(false)
  val isDndAccessGranted: StateFlow<Boolean> = _isDndAccessGranted.asStateFlow()

  private val _isOverlayPermissionGranted = MutableStateFlow(false)
  val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

  val showOverlayExplanationDialog = MutableStateFlow(false)

  private val _isSessionUnlocked = MutableStateFlow(false)
  val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

  // Dialog visibility states
  val showPinSetupDialog = MutableStateFlow(false)
  val showPinVerifyDialog = MutableStateFlow(false)
  val showPinRecoveryDialog = MutableStateFlow(false)
  val showUninstallInfoDialog = MutableStateFlow(false)
  val showDndExplanationDialog = MutableStateFlow(false)
  val showNotificationListenerExplanationDialog = MutableStateFlow(false)

  private var pendingProtectedAction: (() -> Unit)? = null

  init {
    checkDndPermission()
    checkOverlayPermission()
  }

  fun checkDndPermission() {
    val context = getApplication<Application>()
    _isDndAccessGranted.value = DndHelper.isDndAccessGranted(context)
  }

  fun checkOverlayPermission() {
    val context = getApplication<Application>()
    _isOverlayPermissionGranted.value = com.example.apps.util.OverlayPermissionHelper.isOverlayPermissionGranted(context)
  }

  fun openDndSettings(context: Context): Boolean {
    return DndHelper.openDndSettings(context)
  }

  fun openNotificationSettings(context: Context): Boolean {
    return DndHelper.openNotificationSettings(context)
  }

  fun openOverlaySettings(context: Context): Boolean {
    return com.example.apps.util.OverlayPermissionHelper.openOverlaySettings(context)
  }

  fun openOverlaySettingsList(context: Context): Boolean {
    return com.example.apps.util.OverlayPermissionHelper.openOverlaySettingsList(context)
  }

  fun openAppDetailsSettings(context: Context): Boolean {
    return DndHelper.openAndroidAppDetailsSettings(context)
  }

  fun updateNotificationPreferences(
    notifySessionComplete: Boolean,
    notifyShieldAlert: Boolean,
    soundAlertsEnabled: Boolean,
    vibrationAlertsEnabled: Boolean,
    focusRemindersEnabled: Boolean
  ) {
    viewModelScope.launch {
      repository.updateNotificationSettings(
        notifySessionComplete = notifySessionComplete,
        notifyShieldAlert = notifyShieldAlert,
        soundAlertsEnabled = soundAlertsEnabled,
        vibrationAlertsEnabled = vibrationAlertsEnabled,
        focusRemindersEnabled = focusRemindersEnabled
      )
    }
  }

  fun enablePinProtection(pin: String, recoveryHint: String) {
    viewModelScope.launch {
      repository.enableSettingsProtection(pin, recoveryHint)
      _isSessionUnlocked.value = true
      showPinSetupDialog.value = false
    }
  }

  fun disablePinProtection() {
    viewModelScope.launch {
      repository.disableSettingsProtection()
      _isSessionUnlocked.value = false
    }
  }

  fun executeWithProtection(action: () -> Unit) {
    val settings = timerSettings.value
    if (!settings.isSettingsProtectionEnabled || _isSessionUnlocked.value) {
      action()
    } else {
      pendingProtectedAction = action
      showPinVerifyDialog.value = true
    }
  }

  fun onPinVerifiedSuccessfully() {
    _isSessionUnlocked.value = true
    showPinVerifyDialog.value = false
    pendingProtectedAction?.invoke()
    pendingProtectedAction = null
  }

  fun resetPinViaEmergencyRecovery(newPin: String, newHint: String) {
    viewModelScope.launch {
      repository.enableSettingsProtection(newPin, newHint)
      _isSessionUnlocked.value = true
      showPinRecoveryDialog.value = false
    }
  }

  fun setStrictMode(enabled: Boolean) {
    viewModelScope.launch {
      val current = repository.getSettings()
      repository.saveSettings(current.copy(isStrictModeEnabled = enabled))
    }
  }

  fun setMasterShieldEnabled(enabled: Boolean) {
    executeWithProtection {
      viewModelScope.launch {
        repository.setMasterShieldEnabled(enabled)
        if (!enabled) {
          com.example.shield.FocusShieldService.stop(getApplication())
          backupStatusMessage.value = "App Shield is now turned OFF. Blocked apps will not be restricted."
        } else {
          backupStatusMessage.value = "App Shield is now turned ON. Restrictions will apply during focus."
        }
      }
    }
  }

  fun turnOffAllAlerts() {
    viewModelScope.launch {
      repository.turnOffAllAlerts()
      backupStatusMessage.value = "All alerts, vibrations, and audio cues turned OFF."
    }
  }

  fun turnOnAllAlerts() {
    viewModelScope.launch {
      repository.turnOnAllAlerts()
      backupStatusMessage.value = "All alerts and notifications turned ON."
    }
  }

  fun turnOffAllBlockedApps() {
    executeWithProtection {
      viewModelScope.launch {
        repository.clearAllBlockedApps()
        com.example.shield.FocusShieldService.stop(getApplication())
        backupStatusMessage.value = "All app blocks removed. Shield list cleared."
      }
    }
  }

  fun resetSettingsToDefaults() {
    executeWithProtection {
      viewModelScope.launch {
        repository.resetAllSettingsToDefaults()
        com.example.shield.FocusShieldService.stop(getApplication())
        backupStatusMessage.value = "All settings reset to defaults. All restrictions turned OFF."
      }
    }
  }

  fun turnOffActiveFocus() {
    viewModelScope.launch {
      repository.clearActiveTimerState()
      com.example.shield.FocusShieldService.stop(getApplication())
      backupStatusMessage.value = "Active focus session turned OFF."
    }
  }

  // Backup & Restore
  private val database = AppDatabase.getInstance(getApplication())

  val pendingBackupValidation = MutableStateFlow<com.example.backup.BackupValidationResult.Success?>(null)
  val showRestoreConfirmDialog = MutableStateFlow(false)
  val backupStatusMessage = MutableStateFlow<String?>(null)

  suspend fun exportBackupJson(): String {
    return com.example.backup.BackupManager.createBackupJson(database)
  }

  fun importBackupContent(jsonString: String) {
    when (val result = com.example.backup.BackupManager.parseAndValidateBackupJson(jsonString)) {
      is com.example.backup.BackupValidationResult.Success -> {
        pendingBackupValidation.value = result
        showRestoreConfirmDialog.value = true
      }
      is com.example.backup.BackupValidationResult.Error -> {
        backupStatusMessage.value = "Backup validation failed: ${result.message}"
      }
    }
  }

  fun executeRestore(mode: com.example.backup.RestoreMode) {
    val validation = pendingBackupValidation.value ?: return
    viewModelScope.launch {
      when (val result = com.example.backup.BackupManager.restoreBackupData(database, validation.backupData, mode)) {
        is com.example.backup.RestoreResult.Success -> {
          backupStatusMessage.value = "Restore successful: ${result.sessionsRestored} sessions, ${result.tasksRestored} tasks, ${result.rewardsRestored} rewards restored."
          showRestoreConfirmDialog.value = false
          pendingBackupValidation.value = null
        }
        is com.example.backup.RestoreResult.Failure -> {
          backupStatusMessage.value = "Restore failed: ${result.error}"
          showRestoreConfirmDialog.value = false
        }
      }
    }
  }

  fun dismissRestoreDialog() {
    showRestoreConfirmDialog.value = false
    pendingBackupValidation.value = null
  }

  fun clearBackupStatusMessage() {
    backupStatusMessage.value = null
  }
}
