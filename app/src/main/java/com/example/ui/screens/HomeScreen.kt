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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.navigation.Screen
import com.example.timer.FocusTimerViewModel
import com.example.timer.TimerMode
import com.example.timer.TimerStatus
import com.example.ui.components.MetricCard
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

private const val DAILY_GOAL_MINUTES = 120

@Composable
fun HomeScreen(
  viewModel: FocusTimerViewModel,
  onNavigateToScreen: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  val mode by viewModel.mode.collectAsStateWithLifecycle()
  val status by viewModel.status.collectAsStateWithLifecycle()
  val totalSeconds by viewModel.totalDurationSeconds.collectAsStateWithLifecycle()
  val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()

  val todayFocusMinutes by viewModel.todayFocusMinutes.collectAsStateWithLifecycle()
  val todaySessionsCount by viewModel.todayCompletedSessionsCount.collectAsStateWithLifecycle()
  val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
  val isShieldActive by viewModel.isShieldActive.collectAsStateWithLifecycle()
  val blockedAppCount by viewModel.blockedAppCount.collectAsStateWithLifecycle()

  // Gentle breathing glow effect for the focus ring
  val infiniteTransition = rememberInfiniteTransition(label = "home_ring_pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.65f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "home_pulse_alpha"
  )

  val currentQuote by viewModel.currentQuote.collectAsStateWithLifecycle()
  val currentSoundscape by viewModel.currentSoundscape.collectAsStateWithLifecycle()
  val isAudioPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
  val audioVolume by viewModel.audioVolume.collectAsStateWithLifecycle()
  val showBreathingDialog by viewModel.showBreathingDialog.collectAsStateWithLifecycle()

  if (showBreathingDialog) {
    com.example.mindful.BreathingExerciseDialog(
      onDismiss = { viewModel.closeBreathingDialog() }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // 1 & 2. Header Area: App Title, Greeting and Live Shield Status
    HomeHeader(
      isShieldActive = isShieldActive,
      blockedAppCount = blockedAppCount
    )

    // 3. Circular Focus-Timer Display
    CircularTimerSection(
      mode = mode,
      status = status,
      totalSeconds = totalSeconds,
      remainingSeconds = remainingSeconds,
      isShieldActive = isShieldActive,
      blockedAppCount = blockedAppCount,
      pulseAlpha = if (status == TimerStatus.RUNNING) pulseAlpha else 1.0f,
      onOpenTimer = { onNavigateToScreen(Screen.Focus) }
    )

    // 4. Prominent Action Button
    HomeActionButton(
      status = status,
      onAction = {
        if (status == TimerStatus.RUNNING) {
          viewModel.pauseTimer()
        } else {
          viewModel.startTimer()
          onNavigateToScreen(Screen.Focus)
        }
      }
    )

    if (status == TimerStatus.RUNNING || status == TimerStatus.PAUSED) {
      Button(
        onClick = { viewModel.turnOffSession() },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("home_turn_off_focus_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
          contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
      ) {
        Icon(
          imageVector = Icons.Default.Stop,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Turn OFF Focus Session (Stop)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.error
        )
      }
    }

    // 4b. Quick Focus Presets (1-tap setup)
    com.example.ui.components.FocusPresetsRow(
      onSelectPreset = { preset ->
        viewModel.applyPreset(preset, autoStart = true)
        onNavigateToScreen(Screen.Focus)
      }
    )

    // 4c. Ambient Focus Soundscape Player
    com.example.ui.components.SoundscapePlayerCard(
      currentSoundscape = currentSoundscape,
      isPlaying = isAudioPlaying,
      volume = audioVolume,
      onSelectSoundscape = { sound -> viewModel.selectSoundscape(sound) },
      onTogglePlay = { viewModel.toggleAudio() },
      onVolumeChange = { vol -> viewModel.setAudioVolume(vol) }
    )

    // 4d. Pro Smart Shield Hub (Flip-to-Focus #4, Bedtime Sleep Shield #5, Smart Unlock Challenge #1)
    com.example.ui.components.SmartFeaturesHubSection(
      isTimerRunning = (status == TimerStatus.RUNNING),
      onAutoStartFocusFromFlip = {
        if (status != TimerStatus.RUNNING) {
          viewModel.startTimer()
        }
      },
      onActivateSleepShieldNow = {
        viewModel.setMode(TimerMode.FOCUS)
        viewModel.setCustomDuration(60)
        viewModel.startTimer()
        onNavigateToScreen(Screen.Focus)
      }
    )

    // 5. Today's Focus-Time Card
    MetricCard(
      title = "Today's Focus Time",
      value = "$todayFocusMinutes min",
      subtitle = if (todayFocusMinutes > 0) "Tracked across today's deep work" else "Start a session to log focus time",
      icon = Icons.Default.Timer,
      iconTint = ElectricBlue,
      badgeText = if (todayFocusMinutes > 0) "Today" else "Ready",
      testTag = "card_today_focus"
    )

    // 6. Daily Goal Progress Card (Live minutes against target goal of 120 minutes)
    DailyGoalCard(
      currentMinutes = todayFocusMinutes,
      goalMinutes = DAILY_GOAL_MINUTES
    )

    // 7. Small Cards for Completed Sessions (Live) and Current Streak (Live)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      MetricCard(
        title = "Sessions",
        value = "$todaySessionsCount",
        subtitle = "Completed today",
        icon = Icons.Default.TaskAlt,
        iconTint = SuccessEmerald,
        badgeText = if (todaySessionsCount > 0) "Done" else null,
        modifier = Modifier.weight(1f),
        testTag = "card_sessions"
      )

      MetricCard(
        title = "Streak",
        value = "$currentStreak ${if (currentStreak == 1) "day" else "days"}",
        subtitle = "Consistent focus",
        icon = Icons.Default.LocalFireDepartment,
        iconTint = AmberStreak,
        badgeText = null,
        modifier = Modifier.weight(1f),
        testTag = "card_streak"
      )
    }

    // 7b. Daily Inspirational Focus Mantra
    com.example.ui.components.DailyMantraCard(
      quote = currentQuote,
      onRefreshQuote = { viewModel.refreshQuote() }
    )

    // 8. Quick Actions Section
    QuickActionsSection(
      onNavigateToScreen = onNavigateToScreen
    )

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun HomeHeader(
  isShieldActive: Boolean,
  blockedAppCount: Int
) {
  val hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
  val greeting = when {
    hourOfDay < 12 -> "Good Morning • Deep Work"
    hourOfDay < 17 -> "Good Afternoon • Stay Focused"
    else -> "Good Evening • Focus Zone"
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "FOCUS SHIELD PRO",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold
      )

      Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isShieldActive) SuccessEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
          1.dp,
          if (isShieldActive) SuccessEmerald.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
      ) {
        Text(
          text = if (isShieldActive) {
            "SHIELD ON • $blockedAppCount APPS"
          } else if (blockedAppCount > 0) {
            "$blockedAppCount APPS ARMED"
          } else {
            "SHIELD READY"
          },
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (isShieldActive) SuccessEmerald else MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
    }

    Text(
      text = greeting,
      style = MaterialTheme.typography.headlineLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Text(
      text = "Lock distracting apps and command your undivided attention.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun CircularTimerSection(
  mode: TimerMode,
  status: TimerStatus,
  totalSeconds: Int,
  remainingSeconds: Int,
  isShieldActive: Boolean,
  blockedAppCount: Int,
  pulseAlpha: Float,
  onOpenTimer: () -> Unit
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
      .padding(vertical = 12.dp)
      .clickable(onClick = onOpenTimer),
    contentAlignment = Alignment.Center
  ) {
    val ringSize = 250.dp
    val primaryColor = if (mode == TimerMode.FOCUS) RadiantViolet else ElectricBlue
    val secondaryColor = if (mode == TimerMode.FOCUS) ElectricBlue else RadiantViolet
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
      modifier = Modifier
        .size(ringSize)
        .testTag("circular_timer_canvas")
    ) {
      val strokeWidth = 10.dp.toPx()
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

      // Active Arc
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

    // Inside Content
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
          fontWeight = FontWeight.SemiBold
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

      val label = when (status) {
        TimerStatus.IDLE -> if (blockedAppCount > 0) "Shield ready ($blockedAppCount apps) • Tap to start" else "Tap to open timer"
        TimerStatus.RUNNING -> if (isShieldActive) "Session active • Shield ($blockedAppCount apps)" else "Session in progress"
        TimerStatus.PAUSED -> "Session paused"
        TimerStatus.COMPLETED -> "Session completed"
      }

      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = if (status == TimerStatus.RUNNING) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun HomeActionButton(
  status: TimerStatus,
  onAction: () -> Unit
) {
  Button(
    onClick = onAction,
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .testTag("start_focus_button"),
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = if (status == TimerStatus.RUNNING) MaterialTheme.colorScheme.surfaceVariant else RadiantViolet,
      contentColor = if (status == TimerStatus.RUNNING) MaterialTheme.colorScheme.onSurface else Color.White
    ),
    border = if (status == TimerStatus.RUNNING) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = if (status == TimerStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = if (status == TimerStatus.RUNNING) RadiantViolet else Color.White
      )
      Text(
        text = when (status) {
          TimerStatus.RUNNING -> "Pause Active Focus"
          TimerStatus.PAUSED -> "Resume Focus"
          TimerStatus.COMPLETED -> "Start Next Session"
          TimerStatus.IDLE -> "Start Focus"
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun DailyGoalCard(
  currentMinutes: Int,
  goalMinutes: Int
) {
  val progress = (currentMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
  val percent = (progress * 100).toInt()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_daily_goal"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Daily Goal Progress",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Target: $goalMinutes minutes per day",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Text(
            text = "$currentMinutes / $goalMinutes min",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = ElectricBlue,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "$percent% completed",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = if (currentMinutes >= goalMinutes) "Daily goal achieved! 🔥" else "${(goalMinutes - currentMinutes).coerceAtLeast(0)} min remaining",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.tertiary
        )
      }
    }
  }
}

@Composable
private fun QuickActionsSection(
  onNavigateToScreen: (Screen) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Workspace & Shield Controls",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    // 1. Focus Sessions Action
    QuickActionCard(
      title = "Focus Timer & Presets",
      description = "Pomodoro intervals, custom durations & focus tree",
      icon = Icons.Default.Timer,
      iconTint = RadiantViolet,
      onClick = { onNavigateToScreen(Screen.Focus) },
      testTag = "action_focus_sessions"
    )

    // 2. Installed Apps & Shield Action
    QuickActionCard(
      title = "App Shield & Blocked Apps",
      description = "Choose distracting apps to lock during active focus",
      icon = Icons.Default.Security,
      iconTint = ElectricBlue,
      onClick = { onNavigateToScreen(Screen.AppBlocker) },
      testTag = "action_app_blocker"
    )

    // 3. Usage Reports Action
    QuickActionCard(
      title = "Focus Analytics & Daily Planner",
      description = "7-day charts, task planner, milestones & focus coach",
      icon = Icons.Default.ShowChart,
      iconTint = CyanAccent,
      onClick = { onNavigateToScreen(Screen.Statistics) },
      testTag = "action_usage_reports"
    )
  }
}

@Composable
private fun QuickActionCard(
  title: String,
  description: String,
  icon: ImageVector,
  iconTint: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier.size(44.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = iconTint,
              modifier = Modifier.size(22.dp)
            )
          }
        }

        Column {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = "Navigate to $title",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
