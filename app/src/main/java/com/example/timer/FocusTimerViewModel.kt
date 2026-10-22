package com.example.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ActiveTimerStateEntity
import com.example.data.local.entity.BlockedAppEntity
import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TimerSettingsEntity
import com.example.data.repository.SessionRepository
import com.example.apps.util.OverlayPermissionHelper
import com.example.apps.util.UsagePermissionHelper
import com.example.shield.FocusShieldMonitor
import com.example.shield.FocusShieldService
import com.example.shield.ShieldDiagnostics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class FocusTimerViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: SessionRepository
  private val scheduleRepository: com.example.schedule.ScheduleRepository
  private val notificationHelper = NotificationHelper(application)
  private val focusShieldMonitor: FocusShieldMonitor
  private val audioEngine = com.example.audio.FocusAudioEngine.get()

  init {
    val database = AppDatabase.getInstance(application)
    repository = SessionRepository(
      database.sessionDao(),
      database.timerSettingsDao(),
      database.activeTimerDao(),
      database.blockedAppDao(),
      database.rewardDao()
    )
    scheduleRepository = com.example.schedule.ScheduleRepository(database.scheduledFocusDao())

    focusShieldMonitor = FocusShieldMonitor(
      context = application,
      scope = viewModelScope,
      notificationHelper = notificationHelper,
      onDistractionDetected = { appName, _ ->
        _distractionAlertAppName.value = appName
      }
    )
  }

  // Timer State
  private val _mode = MutableStateFlow(TimerMode.FOCUS)
  val mode: StateFlow<TimerMode> = _mode.asStateFlow()

  private val _status = MutableStateFlow(TimerStatus.IDLE)
  val status: StateFlow<TimerStatus> = _status.asStateFlow()

  private val _totalDurationSeconds = MutableStateFlow(25 * 60)
  val totalDurationSeconds: StateFlow<Int> = _totalDurationSeconds.asStateFlow()

  private val _remainingSeconds = MutableStateFlow(25 * 60)
  val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

  // Shield & Restriction State
  private val _isShieldActive = MutableStateFlow(false)
  val isShieldActive: StateFlow<Boolean> = _isShieldActive.asStateFlow()

  val shieldDiagnostics: StateFlow<ShieldDiagnostics> = focusShieldMonitor.diagnostics

  private val _distractionAlertAppName = MutableStateFlow<String?>(null)
  val distractionAlertAppName: StateFlow<String?> = _distractionAlertAppName.asStateFlow()

  private val _isUsageAccessGranted = MutableStateFlow(false)
  val isUsageAccessGranted: StateFlow<Boolean> = _isUsageAccessGranted.asStateFlow()

  private val _isOverlayPermissionGranted = MutableStateFlow(false)
  val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

  // Session Tracking Metadata
  private var activeSessionId: String = UUID.randomUUID().toString()
  private var startTimeMillis: Long = 0L
  private var targetEndTimeMillis: Long = 0L
  private val isCompletingSession = AtomicBoolean(false)
  private var tickerJob: Job? = null

  // Dialog & UI Feedback states
  private val _showCancelDialog = MutableStateFlow(false)
  val showCancelDialog: StateFlow<Boolean> = _showCancelDialog.asStateFlow()

  private val _showStrictCancelDialog = MutableStateFlow(false)
  val showStrictCancelDialog: StateFlow<Boolean> = _showStrictCancelDialog.asStateFlow()

  private val _showCustomDurationDialog = MutableStateFlow(false)
  val showCustomDurationDialog: StateFlow<Boolean> = _showCustomDurationDialog.asStateFlow()

  private val _showCompletionDialog = MutableStateFlow(false)
  val showCompletionDialog: StateFlow<Boolean> = _showCompletionDialog.asStateFlow()

  private val _lastCompletedSession = MutableStateFlow<SessionRecord?>(null)
  val lastCompletedSession: StateFlow<SessionRecord?> = _lastCompletedSession.asStateFlow()

  // Reactive Data from Repository
  val allSessions: StateFlow<List<SessionRecord>> = repository.allSessions
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val timerSettings: StateFlow<TimerSettingsEntity> = repository.timerSettings
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = TimerSettingsEntity()
    )

  val isStrictModeEnabled: StateFlow<Boolean> = timerSettings.map { it.isStrictModeEnabled }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  val isMasterShieldEnabled: StateFlow<Boolean> = timerSettings.map { it.isMasterShieldEnabled }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = true
    )

  val blockedApps: StateFlow<List<BlockedAppEntity>> = repository.blockedApps
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val blockedAppCount: StateFlow<Int> = blockedApps.map { it.size }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

  // Real Focus Minutes and Sessions calculated reactively from Room DB
  val todayFocusMinutes: StateFlow<Int> = allSessions.map { list ->
    val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    list.filter {
      it.sessionType == TimerMode.FOCUS.name &&
        it.status == "COMPLETED" &&
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.startTimestamp)) == todayString
    }.sumOf { it.actualDurationSeconds } / 60
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0
  )

  val todayCompletedSessionsCount: StateFlow<Int> = allSessions.map { list ->
    val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    list.count {
      it.sessionType == TimerMode.FOCUS.name &&
        it.status == "COMPLETED" &&
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.startTimestamp)) == todayString
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0
  )

  val currentStreak: StateFlow<Int> = allSessions.map { list ->
    val completedDates = list
      .filter { it.sessionType == TimerMode.FOCUS.name && it.status == "COMPLETED" }
      .map { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.startTimestamp)) }
      .toSet()
    com.example.rewards.StreakCalculator.calculateStreak(completedDates)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0
  )

  init {
    checkPermissions()
    recoverOrInitializeState()

    // Whenever blockedApps changes in database while timer is actively running, dynamically update the shield
    viewModelScope.launch {
      repository.blockedApps.collect {
        if (_status.value == TimerStatus.RUNNING && _mode.value == TimerMode.FOCUS) {
          activateShield()
        }
      }
    }
  }

  private fun recoverOrInitializeState() {
    viewModelScope.launch {
      val settings = repository.getSettings()

      val activeState = repository.getActiveTimerState()
      if (activeState != null && activeState.status != TimerStatus.IDLE.name) {
        val restoredMode = try {
          TimerMode.valueOf(activeState.mode)
        } catch (_: Exception) {
          TimerMode.FOCUS
        }
        _mode.value = restoredMode
        _totalDurationSeconds.value = activeState.totalDurationSeconds
        activeSessionId = activeState.sessionId.ifEmpty { UUID.randomUUID().toString() }
        startTimeMillis = activeState.startTimeMillis
        targetEndTimeMillis = activeState.targetEndTimeMillis

        when (activeState.status) {
          TimerStatus.RUNNING.name -> {
            val now = System.currentTimeMillis()
            if (now >= targetEndTimeMillis) {
              if (!activeState.hasBeenRecorded) {
                recordCompletedSession(
                  sessionId = activeSessionId,
                  mode = restoredMode,
                  durationSeconds = activeState.totalDurationSeconds,
                  start = startTimeMillis,
                  end = targetEndTimeMillis
                )
              }
              _remainingSeconds.value = 0
              _status.value = TimerStatus.COMPLETED
              _showCompletionDialog.value = true
              notificationHelper.sendTimerCompleteNotification(
                title = "${restoredMode.displayName} Completed!",
                message = "Great work! Session logged to history."
              )
              repository.clearActiveTimerState()
              deactivateShield()
            } else {
              val remaining = ((targetEndTimeMillis - now + 999) / 1000).toInt().coerceAtLeast(0)
              _remainingSeconds.value = remaining
              _status.value = TimerStatus.RUNNING
              startTicker()
              if (restoredMode == TimerMode.FOCUS) {
                activateShield()
              }
            }
          }
          TimerStatus.PAUSED.name -> {
            _remainingSeconds.value = activeState.remainingSeconds.coerceAtLeast(0)
            _status.value = TimerStatus.PAUSED
          }
          else -> {
            resetToDefaultDuration(settings, restoredMode)
          }
        }
      } else {
        resetToDefaultDuration(settings, _mode.value)
      }
    }
  }

  private fun resetToDefaultDuration(settings: TimerSettingsEntity, mode: TimerMode) {
    val durationMinutes = when (mode) {
      TimerMode.FOCUS -> settings.focusDurationMinutes
      TimerMode.SHORT_BREAK -> settings.shortBreakMinutes
      TimerMode.LONG_BREAK -> settings.longBreakMinutes
    }
    _totalDurationSeconds.value = durationMinutes * 60
    _remainingSeconds.value = durationMinutes * 60
    _status.value = TimerStatus.IDLE
  }

  fun setMode(newMode: TimerMode) {
    if (_status.value == TimerStatus.RUNNING || _status.value == TimerStatus.PAUSED) {
      return
    }
    _mode.value = newMode
    val settings = timerSettings.value
    val durationMinutes = when (newMode) {
      TimerMode.FOCUS -> settings.focusDurationMinutes
      TimerMode.SHORT_BREAK -> settings.shortBreakMinutes
      TimerMode.LONG_BREAK -> settings.longBreakMinutes
    }
    _totalDurationSeconds.value = durationMinutes * 60
    _remainingSeconds.value = durationMinutes * 60
    _status.value = TimerStatus.IDLE
    _showCompletionDialog.value = false
  }

  fun setPresetDuration(minutes: Int) {
    if (_status.value != TimerStatus.IDLE && _status.value != TimerStatus.COMPLETED) return
    val seconds = minutes * 60
    _totalDurationSeconds.value = seconds
    _remainingSeconds.value = seconds
    _status.value = TimerStatus.IDLE

    viewModelScope.launch {
      val current = repository.getSettings()
      val updated = when (_mode.value) {
        TimerMode.FOCUS -> current.copy(focusDurationMinutes = minutes)
        TimerMode.SHORT_BREAK -> current.copy(shortBreakMinutes = minutes)
        TimerMode.LONG_BREAK -> current.copy(longBreakMinutes = minutes)
      }
      repository.saveSettings(updated)
    }
  }

  fun setCustomDuration(minutes: Int) {
    val validMinutes = minutes.coerceIn(1, 180)
    setPresetDuration(validMinutes)
    _showCustomDurationDialog.value = false
  }

  fun openCustomDurationDialog() {
    if (_status.value == TimerStatus.IDLE || _status.value == TimerStatus.COMPLETED) {
      _showCustomDurationDialog.value = true
    }
  }

  fun dismissCustomDurationDialog() {
    _showCustomDurationDialog.value = false
  }

  fun setStrictModeEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setStrictModeEnabled(enabled)
      if (_status.value == TimerStatus.RUNNING && _mode.value == TimerMode.FOCUS) {
        val blocked = repository.getBlockedAppsDirect()
        val packageNames = blocked.map { it.packageName }
        val labelsMap = blocked.associate { it.packageName to it.appName }
        FocusShieldService.update(
          getApplication(),
          packageNames,
          labelsMap,
          enabled
        )
      }
      focusShieldMonitor.updateDiagnostics(
        isTimerActive = (_status.value == TimerStatus.RUNNING),
        isStrictMode = enabled
      )
    }
  }

  fun startTimer() {
    if (_status.value == TimerStatus.RUNNING) return

    val now = System.currentTimeMillis()
    if (_status.value == TimerStatus.IDLE || _status.value == TimerStatus.COMPLETED) {
      activeSessionId = UUID.randomUUID().toString()
      startTimeMillis = now
      _remainingSeconds.value = _totalDurationSeconds.value
    }
    targetEndTimeMillis = now + (_remainingSeconds.value * 1000L)
    _status.value = TimerStatus.RUNNING
    _showCompletionDialog.value = false
    isCompletingSession.set(false)

    persistActiveTimerState()
    startTicker()

    if (_mode.value == TimerMode.FOCUS) {
      activateShield()
    }
  }

  fun pauseTimer() {
    if (_status.value != TimerStatus.RUNNING) return
    tickerJob?.cancel()

    val now = System.currentTimeMillis()
    val recalculatedRemaining = ((targetEndTimeMillis - now + 999) / 1000).toInt().coerceAtLeast(0)
    _remainingSeconds.value = recalculatedRemaining
    _status.value = TimerStatus.PAUSED
    targetEndTimeMillis = 0L

    persistActiveTimerState()
    deactivateShield()
  }

  fun resumeTimer() {
    if (_status.value != TimerStatus.PAUSED) return
    val now = System.currentTimeMillis()
    targetEndTimeMillis = now + (_remainingSeconds.value * 1000L)
    _status.value = TimerStatus.RUNNING

    persistActiveTimerState()
    startTicker()

    if (_mode.value == TimerMode.FOCUS) {
      activateShield()
    }
  }

  fun requestCancel() {
    if (_status.value == TimerStatus.RUNNING || _status.value == TimerStatus.PAUSED) {
      if (isStrictModeEnabled.value) {
        _showStrictCancelDialog.value = true
      } else {
        _showCancelDialog.value = true
      }
    } else {
      resetTimer()
    }
  }

  fun dismissCancelDialog() {
    _showCancelDialog.value = false
    _showStrictCancelDialog.value = false
  }

  fun confirmCancel() {
    _showCancelDialog.value = false
    _showStrictCancelDialog.value = false
    tickerJob?.cancel()
    deactivateShield()

    val elapsedSeconds = (_totalDurationSeconds.value - _remainingSeconds.value).coerceAtLeast(0)
    val now = System.currentTimeMillis()

    if (elapsedSeconds > 0) {
      viewModelScope.launch {
        val dateFormatted = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(startTimeMillis))
        val record = SessionRecord(
          sessionId = activeSessionId,
          sessionType = _mode.value.name,
          plannedDurationSeconds = _totalDurationSeconds.value,
          actualDurationSeconds = elapsedSeconds,
          startTimestamp = startTimeMillis,
          endTimestamp = now,
          status = "CANCELLED",
          dateFormatted = dateFormatted
        )
        repository.recordSession(record)
        repository.clearActiveTimerState()
      }
    } else {
      viewModelScope.launch {
        repository.clearActiveTimerState()
      }
    }

    _remainingSeconds.value = _totalDurationSeconds.value
    _status.value = TimerStatus.IDLE
  }

  fun emergencyExit() {
    // Immediate cancellation bypass preserving safety and preventing lock-in
    _showCancelDialog.value = false
    _showStrictCancelDialog.value = false
    tickerJob?.cancel()
    deactivateShield()

    val elapsedSeconds = (_totalDurationSeconds.value - _remainingSeconds.value).coerceAtLeast(0)
    val now = System.currentTimeMillis()

    viewModelScope.launch {
      val dateFormatted = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(startTimeMillis))
      val record = SessionRecord(
        sessionId = activeSessionId,
        sessionType = _mode.value.name,
        plannedDurationSeconds = _totalDurationSeconds.value,
        actualDurationSeconds = elapsedSeconds,
        startTimestamp = startTimeMillis,
        endTimestamp = now,
        status = "CANCELLED (Emergency Exit)",
        dateFormatted = dateFormatted
      )
      repository.recordSession(record)
      repository.clearActiveTimerState()
    }

    _remainingSeconds.value = _totalDurationSeconds.value
    _status.value = TimerStatus.IDLE
  }

  fun resetTimer() {
    tickerJob?.cancel()
    deactivateShield()
    _remainingSeconds.value = _totalDurationSeconds.value
    _status.value = TimerStatus.IDLE
    _showCompletionDialog.value = false
    viewModelScope.launch {
      repository.clearActiveTimerState()
    }
  }

  fun dismissCompletionDialog() {
    _showCompletionDialog.value = false
  }

  fun dismissDistractionAlert() {
    _distractionAlertAppName.value = null
  }

  fun checkPermissions() {
    _isUsageAccessGranted.value = UsagePermissionHelper.isUsageAccessGranted(getApplication())
    _isOverlayPermissionGranted.value = OverlayPermissionHelper.isOverlayPermissionGranted(getApplication())
    focusShieldMonitor.updateDiagnostics(
      isTimerActive = (_status.value == TimerStatus.RUNNING),
      isStrictMode = isStrictModeEnabled.value
    )
  }

  fun checkUsageAccessPermission() {
    checkPermissions()
  }

  fun openUsageAccessSettings(): Boolean {
    return UsagePermissionHelper.openUsageAccessSettings(getApplication())
  }

  fun openOverlaySettings(): Boolean {
    return OverlayPermissionHelper.openOverlaySettings(getApplication())
  }

  fun openAppDetailsSettings(): Boolean {
    return OverlayPermissionHelper.openAppDetailsSettings(getApplication())
  }

  fun openOverlaySettingsList(): Boolean {
    return OverlayPermissionHelper.openOverlaySettingsList(getApplication())
  }

  private fun activateShield() {
    viewModelScope.launch {
      val settings = repository.getSettings()
      if (!settings.isMasterShieldEnabled) {
        _isShieldActive.value = false
        focusShieldMonitor.stopMonitoring()
        return@launch
      }

      val blocked = repository.getBlockedAppsDirect()
      val packageNames = blocked.map { it.packageName }.toSet()
      val labelsMap = blocked.associate { it.packageName to it.appName }

      if (packageNames.isNotEmpty()) {
        _isShieldActive.value = true
        focusShieldMonitor.startMonitoring(
          blockedPackages = packageNames,
          packageNamesToLabels = labelsMap,
          isStrictMode = isStrictModeEnabled.value
        )
      } else {
        _isShieldActive.value = false
        focusShieldMonitor.stopMonitoring()
      }
    }
  }

  private fun deactivateShield() {
    _isShieldActive.value = false
    focusShieldMonitor.stopMonitoring()
  }

  fun setMasterShieldEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setMasterShieldEnabled(enabled)
      if (!enabled) {
        deactivateShield()
      } else if (_status.value == TimerStatus.RUNNING && _mode.value == TimerMode.FOCUS) {
        activateShield()
      }
    }
  }

  fun turnOffSession() {
    emergencyExit()
  }

  fun turnOffAudio() {
    audioEngine.stop()
  }

  private fun startTicker() {
    tickerJob?.cancel()
    tickerJob = viewModelScope.launch {
      while (_status.value == TimerStatus.RUNNING) {
        val now = System.currentTimeMillis()
        val remaining = ((targetEndTimeMillis - now + 999) / 1000).toInt()

        if (remaining <= 0) {
          _remainingSeconds.value = 0
          handleTimerFinished()
          break
        } else {
          _remainingSeconds.value = remaining
        }
        delay(500)
      }
    }
  }

  private fun handleTimerFinished() {
    if (!isCompletingSession.compareAndSet(false, true)) {
      return
    }

    _status.value = TimerStatus.COMPLETED
    deactivateShield()
    val now = System.currentTimeMillis()

    viewModelScope.launch {
      val record = recordCompletedSession(
        sessionId = activeSessionId,
        mode = _mode.value,
        durationSeconds = _totalDurationSeconds.value,
        start = startTimeMillis,
        end = now
      )
      _lastCompletedSession.value = record
      _showCompletionDialog.value = true

      notificationHelper.sendTimerCompleteNotification(
        title = "${_mode.value.displayName} Completed! 🎉",
        message = "You finished ${_totalDurationSeconds.value / 60} minutes of focus."
      )

      repository.clearActiveTimerState()

      val settings = timerSettings.value
      if (_mode.value == TimerMode.FOCUS && settings.autoStartBreaks) {
        setMode(TimerMode.SHORT_BREAK)
        startTimer()
      } else if (_mode.value != TimerMode.FOCUS && settings.autoStartFocus) {
        setMode(TimerMode.FOCUS)
        startTimer()
      }
    }
  }

  private suspend fun recordCompletedSession(
    sessionId: String,
    mode: TimerMode,
    durationSeconds: Int,
    start: Long,
    end: Long
  ): SessionRecord {
    val dateFormatted = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(start))
    val record = SessionRecord(
      sessionId = sessionId,
      sessionType = mode.name,
      plannedDurationSeconds = durationSeconds,
      actualDurationSeconds = durationSeconds,
      startTimestamp = start,
      endTimestamp = end,
      status = "COMPLETED",
      dateFormatted = dateFormatted
    )
    repository.recordSession(record)
    return record
  }

  private fun persistActiveTimerState() {
    viewModelScope.launch {
      val state = ActiveTimerStateEntity(
        id = 1,
        sessionId = activeSessionId,
        mode = _mode.value.name,
        status = _status.value.name,
        totalDurationSeconds = _totalDurationSeconds.value,
        remainingSeconds = _remainingSeconds.value,
        startTimeMillis = startTimeMillis,
        targetEndTimeMillis = targetEndTimeMillis,
        lastUpdatedMillis = System.currentTimeMillis(),
        hasBeenRecorded = (_status.value == TimerStatus.COMPLETED)
      )
      repository.saveActiveTimerState(state)
    }
  }

  fun deleteSession(id: Long) {
    viewModelScope.launch {
      repository.deleteSession(id)
    }
  }

  fun clearAllHistory() {
    viewModelScope.launch {
      repository.clearAllHistory()
    }
  }

  // --- Soundscape Audio Synthesizer ---
  val currentSoundscape: StateFlow<com.example.audio.SoundscapeType> = audioEngine.currentSoundscape
  val isAudioPlaying: StateFlow<Boolean> = audioEngine.isPlaying
  val audioVolume: StateFlow<Float> = audioEngine.volume

  fun selectSoundscape(soundscape: com.example.audio.SoundscapeType) {
    audioEngine.selectSoundscape(soundscape)
  }

  fun toggleAudio() {
    audioEngine.togglePlayback()
  }

  fun setAudioVolume(volume: Float) {
    audioEngine.setVolume(volume)
  }

  // --- Daily Motivational Quotes ---
  val currentQuote = MutableStateFlow(com.example.quotes.FocusQuotesRepository.getDailyQuote())

  fun refreshQuote() {
    currentQuote.value = com.example.quotes.FocusQuotesRepository.getRandomQuote()
  }

  // --- Mindful Box Breathing Reset ---
  private val _showBreathingDialog = MutableStateFlow(false)
  val showBreathingDialog: StateFlow<Boolean> = _showBreathingDialog.asStateFlow()

  fun openBreathingDialog() {
    _showBreathingDialog.value = true
  }

  fun closeBreathingDialog() {
    _showBreathingDialog.value = false
  }

  // --- Quick Focus Presets ---
  fun applyPreset(preset: com.example.presets.FocusPreset, autoStart: Boolean = false) {
    if (_status.value == TimerStatus.RUNNING && isStrictModeEnabled.value) {
      return
    }
    resetTimer()
    _mode.value = TimerMode.FOCUS
    val durationSec = preset.focusMinutes * 60
    _totalDurationSeconds.value = durationSec
    _remainingSeconds.value = durationSec
    setStrictModeEnabled(preset.isStrictMode)
    if (autoStart) {
      startTimer()
    }
  }

  // --- Scheduled Focus Blocks ---
  val scheduledBlocks: StateFlow<List<com.example.data.local.entity.ScheduledFocusEntity>> =
    scheduleRepository.allScheduled
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun addScheduledBlock(title: String, start: String, end: String, days: String, strict: Boolean = true) {
    viewModelScope.launch {
      scheduleRepository.addScheduledBlock(title, start, end, days, strict)
    }
  }

  fun toggleScheduledBlock(id: Long, isEnabled: Boolean) {
    viewModelScope.launch {
      scheduleRepository.toggleEnabled(id, isEnabled)
    }
  }

  fun deleteScheduledBlock(block: com.example.data.local.entity.ScheduledFocusEntity) {
    viewModelScope.launch {
      scheduleRepository.delete(block)
    }
  }

  override fun onCleared() {
    super.onCleared()
    focusShieldMonitor.stopMonitoring()
    audioEngine.stop()
  }
}
