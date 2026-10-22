package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timer.FocusTimerViewModel
import com.example.timer.TimerMode
import com.example.timer.TimerStatus
import com.example.ui.screens.focus.CancelConfirmationDialog
import com.example.ui.screens.focus.CustomDurationDialog
import com.example.ui.screens.focus.DistractionInterventionDialog
import com.example.ui.screens.focus.SessionCompletedDialog
import com.example.ui.screens.focus.SessionHistorySection
import com.example.ui.screens.focus.StrictModeCancelDialog
import com.example.ui.screens.focus.StrictModeExplanationDialog
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.screens.apps.OverlayAccessExplanationDialog
import com.example.garden.FocusTreeWidget
import com.example.mindful.BreathingExerciseDialog
import com.example.shield.ShieldDiagnostics
import com.example.ui.components.DailyMantraCard
import com.example.ui.components.FocusPresetsRow
import com.example.ui.components.SoundscapePlayerCard
import com.example.ui.screens.focus.ScheduledFocusDialog
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

@Composable
fun FocusScreen(
  viewModel: FocusTimerViewModel,
  onNavigateHome: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  val mode by viewModel.mode.collectAsStateWithLifecycle()
  val status by viewModel.status.collectAsStateWithLifecycle()
  val totalSeconds by viewModel.totalDurationSeconds.collectAsStateWithLifecycle()
  val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
  val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

  val isShieldActive by viewModel.isShieldActive.collectAsStateWithLifecycle()
  val blockedAppCount by viewModel.blockedAppCount.collectAsStateWithLifecycle()
  val isStrictModeEnabled by viewModel.isStrictModeEnabled.collectAsStateWithLifecycle()
  val distractionAlertAppName by viewModel.distractionAlertAppName.collectAsStateWithLifecycle()

  val showCancelDialog by viewModel.showCancelDialog.collectAsStateWithLifecycle()
  val showStrictCancelDialog by viewModel.showStrictCancelDialog.collectAsStateWithLifecycle()
  val showCustomDialog by viewModel.showCustomDurationDialog.collectAsStateWithLifecycle()
  val showCompletionDialog by viewModel.showCompletionDialog.collectAsStateWithLifecycle()
  val lastCompletedSession by viewModel.lastCompletedSession.collectAsStateWithLifecycle()

  val currentSoundscape by viewModel.currentSoundscape.collectAsStateWithLifecycle()
  val isAudioPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
  val audioVolume by viewModel.audioVolume.collectAsStateWithLifecycle()
  val currentQuote by viewModel.currentQuote.collectAsStateWithLifecycle()
  val showBreathingDialog by viewModel.showBreathingDialog.collectAsStateWithLifecycle()
  val scheduledBlocks by viewModel.scheduledBlocks.collectAsStateWithLifecycle()
  var showScheduledDialog by remember { mutableStateOf(false) }

  var showStrictModeExplainDialog by remember { mutableStateOf(false) }

  val timerProgress = if (totalSeconds > 0) {
    ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
  } else 0f

  // Gentle pulsing animation when running
  val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.70f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  // Dialogs
  if (showCustomDialog) {
    CustomDurationDialog(
      initialMinutes = totalSeconds / 60,
      onDismiss = { viewModel.dismissCustomDurationDialog() },
      onConfirm = { minutes -> viewModel.setCustomDuration(minutes) }
    )
  }

  if (showCancelDialog) {
    CancelConfirmationDialog(
      onDismiss = { viewModel.dismissCancelDialog() },
      onConfirm = { viewModel.confirmCancel() }
    )
  }

  if (showStrictCancelDialog) {
    StrictModeCancelDialog(
      onDismiss = { viewModel.dismissCancelDialog() },
      onConfirmedCancel = { viewModel.confirmCancel() },
      onEmergencyExit = { viewModel.emergencyExit() }
    )
  }

  if (showStrictModeExplainDialog) {
    StrictModeExplanationDialog(
      onDismiss = { showStrictModeExplainDialog = false },
      onConfirm = {
        showStrictModeExplainDialog = false
        viewModel.setStrictModeEnabled(true)
      }
    )
  }

  var showOverlayTroubleshootDialog by remember { mutableStateOf(false) }

  if (showOverlayTroubleshootDialog) {
    OverlayAccessExplanationDialog(
      onDismiss = { showOverlayTroubleshootDialog = false },
      onOpenOverlaySettings = { viewModel.openOverlaySettings() },
      onOpenAppDetailsSettings = { viewModel.openAppDetailsSettings() },
      onOpenOverlayListSettings = { viewModel.openOverlaySettingsList() }
    )
  }

  val isUsageAccessGranted by viewModel.isUsageAccessGranted.collectAsStateWithLifecycle()
  val diagnostics by viewModel.shieldDiagnostics.collectAsStateWithLifecycle()

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.checkUsageAccessPermission()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  if (distractionAlertAppName != null) {
    DistractionInterventionDialog(
      appName = distractionAlertAppName ?: "Application",
      isStrictMode = isStrictModeEnabled,
      onResumeFocus = { viewModel.dismissDistractionAlert() },
      onRequestCancel = {
        viewModel.dismissDistractionAlert()
        viewModel.requestCancel()
      }
    )
  }

  if (showCompletionDialog) {
    SessionCompletedDialog(
      session = lastCompletedSession,
      onDismiss = { viewModel.dismissCompletionDialog() }
    )
  }

  if (showBreathingDialog) {
    BreathingExerciseDialog(
      onDismiss = { viewModel.closeBreathingDialog() }
    )
  }

  if (showScheduledDialog) {
    ScheduledFocusDialog(
      scheduledBlocks = scheduledBlocks,
      onAddBlock = { title, start, end, days ->
        viewModel.addScheduledBlock(title, start, end, days)
      },
      onToggleBlock = { id, enabled ->
        viewModel.toggleScheduledBlock(id, enabled)
      },
      onDeleteBlock = { block ->
        viewModel.deleteScheduledBlock(block)
      },
      onDismiss = { showScheduledDialog = false }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // 1. Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onNavigateHome,
          modifier = Modifier.testTag("focus_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Home",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
        Text(
          text = "Focus Timer",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
          onClick = { viewModel.openBreathingDialog() },
          modifier = Modifier.testTag("btn_header_breathing")
        ) {
          Icon(
            imageVector = Icons.Default.Spa,
            contentDescription = "Mindful Breathing Reset",
            tint = CyanAccent,
            modifier = Modifier.size(22.dp)
          )
        }

        IconButton(
          onClick = { showScheduledDialog = true },
          modifier = Modifier.testTag("btn_header_schedule")
        ) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "Scheduled Focus",
            tint = ElectricBlue,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }

    // 2. Focus Presets (1-tap setup for Pomodoro, Deep Work, Study, Sprint, Monk Mode)
    FocusPresetsRow(
      onSelectPreset = { preset -> viewModel.applyPreset(preset) }
    )

    // 3. Mode Selector (Focus / Short Break / Long Break)
    ModeSelectorRow(
      selectedMode = mode,
      isTimerActive = (status == TimerStatus.RUNNING || status == TimerStatus.PAUSED),
      onSelectMode = { newMode -> viewModel.setMode(newMode) }
    )

    // 4. Preset Duration Pills (Selectable presets & custom option)
    PresetDurationRow(
      mode = mode,
      currentTotalMinutes = totalSeconds / 60,
      isTimerActive = (status == TimerStatus.RUNNING || status == TimerStatus.PAUSED),
      onSelectPreset = { minutes -> viewModel.setPresetDuration(minutes) },
      onOpenCustomDialog = { viewModel.openCustomDurationDialog() }
    )

    // 5. Large Circular Timer Display
    CircularTimerDisplay(
      mode = mode,
      status = status,
      totalSeconds = totalSeconds,
      remainingSeconds = remainingSeconds,
      pulseAlpha = if (status == TimerStatus.RUNNING) pulseAlpha else 1.0f
    )

    // 6. Interactive Focus Tree Garden Growth
    FocusTreeWidget(
      progress = timerProgress,
      isWithered = false
    )

    // 7. Timer Controls (Start, Pause, Resume, Reset, Cancel)
    TimerControlsSection(
      status = status,
      onStart = { viewModel.startTimer() },
      onPause = { viewModel.pauseTimer() },
      onResume = { viewModel.resumeTimer() },
      onCancel = { viewModel.requestCancel() },
      onReset = { viewModel.resetTimer() }
    )

    // 8. Focus Ambient Soundscape Player (Synthesized AudioTrack)
    SoundscapePlayerCard(
      currentSoundscape = currentSoundscape,
      isPlaying = isAudioPlaying,
      volume = audioVolume,
      onSelectSoundscape = { sound -> viewModel.selectSoundscape(sound) },
      onTogglePlay = { viewModel.toggleAudio() },
      onVolumeChange = { vol -> viewModel.setAudioVolume(vol) }
    )

    // 9. Shield & Strict Mode Controls Card
    FocusShieldStatusCard(
      mode = mode,
      status = status,
      isShieldActive = isShieldActive,
      blockedAppCount = blockedAppCount,
      isStrictModeEnabled = isStrictModeEnabled,
      isUsageAccessGranted = isUsageAccessGranted,
      diagnostics = diagnostics,
      onRequestUsagePermission = {
        viewModel.openUsageAccessSettings()
      },
      onRequestOverlayPermission = {
        viewModel.openOverlaySettings()
      },
      onShowOverlayHelp = {
        showOverlayTroubleshootDialog = true
      },
      onToggleStrictMode = { requested ->
        if (requested) {
          showStrictModeExplainDialog = true
        } else {
          viewModel.setStrictModeEnabled(false)
        }
      }
    )

    // 10. Daily Inspirational Focus Mantra
    DailyMantraCard(
      quote = currentQuote,
      onRefreshQuote = { viewModel.refreshQuote() }
    )

    Spacer(modifier = Modifier.height(4.dp))

    // 11. Session History Section
    SessionHistorySection(
      sessions = allSessions,
      onDeleteSession = { id -> viewModel.deleteSession(id) },
      onClearAllHistory = { viewModel.clearAllHistory() }
    )

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun FocusShieldStatusCard(
  mode: TimerMode,
  status: TimerStatus,
  isShieldActive: Boolean,
  blockedAppCount: Int,
  isStrictModeEnabled: Boolean,
  isUsageAccessGranted: Boolean,
  diagnostics: ShieldDiagnostics,
  onRequestUsagePermission: () -> Unit,
  onRequestOverlayPermission: () -> Unit,
  onShowOverlayHelp: () -> Unit,
  onToggleStrictMode: (Boolean) -> Unit
) {
  var showDiagnostics by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("shield_status_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Focus Shield Status Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = if (isShieldActive) SuccessEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = if (isShieldActive) SuccessEmerald else RadiantViolet,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Column {
            Text(
              text = if (isShieldActive) "Focus Shield Active" else "Focus Shield Ready",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = when {
                !isUsageAccessGranted -> "Permission missing • Detection disabled"
                isShieldActive -> "$blockedAppCount apps restricted • Live foreground intervention"
                blockedAppCount > 0 -> "$blockedAppCount apps configured for restriction"
                else -> "No apps selected • Choose apps in Apps tab"
              },
              style = MaterialTheme.typography.bodySmall,
              color = if (!isUsageAccessGranted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = when {
            !isUsageAccessGranted -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            isShieldActive -> SuccessEmerald.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
          }
        ) {
          Text(
            text = when {
              !isUsageAccessGranted -> "NEEDS PERM"
              isShieldActive -> "ACTIVE"
              else -> "$blockedAppCount APPS"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when {
              !isUsageAccessGranted -> MaterialTheme.colorScheme.error
              isShieldActive -> SuccessEmerald
              else -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Permission Warning Banner if Usage Access is disabled
      if (!isUsageAccessGranted) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AmberStreak.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, AmberStreak.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = AmberStreak,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Usage Access needed to detect restricted apps during focus.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Button(
              onClick = onRequestUsagePermission,
              colors = ButtonDefaults.buttonColors(containerColor = AmberStreak),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("enable_usage_access_shield_button")
            ) {
              Text(
                text = "Enable",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
              )
            }
          }
        }
      }

      // Permission Warning Banner if Display Over Other Apps is disabled
      if (!diagnostics.isOverlayPermissionGranted) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AmberStreak.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, AmberStreak.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = AmberStreak,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Display Over Apps needed to show 60s blocking screen.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Button(
                onClick = onRequestOverlayPermission,
                colors = ButtonDefaults.buttonColors(containerColor = AmberStreak),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("enable_overlay_shield_button")
              ) {
                Text(
                  text = "Enable",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.Black
                )
              }
            }

            // Quick troubleshooting hint for Restricted Settings
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Turns OFF automatically in Settings?",
                style = MaterialTheme.typography.labelSmall,
                color = AmberStreak,
                fontWeight = FontWeight.Medium
              )
              TextButton(
                onClick = onShowOverlayHelp,
                modifier = Modifier.testTag("overlay_troubleshoot_focus_button")
              ) {
                Text(
                  text = "Fix / 3 Dots (⋮)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = RadiantViolet
                )
              }
            }
          }
        }
      }

      // Divider
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp),
        color = MaterialTheme.colorScheme.outlineVariant
      ) {}

      // Strict Mode Switch Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = if (isStrictModeEnabled) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
          Column {
            Text(
              text = "Strict Mode",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Hold-to-cancel & exit hurdles to prevent giving up",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Switch(
          checked = isStrictModeEnabled,
          onCheckedChange = onToggleStrictMode,
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = RadiantViolet,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
          ),
          modifier = Modifier.testTag("strict_mode_switch")
        )
      }

      // Diagnostics Toggle & Panel
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "Shield Diagnostics",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        TextButton(
          onClick = { showDiagnostics = !showDiagnostics },
          modifier = Modifier.testTag("toggle_shield_diagnostics")
        ) {
          Text(
            text = if (showDiagnostics) "Hide" else "Inspect",
            style = MaterialTheme.typography.labelSmall
          )
        }
      }

      if (showDiagnostics) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            DiagnosticItem("Focus Session", if (status == TimerStatus.RUNNING) "Active (${if (isStrictModeEnabled) "Strict" else "Standard"})" else "Inactive")
            DiagnosticItem("Restricted Apps", "${diagnostics.blockedPackageCount} packages loaded")
            DiagnosticItem("Usage Access", if (diagnostics.isUsageAccessGranted) "Granted" else "Missing (Action Needed)")
            DiagnosticItem("Display Over Apps", if (diagnostics.isOverlayPermissionGranted) "Granted" else "Missing (Action Needed)")
            DiagnosticItem("Notification Access", if (diagnostics.isNotificationPermissionGranted) "Granted" else "Missing")
            DiagnosticItem("Blocking Service", if (diagnostics.isServiceActive) "Running (Foreground Service)" else "Inactive")
            DiagnosticItem("Protection Capability", diagnostics.capabilityLevel)
            DiagnosticItem("Last Blocking Event", diagnostics.lastDetectedEvent ?: "None yet")
            DiagnosticItem("Status Message", diagnostics.statusMessage)
          }
        }
      }
    }
  }
}

@Composable
private fun DiagnosticItem(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun ModeSelectorRow(
  selectedMode: TimerMode,
  isTimerActive: Boolean,
  onSelectMode: (TimerMode) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    TimerMode.values().forEach { mode ->
      val isSelected = mode == selectedMode
      val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceContainerHigh
      } else {
        MaterialTheme.colorScheme.surfaceVariant
      }
      val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(
          1.dp,
          if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .clickable(enabled = !isTimerActive) { onSelectMode(mode) }
          .testTag("mode_tab_${mode.name}")
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.padding(vertical = 10.dp)
        ) {
          Text(
            text = mode.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
          )
        }
      }
    }
  }
}

@Composable
private fun PresetDurationRow(
  mode: TimerMode,
  currentTotalMinutes: Int,
  isTimerActive: Boolean,
  onSelectPreset: (Int) -> Unit,
  onOpenCustomDialog: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    mode.presets.forEach { presetMinutes ->
      val isSelected = presetMinutes == currentTotalMinutes
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) RadiantViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
          1.dp,
          if (isSelected) RadiantViolet else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(10.dp))
          .clickable(enabled = !isTimerActive) { onSelectPreset(presetMinutes) }
          .testTag("preset_${presetMinutes}m")
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.padding(vertical = 8.dp)
        ) {
          Text(
            text = "${presetMinutes}m",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Custom Duration Button
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier
        .clip(RoundedCornerShape(10.dp))
        .clickable(enabled = !isTimerActive) { onOpenCustomDialog() }
        .testTag("preset_custom")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.MoreTime,
          contentDescription = "Custom duration",
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "Custom",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.secondary
        )
      }
    }
  }
}

@Composable
private fun CircularTimerDisplay(
  mode: TimerMode,
  status: TimerStatus,
  totalSeconds: Int,
  remainingSeconds: Int,
  pulseAlpha: Float
) {
  val minutes = remainingSeconds / 60
  val seconds = remainingSeconds % 60
  val formattedTime = String.format("%02d:%02d", minutes, seconds)

  val progress = if (totalSeconds > 0) {
    (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
  } else 1f

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    val ringSize = 260.dp
    val primaryColor = if (mode == TimerMode.FOCUS) RadiantViolet else ElectricBlue
    val secondaryColor = if (mode == TimerMode.FOCUS) ElectricBlue else RadiantViolet
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
      modifier = Modifier
        .size(ringSize)
        .testTag("circular_timer_canvas")
    ) {
      val strokeWidth = 12.dp.toPx()
      val arcSize = size.width - strokeWidth
      val topLeftOffset = strokeWidth / 2

      // Background Track
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(topLeftOffset, topLeftOffset),
        size = Size(arcSize, arcSize),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      // Active Remaining Arc
      val sweepAngle = progress * 360f
      drawArc(
        brush = Brush.sweepGradient(
          0.0f to secondaryColor.copy(alpha = pulseAlpha),
          0.5f to primaryColor.copy(alpha = pulseAlpha),
          1.0f to secondaryColor.copy(alpha = pulseAlpha)
        ),
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(topLeftOffset, topLeftOffset),
        size = Size(arcSize, arcSize),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )
    }

    // Inner Content
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(bottom = 8.dp)
      ) {
        Text(
          text = "${mode.displayName.uppercase()} • ${totalSeconds / 60} MIN",
          style = MaterialTheme.typography.labelSmall,
          color = primaryColor,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          letterSpacing = 1.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Text(
        text = formattedTime,
        style = MaterialTheme.typography.displayLarge.copy(
          fontFamily = FontFamily.Monospace,
          letterSpacing = (-1.5).sp
        ),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(4.dp))

      val statusText = when (status) {
        TimerStatus.IDLE -> "Ready to start"
        TimerStatus.RUNNING -> "Session active"
        TimerStatus.PAUSED -> "Session paused"
        TimerStatus.COMPLETED -> "Session completed"
      }

      Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        color = if (status == TimerStatus.RUNNING) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun TimerControlsSection(
  status: TimerStatus,
  onStart: () -> Unit,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onCancel: () -> Unit,
  onReset: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    when (status) {
      TimerStatus.IDLE, TimerStatus.COMPLETED -> {
        Button(
          onClick = onStart,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("start_timer_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = RadiantViolet,
            contentColor = Color.White
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text(
              text = if (status == TimerStatus.COMPLETED) "Start Next Session" else "Start Focus",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      TimerStatus.RUNNING -> {
        Button(
          onClick = onPause,
          modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .testTag("pause_timer_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Pause, contentDescription = null, tint = RadiantViolet)
            Text("Pause", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }

        OutlinedButton(
          onClick = onCancel,
          modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .testTag("cancel_timer_button"),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text("Cancel", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
          }
        }
      }

      TimerStatus.PAUSED -> {
        Button(
          onClick = onResume,
          modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .testTag("resume_timer_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = RadiantViolet,
            contentColor = Color.White
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text("Resume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }

        OutlinedButton(
          onClick = onCancel,
          modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .testTag("cancel_timer_button"),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text("Cancel", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
          }
        }

        IconButton(
          onClick = onReset,
          modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag("reset_timer_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Reset timer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}
