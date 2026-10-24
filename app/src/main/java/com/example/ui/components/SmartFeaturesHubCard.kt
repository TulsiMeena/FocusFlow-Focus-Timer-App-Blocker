package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensor.FlipToFocusSensorManager
import com.example.shield.SmartShieldPreferencesManager
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

@Composable
fun SmartFeaturesHubSection(
  isTimerRunning: Boolean,
  onAutoStartFocusFromFlip: () -> Unit,
  onActivateSleepShieldNow: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val smartPrefs = remember(context) { SmartShieldPreferencesManager.getInstance(context) }

  val isSmartChallengeEnabled by smartPrefs.isSmartChallengeEnabled.collectAsStateWithLifecycle()
  val isSleepShieldEnabled by smartPrefs.isSleepShieldEnabled.collectAsStateWithLifecycle()
  val sleepStartHour by smartPrefs.sleepStartHour.collectAsStateWithLifecycle()
  val sleepEndHour by smartPrefs.sleepEndHour.collectAsStateWithLifecycle()
  val isFlipToFocusEnabled by smartPrefs.isFlipToFocusEnabled.collectAsStateWithLifecycle()

  val flipSensorManager = remember(context) {
    FlipToFocusSensorManager(
      context = context,
      onFaceDownConfirmed = {
        if (smartPrefs.isFlipToFocusEnabled.value && !isTimerRunning) {
          onAutoStartFocusFromFlip()
        }
      },
      onPhonePickedUp = {}
    )
  }

  val isSensorAvailable by flipSensorManager.isSensorAvailable.collectAsStateWithLifecycle()
  val isFaceDown by flipSensorManager.isFaceDown.collectAsStateWithLifecycle()
  val zGravity by flipSensorManager.zGravity.collectAsStateWithLifecycle()
  val orientationLabel by flipSensorManager.orientationLabel.collectAsStateWithLifecycle()
  val pickupWarning by flipSensorManager.pickupWarningMessage.collectAsStateWithLifecycle()

  DisposableEffect(isFlipToFocusEnabled) {
    if (isFlipToFocusEnabled) {
      flipSensorManager.startListening()
    } else {
      flipSensorManager.stopListening()
    }
    onDispose {
      flipSensorManager.stopListening()
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text(
      text = "Smart Pro Controls",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    // 1. FEATURE #4: FLIP-TO-FOCUS (HARDWARE ACCELEROMETER SENSOR)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("card_flip_to_focus"),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      border = BorderStroke(
        1.dp,
        if (isFaceDown && isFlipToFocusEnabled) SuccessEmerald else MaterialTheme.colorScheme.outlineVariant
      )
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
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = if (isFlipToFocusEnabled) CyanAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier.size(42.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.ScreenRotation,
                  contentDescription = null,
                  tint = if (isFlipToFocusEnabled) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Column {
              Text(
                text = "Flip-to-Focus Sensor Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (!isSensorAvailable) {
                  "Accelerometer sensor unavailable"
                } else if (isFlipToFocusEnabled) {
                  "Place phone Face-Down on table to auto-lock focus"
                } else {
                  "Auto-start focus when phone is placed face-down"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = isFlipToFocusEnabled,
            onCheckedChange = { smartPrefs.setFlipToFocusEnabled(it) },
            modifier = Modifier.testTag("switch_flip_to_focus"),
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.surface,
              checkedTrackColor = CyanAccent
            )
          )
        }

        AnimatedVisibility(visible = isFlipToFocusEnabled) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = if (isFaceDown) Icons.Default.CheckCircle else Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = if (isFaceDown) SuccessEmerald else CyanAccent,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "Sensor: $orientationLabel",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFaceDown) SuccessEmerald else MaterialTheme.colorScheme.onSurface
                  )
                }

                Text(
                  text = "Z: ${zGravity}m/s²",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                  ),
                  color = CyanAccent
                )
              }
            }

            if (pickupWarning != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberStreak.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, AmberStreak.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
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
                    text = pickupWarning ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberStreak,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }
        }
      }
    }

    // 2. FEATURE #5: BEDTIME / SLEEP SHIELD (AUTO NIGHT LOCK)
    val isInSleepWindowNow = smartPrefs.isCurrentlyInSleepWindow()
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("card_sleep_shield"),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      border = BorderStroke(
        1.dp,
        if (isSleepShieldEnabled) RadiantViolet.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
      )
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
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = if (isSleepShieldEnabled) RadiantViolet.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier.size(42.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Bedtime,
                  contentDescription = null,
                  tint = if (isSleepShieldEnabled) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Column {
              Text(
                text = "Bedtime / Sleep Shield",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isSleepShieldEnabled) {
                  "Active Window: ${smartPrefs.formatSleepWindow()}"
                } else {
                  "Auto-lock distracting apps at night for deep sleep"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isSleepShieldEnabled) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = isSleepShieldEnabled,
            onCheckedChange = { smartPrefs.setSleepShieldEnabled(it) },
            modifier = Modifier.testTag("switch_sleep_shield"),
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.surface,
              checkedTrackColor = RadiantViolet
            )
          )
        }

        AnimatedVisibility(visible = isSleepShieldEnabled) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "Select Night Lock Schedule:",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val schedules = listOf(
                Triple("10 PM – 6 AM", 22, 6),
                Triple("11 PM – 6 AM", 23, 6),
                Triple("12 AM – 7 AM", 0, 7)
              )
              schedules.forEach { (label, startH, endH) ->
                val selected = (sleepStartHour == startH && sleepEndHour == endH)
                FilterChip(
                  selected = selected,
                  onClick = { smartPrefs.setSleepSchedule(startH, 0, endH, 0) },
                  label = { Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RadiantViolet.copy(alpha = 0.22f),
                    selectedLabelColor = RadiantViolet
                  ),
                  modifier = Modifier.weight(1f)
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isInSleepWindowNow) "🌙 Bedtime window active now!" else "🌙 Armed for tonight",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isInSleepWindowNow) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant
              )

              OutlinedButton(
                onClick = onActivateSleepShieldNow,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_activate_sleep_lock_now")
              ) {
                Text("Start Sleep Lock Now", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
    }

    // 3. FEATURE #1: SMART UNLOCK CHALLENGE (BRAIN TASK)
    var showSamplePuzzle by remember { mutableStateOf(false) }
    var samplePuzzle by remember { mutableStateOf(smartPrefs.generateMathChallenge()) }

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("card_smart_unlock_challenge"),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      border = BorderStroke(
        1.dp,
        if (isSmartChallengeEnabled) ElectricBlue.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
      )
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
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = if (isSmartChallengeEnabled) ElectricBlue.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier.size(42.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Psychology,
                  contentDescription = null,
                  tint = if (isSmartChallengeEnabled) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Column {
              Text(
                text = "Smart Unlock Brain Challenge",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Requires solving a hard math puzzle for 2-min emergency app access",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = isSmartChallengeEnabled,
            onCheckedChange = { smartPrefs.setSmartChallengeEnabled(it) },
            modifier = Modifier.testTag("switch_smart_challenge"),
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.surface,
              checkedTrackColor = ElectricBlue
            )
          )
        }

        AnimatedVisibility(visible = isSmartChallengeEnabled) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (showSamplePuzzle) {
                "Sample: ${samplePuzzle.expression} (Ans: ${samplePuzzle.expectedAnswer})"
              } else {
                "Active on Full-Screen Block Shield"
              },
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = ElectricBlue,
              modifier = Modifier.weight(1f)
            )

            OutlinedButton(
              onClick = {
                samplePuzzle = smartPrefs.generateMathChallenge()
                showSamplePuzzle = true
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("btn_preview_brain_puzzle")
            ) {
              Text("Preview Puzzle", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }
}
