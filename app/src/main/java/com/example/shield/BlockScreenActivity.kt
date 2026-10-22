package com.example.shield

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ActiveTimerStateEntity
import com.example.data.local.entity.SessionRecord
import com.example.navigation.Screen
import com.example.quotes.FocusQuotesRepository
import com.example.timer.NotificationHelper
import com.example.timer.TimerStatus
import com.example.ui.theme.AdvancedFocusTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Premium Midnight & Neon Palette Tokens
private val MidnightBlack = Color(0xFF07080D)
private val DeepCanvasBlack = Color(0xFF0A0C14)
private val NeonPurple = Color(0xFF9D4EDD)
private val RadiantViolet = Color(0xFF8B5CF6)
private val ElectricCyan = Color(0xFF00E5FF)
private val CardSurface = Color(0xFF12141F)

class BlockScreenActivity : ComponentActivity() {

  companion object {
    const val EXTRA_BLOCKED_APP_NAME = "EXTRA_BLOCKED_APP_NAME"
    const val EXTRA_BLOCKED_PACKAGE_NAME = "EXTRA_BLOCKED_PACKAGE_NAME"
    const val EXTRA_IS_STRICT = "EXTRA_IS_STRICT"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Dismiss any active floating window manager overlay to avoid overlapping
    try {
      FocusBlockOverlayManager(applicationContext).hideBlockOverlay()
    } catch (_: Exception) {
    }

    val appName = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: "Restricted App"
    val isStrict = intent.getBooleanExtra(EXTRA_IS_STRICT, false)

    setContent {
      AdvancedFocusTheme {
        BlockScreenContent(
          blockedAppName = appName,
          isStrictMode = isStrict,
          onReturnToFocus = {
            returnToFocusApp(appName)
          },
          onGoToHomeScreen = {
            goToHomeScreen()
          },
          onEmergencyExit = {
            performEmergencyExit()
          }
        )
      }
    }
  }

  private fun returnToFocusApp(appName: String) {
    val focusIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(MainActivity.EXTRA_NAVIGATE_TO, Screen.Focus.route)
    }
    try {
      startActivity(focusIntent)
    } catch (e: Exception) {
      // Compliant fallback for Android background/restriction limitations
      NotificationHelper(applicationContext).sendRedirectToFocusNotification(appName)
    }
    finish()
  }

  private fun goToHomeScreen() {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(homeIntent)
    finish()
  }

  private fun performEmergencyExit() {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    coroutineScope.launch {
      try {
        val database = AppDatabase.getInstance(applicationContext)
        val activeTimer = database.activeTimerDao().getActiveTimerStateDirect()
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val planned: Int = activeTimer?.totalDurationSeconds ?: 1500
        val actual: Int = if (activeTimer != null) {
          val remaining = ((activeTimer.targetEndTimeMillis - now) / 1000).toInt().coerceAtLeast(0)
          (planned - remaining).coerceAtLeast(0)
        } else {
          0
        }

        val record = SessionRecord(
          sessionId = activeTimer?.sessionId ?: UUID.randomUUID().toString(),
          sessionType = activeTimer?.mode ?: "FOCUS",
          plannedDurationSeconds = planned,
          actualDurationSeconds = actual,
          startTimestamp = activeTimer?.startTimeMillis ?: (now - 60_000L),
          endTimestamp = now,
          status = "CANCELLED (Emergency Exit)",
          dateFormatted = dateFormat.format(Date(now))
        )
        database.sessionDao().insertSession(record)
        database.activeTimerDao().clearActiveTimerState()
      } catch (_: Exception) {
      } finally {
        FocusShieldService.stop(applicationContext)
        goToHomeScreen()
      }
    }
  }
}

/**
 * Premium, polished, full-screen blocked app interface with 60-second countdown
 * and automatic redirect to the existing Focus App.
 */
@Composable
fun BlockScreenContent(
  blockedAppName: String,
  isStrictMode: Boolean,
  onReturnToFocus: () -> Unit,
  onGoToHomeScreen: () -> Unit,
  onEmergencyExit: () -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  // Prevent returning to the blocked app via hardware back gesture
  BackHandler {
    onGoToHomeScreen()
  }

  // 1. Stable 60-Second Countdown Calculation:
  // Using rememberSaveable with SystemClock.elapsedRealtime ensures recompositions
  // or configuration events NEVER restart or drift the 60-second timer.
  val countdownTotalSeconds = 60
  val countdownEndEpoch = rememberSaveable { SystemClock.elapsedRealtime() + (countdownTotalSeconds * 1000L) }
  var remainingSeconds by remember {
    mutableIntStateOf(
      ((countdownEndEpoch - SystemClock.elapsedRealtime() + 999) / 1000).toInt().coerceIn(0, countdownTotalSeconds)
    )
  }

  var hasTriggeredRedirect by remember { mutableStateOf(false) }

  // 2. Real Focus Session Observer:
  // Preserves existing Focus session state and verifies that the session is still active
  var activeSessionState by remember { mutableStateOf<ActiveTimerStateEntity?>(null) }
  var isSessionActiveInDb by remember { mutableStateOf(true) }
  var isSessionPausedInDb by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      val database = AppDatabase.getInstance(context)
      val initial = database.activeTimerDao().getActiveTimerStateDirect()
      withContext(Dispatchers.Main) {
        activeSessionState = initial
        isSessionActiveInDb = (initial != null)
        isSessionPausedInDb = (initial?.status == TimerStatus.PAUSED.name)
      }
    }

    // Monitor for early session completion or cancellation in background
    while (isActive) {
      delay(1000L)
      withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val current = database.activeTimerDao().getActiveTimerStateDirect()
        withContext(Dispatchers.Main) {
          activeSessionState = current
          isSessionActiveInDb = (current != null)
          isSessionPausedInDb = (current?.status == TimerStatus.PAUSED.name)
        }
      }
    }
  }

  // 3. 60-Second Countdown Ticker & Single Automatic Redirect
  LaunchedEffect(countdownEndEpoch, isSessionActiveInDb) {
    while (isActive && isSessionActiveInDb) {
      val remaining = ((countdownEndEpoch - SystemClock.elapsedRealtime() + 999) / 1000).toInt().coerceIn(0, countdownTotalSeconds)
      remainingSeconds = remaining

      if (remaining <= 0) {
        if (!hasTriggeredRedirect) {
          hasTriggeredRedirect = true
          delay(400L) // Brief pause to show completion state cleanly
          onReturnToFocus()
        }
        break
      }
      delay(250L) // Responsive check without CPU spike
    }
  }

  // 4. Subtle, battery-friendly ambient glow animation
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.45f,
    targetValue = 0.90f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  // Curated motivational quote
  val motivationalQuote = remember {
    FocusQuotesRepository.getRandomQuote()
  }

  // Emergency hold controls (for strict mode)
  var isHoldingEmergency by remember { mutableStateOf(false) }
  var holdProgress by remember { mutableFloatStateOf(0f) }
  var showEmergencyDetails by remember { mutableStateOf(false) }

  LaunchedEffect(isHoldingEmergency) {
    if (isHoldingEmergency) {
      val totalSteps = 30 // 3 seconds at 100ms intervals
      for (step in 1..totalSteps) {
        delay(100L)
        if (!isHoldingEmergency) {
          holdProgress = 0f
          break
        }
        holdProgress = step.toFloat() / totalSteps.toFloat()
      }
      if (isHoldingEmergency && holdProgress >= 1f) {
        onEmergencyExit()
      }
    } else {
      holdProgress = 0f
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MidnightBlack
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              MidnightBlack,
              DeepCanvasBlack,
              MidnightBlack
            )
          )
        )
        .padding(innerPadding)
        .padding(horizontal = 20.dp, vertical = 16.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp)
          .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. FOCUS MODE ACTIVE STATUS PILL
        FocusActiveIndicatorPill(
          isStrictMode = isStrictMode,
          isSessionActive = isSessionActiveInDb,
          isSessionPaused = isSessionPausedInDb,
          pulseAlpha = pulseAlpha
        )

        // 2. PROMINENT ANIMATED LOCK ILLUSTRATION & COUNTDOWN PROGRESS RING
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(210.dp)
            .padding(8.dp)
        ) {
          val progressFraction = (remainingSeconds.toFloat() / countdownTotalSeconds.toFloat()).coerceIn(0f, 1f)

          // Glowing canvas progress ring
          Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val arcSize = size.width - strokeWidth
            val offset = strokeWidth / 2

            // Background subtle track
            drawArc(
              color = NeonPurple.copy(alpha = 0.15f),
              startAngle = 0f,
              sweepAngle = 360f,
              useCenter = false,
              topLeft = Offset(offset, offset),
              size = Size(arcSize, arcSize),
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Neon Purple to Electric Cyan gradient active progress arc
            drawArc(
              brush = Brush.sweepGradient(
                0.0f to ElectricCyan.copy(alpha = pulseAlpha),
                0.5f to NeonPurple.copy(alpha = pulseAlpha),
                1.0f to ElectricCyan.copy(alpha = pulseAlpha)
              ),
              startAngle = -90f,
              sweepAngle = 360f * progressFraction,
              useCenter = false,
              topLeft = Offset(offset, offset),
              size = Size(arcSize, arcSize),
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
          }

          // Inner Animated Lock & Central Countdown Timer
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Surface(
              shape = CircleShape,
              color = CardSurface.copy(alpha = 0.95f),
              border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                  listOf(
                    NeonPurple.copy(alpha = pulseAlpha),
                    ElectricCyan.copy(alpha = pulseAlpha)
                  )
                )
              ),
              modifier = Modifier.size(72.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isStrictMode) Icons.Default.Lock else Icons.Default.Security,
                  contentDescription = "Lock Illustration",
                  tint = if (isStrictMode) NeonPurple else ElectricCyan,
                  modifier = Modifier.size(36.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Digital Countdown (MM:SS)
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

            Text(
              text = if (isSessionActiveInDb) formattedTime else "--:--",
              style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                letterSpacing = (-1).sp
              ),
              color = if (remainingSeconds <= 5) NeonPurple else ElectricCyan
            )

            Text(
              text = if (hasTriggeredRedirect) "Redirecting..." else "Auto-return timer",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFA0A7BD)
            )
          }
        }

        // 3. HEADINGS & SUBTITLE
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "YOUR FOCUS MATTERS",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 2.sp,
              fontSize = 26.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
          )

          Text(
            text = "This app is paused while you focus.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ElectricCyan,
            textAlign = TextAlign.Center
          )

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.35f)),
            modifier = Modifier.padding(top = 4.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "$blockedAppName is paused",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
              )
            }
          }
        }

        // 4. MOTIVATIONAL QUOTE CARD
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.85f)),
          border = BorderStroke(1.dp, Color(0xFF232635)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "“${motivationalQuote.text}”",
              style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
              color = Color(0xFFE0E2EC),
              lineHeight = 20.sp
            )
            Text(
              text = "— ${motivationalQuote.author}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = NeonPurple,
              modifier = Modifier.align(Alignment.End)
            )
          }
        }

        // 5. SESSION STATUS CARD (Active, Ended Early, or Paused)
        if (!isSessionActiveInDb) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(24.dp)
              )
              Column {
                Text(
                  text = "Focus Session Completed",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.White
                )
                Text(
                  text = "Your scheduled focus has ended. Apps are no longer restricted.",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFA0A7BD)
                )
              }
            }
          }
        } else if (isSessionPausedInDb) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261E14)),
            border = BorderStroke(1.dp, Color(0xFFFFB236).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PauseCircle,
                contentDescription = null,
                tint = Color(0xFFFFB236),
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Focus Session is currently paused. Return to resume.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFD599)
              )
            }
          }
        }

        // 6. ACTION BUTTONS
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Primary: Return to Focus App Now
          Button(
            onClick = onReturnToFocus,
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("block_screen_return_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = RadiantViolet,
              contentColor = Color.White
            )
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Return to Focus App Now",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }

          // Secondary: Go to Phone Home Screen
          OutlinedButton(
            onClick = onGoToHomeScreen,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("block_screen_home_button"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2F3346)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE0E2EC))
          ) {
            Icon(
              imageVector = Icons.Default.Home,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Go to Phone Home Screen",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
          }
        }

        // 7. EMERGENCY EXIT SECTION (Strict Mode Safeguards)
        if (isStrictMode) {
          TextButton(
            onClick = { showEmergencyDetails = !showEmergencyDetails },
            modifier = Modifier.testTag("emergency_toggle_button")
          ) {
            Text(
              text = if (showEmergencyDetails) "Hide Emergency Option" else "Emergency Exit",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.error
            )
          }

          AnimatedVisibility(visible = showEmergencyDetails) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "Hold button for 3 seconds to cancel session:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
              )

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("emergency_hold_button")
                  .pointerInput(Unit) {
                    detectTapGestures(
                      onPress = {
                        isHoldingEmergency = true
                        tryAwaitRelease()
                        isHoldingEmergency = false
                      }
                    )
                  }
              ) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (isHoldingEmergency) "Hold to Exit..." else "Hold for 3s to Cancel Session",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                  )
                }
              }

              if (holdProgress > 0f) {
                LinearProgressIndicator(
                  progress = { holdProgress },
                  modifier = Modifier.fillMaxWidth(),
                  color = MaterialTheme.colorScheme.error
                )
              }
            }
          }
        } else if (isSessionActiveInDb) {
          TextButton(
            onClick = onEmergencyExit,
            modifier = Modifier.testTag("cancel_session_button")
          ) {
            Text(
              text = "End Focus Session",
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFF666E85)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun FocusActiveIndicatorPill(
  isStrictMode: Boolean,
  isSessionActive: Boolean,
  isSessionPaused: Boolean,
  pulseAlpha: Float
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = CardSurface,
    border = BorderStroke(
      1.dp,
      if (isStrictMode) NeonPurple.copy(alpha = 0.6f) else ElectricCyan.copy(alpha = 0.5f)
    )
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(
            if (!isSessionActive) Color(0xFFA0A7BD)
            else if (isSessionPaused) Color(0xFFFFB236)
            else ElectricCyan.copy(alpha = pulseAlpha)
          )
      )

      val statusText = when {
        !isSessionActive -> "SESSION ENDED"
        isSessionPaused -> "FOCUS MODE PAUSED"
        isStrictMode -> "STRICT FOCUS MODE ACTIVE"
        else -> "FOCUS MODE ACTIVE • DISTRACTION LOCKED"
      }

      Text(
        text = statusText,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        ),
        color = if (isStrictMode) NeonPurple else ElectricCyan
      )
    }
  }
}
