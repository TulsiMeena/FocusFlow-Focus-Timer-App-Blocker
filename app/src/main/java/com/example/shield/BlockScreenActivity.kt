package com.example.shield

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.example.ui.screens.apps.AppIconImage
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
private val MidnightBlack = Color(0xFF05060B)
private val DeepCanvasBlack = Color(0xFF0B0E1C)
private val NeonPurple = Color(0xFF9D4EDD)
private val RadiantViolet = Color(0xFF8B5CF6)
private val ElectricCyan = Color(0xFF00E5FF)
private val CardSurface = Color(0xFF121526)

class BlockScreenActivity : ComponentActivity() {

  companion object {
    const val EXTRA_BLOCKED_APP_NAME = "EXTRA_BLOCKED_APP_NAME"
    const val EXTRA_BLOCKED_PACKAGE_NAME = "EXTRA_BLOCKED_PACKAGE_NAME"
    const val EXTRA_IS_STRICT = "EXTRA_IS_STRICT"
  }

  private val blockedAppNameState = mutableStateOf("Restricted App")
  private val blockedPackageNameState = mutableStateOf("")
  private val isStrictModeState = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Dismiss any active floating window manager overlay so full-screen activity renders cleanly
    try {
      FocusBlockOverlayManager.dismissActiveOverlay()
    } catch (_: Exception) {
    }

    blockedAppNameState.value = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: "Restricted App"
    blockedPackageNameState.value = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE_NAME) ?: ""
    isStrictModeState.value = intent.getBooleanExtra(EXTRA_IS_STRICT, false)

    setContent {
      AdvancedFocusTheme {
        val currentAppName = blockedAppNameState.value
        val currentPkgName = blockedPackageNameState.value
        val currentStrict = isStrictModeState.value

        BlockScreenContent(
          blockedAppName = currentAppName,
          blockedPackageName = currentPkgName,
          isStrictMode = currentStrict,
          onReturnToFocus = {
            returnToFocusApp(currentAppName)
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

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    try {
      FocusBlockOverlayManager.dismissActiveOverlay()
    } catch (_: Exception) {
    }
    intent.getStringExtra(EXTRA_BLOCKED_APP_NAME)?.let { blockedAppNameState.value = it }
    intent.getStringExtra(EXTRA_BLOCKED_PACKAGE_NAME)?.let { blockedPackageNameState.value = it }
    isStrictModeState.value = intent.getBooleanExtra(EXTRA_IS_STRICT, isStrictModeState.value)
  }

  private fun returnToFocusApp(appName: String) {
    val focusIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(MainActivity.EXTRA_NAVIGATE_TO, Screen.Focus.route)
    }
    try {
      startActivity(focusIntent)
    } catch (e: Exception) {
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
 * Advance-level, edge-to-edge full-screen blocked app interface with
 * dual animated cyber radar rings, real blocked app icon, live focus session HUD,
 * 60-second countdown, and automatic redirect to the Focus App.
 */
@Composable
fun BlockScreenContent(
  blockedAppName: String,
  blockedPackageName: String = "",
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

  // Load real icon of the blocked app if available
  val blockedAppDrawable: Drawable? = remember(blockedPackageName) {
    if (blockedPackageName.isNotBlank()) {
      try {
        context.packageManager.getApplicationIcon(blockedPackageName)
      } catch (_: Exception) {
        null
      }
    } else {
      null
    }
  }

  // 1. Stable 60-Second Countdown Calculation
  val countdownTotalSeconds = 60
  val countdownEndEpoch = rememberSaveable { SystemClock.elapsedRealtime() + (countdownTotalSeconds * 1000L) }
  var remainingSeconds by remember {
    mutableIntStateOf(
      ((countdownEndEpoch - SystemClock.elapsedRealtime() + 999) / 1000).toInt().coerceIn(0, countdownTotalSeconds)
    )
  }

  var hasTriggeredRedirect by remember { mutableStateOf(false) }

  // 2. Real Focus Session Observer
  var activeSessionState by remember { mutableStateOf<ActiveTimerStateEntity?>(null) }
  var isSessionActiveInDb by remember { mutableStateOf(true) }
  var isSessionPausedInDb by remember { mutableStateOf(false) }
  var mainSessionRemainingSec by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      val database = AppDatabase.getInstance(context)
      val initial = database.activeTimerDao().getActiveTimerStateDirect()
      val now = System.currentTimeMillis()
      val mainRem = if (initial != null) {
        if (initial.status == TimerStatus.PAUSED.name) {
          initial.remainingSeconds
        } else {
          ((initial.targetEndTimeMillis - now) / 1000).toInt().coerceAtLeast(0)
        }
      } else 0
      withContext(Dispatchers.Main) {
        activeSessionState = initial
        isSessionActiveInDb = (initial != null)
        isSessionPausedInDb = (initial?.status == TimerStatus.PAUSED.name)
        mainSessionRemainingSec = mainRem
      }
    }

    while (isActive) {
      delay(1000L)
      withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val current = database.activeTimerDao().getActiveTimerStateDirect()
        val now = System.currentTimeMillis()
        val mainRem = if (current != null) {
          if (current.status == TimerStatus.PAUSED.name) {
            current.remainingSeconds
          } else {
            ((current.targetEndTimeMillis - now) / 1000).toInt().coerceAtLeast(0)
          }
        } else 0
        withContext(Dispatchers.Main) {
          activeSessionState = current
          isSessionActiveInDb = (current != null)
          isSessionPausedInDb = (current?.status == TimerStatus.PAUSED.name)
          mainSessionRemainingSec = mainRem
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
          delay(400L)
          onReturnToFocus()
        }
        break
      }
      delay(250L)
    }
  }

  // 4. Smooth ambient animations
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.45f,
    targetValue = 0.92f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  val radarRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(9000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "radarRotation"
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
      val totalSteps = 30
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
    ) {
      // Full-Screen Ambient Cyber Auras
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              NeonPurple.copy(alpha = 0.16f * pulseAlpha),
              Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.28f),
            radius = size.minDimension * 0.7f
          ),
          center = Offset(size.width * 0.5f, size.height * 0.28f),
          radius = size.minDimension * 0.7f
        )

        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              ElectricCyan.copy(alpha = 0.12f * pulseAlpha),
              Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.78f),
            radius = size.minDimension * 0.6f
          ),
          center = Offset(size.width * 0.5f, size.height * 0.78f),
          radius = size.minDimension * 0.6f
        )
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 22.dp, vertical = 18.dp)
          .widthIn(max = 540.dp)
          .align(Alignment.Center)
          .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // TOP SECTION: Status Pill + Radar Ring + Headings
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          // 1. FOCUS MODE ACTIVE STATUS PILL
          FocusActiveIndicatorPill(
            isStrictMode = isStrictMode,
            isSessionActive = isSessionActiveInDb,
            isSessionPaused = isSessionPausedInDb,
            pulseAlpha = pulseAlpha
          )

          // 2. DUAL CONCENTRIC CYBER RADAR & 60S COUNTDOWN RING
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(232.dp)
              .padding(6.dp)
          ) {
            val progressFraction = (remainingSeconds.toFloat() / countdownTotalSeconds.toFloat()).coerceIn(0f, 1f)

            Canvas(modifier = Modifier.fillMaxSize()) {
              val outerDashStroke = 2.dp.toPx()
              val mainStroke = 9.dp.toPx()
              val mainPadding = 12.dp.toPx()
              val arcDiameter = size.minDimension - (mainPadding * 2) - mainStroke
              val arcTopLeft = Offset(mainPadding + mainStroke / 2, mainPadding + mainStroke / 2)

              // Outer rotating dashed radar ring
              rotate(degrees = radarRotation) {
                drawCircle(
                  color = if (isStrictMode) NeonPurple.copy(alpha = 0.35f) else ElectricCyan.copy(alpha = 0.35f),
                  radius = (size.minDimension - outerDashStroke) / 2f,
                  style = Stroke(
                    width = outerDashStroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f), 0f)
                  )
                )
              }

              // Background subtle track
              drawArc(
                color = NeonPurple.copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = Size(arcDiameter, arcDiameter),
                style = Stroke(width = mainStroke, cap = StrokeCap.Round)
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
                topLeft = arcTopLeft,
                size = Size(arcDiameter, arcDiameter),
                style = Stroke(width = mainStroke, cap = StrokeCap.Round)
              )
            }

            // Inner Blocked App Icon / Lock & Central Countdown Timer
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                  shape = CircleShape,
                  color = CardSurface.copy(alpha = 0.96f),
                  border = BorderStroke(
                    2.dp,
                    Brush.linearGradient(
                      listOf(
                        NeonPurple.copy(alpha = pulseAlpha),
                        ElectricCyan.copy(alpha = pulseAlpha)
                      )
                    )
                  ),
                  modifier = Modifier.size(68.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    if (blockedAppDrawable != null) {
                      AppIconImage(
                        drawable = blockedAppDrawable,
                        appName = blockedAppName,
                        modifier = Modifier.size(44.dp)
                      )
                    } else {
                      Icon(
                        imageVector = if (isStrictMode) Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = "Lock Illustration",
                        tint = if (isStrictMode) NeonPurple else ElectricCyan,
                        modifier = Modifier.size(34.dp)
                      )
                    }
                  }
                }

                // Mini Lock Badge on corner of app icon
                Surface(
                  shape = CircleShape,
                  color = NeonPurple,
                  border = BorderStroke(1.5.dp, MidnightBlack),
                  modifier = Modifier.size(24.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Lock,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(13.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

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
                text = if (hasTriggeredRedirect) "REDIRECTING..." else "AUTO-RETURN TIMER",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = Color(0xFF94A3B8)
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
                fontSize = 25.sp
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
              shape = RoundedCornerShape(14.dp),
              color = CardSurface,
              border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.45f)),
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // MIDDLE SECTION: Live Main Session Telemetry & Quote Card
        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          // Live Active Focus Session Remaining HUD
          if (isSessionActiveInDb && activeSessionState != null) {
            val mainMin = mainSessionRemainingSec / 60
            val mainSec = mainSessionRemainingSec % 60
            val mainFormatted = String.format(Locale.US, "%02d:%02d", mainMin, mainSec)
            val totalPlanned = (activeSessionState?.totalDurationSeconds ?: 1500).coerceAtLeast(1)
            val completedFraction = ((totalPlanned - mainSessionRemainingSec).toFloat() / totalPlanned.toFloat()).coerceIn(0f, 1f)

            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.92f)),
              border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.28f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
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
                      imageVector = Icons.Default.Timer,
                      contentDescription = null,
                      tint = ElectricCyan,
                      modifier = Modifier.size(16.dp)
                    )
                    Text(
                      text = "Active Focus Session Remaining",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = Color(0xFFCBD5E1)
                    )
                  }
                  Text(
                    text = mainFormatted,
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold
                    ),
                    color = ElectricCyan
                  )
                }

                LinearProgressIndicator(
                  progress = { completedFraction },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                  color = NeonPurple,
                  trackColor = Color(0xFF1E243B)
                )
              }
            }
          }

          // 4. MOTIVATIONAL QUOTE CARD
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, Color(0xFF262B44)),
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

          // 5. SESSION STATUS BANNERS (Ended Early or Paused)
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // BOTTOM SECTION: Action Buttons & Emergency Exit
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
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
        }
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
