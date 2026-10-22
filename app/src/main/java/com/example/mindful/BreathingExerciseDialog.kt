package com.example.mindful

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald
import kotlinx.coroutines.delay

enum class BreathingPhase(val label: String, val instruction: String, val targetScale: Float, val color: Color) {
  INHALE("Inhale", "Breathe in deeply through your nose...", 1.25f, CyanAccent),
  HOLD_IN("Hold", "Hold your breath calmly...", 1.25f, RadiantViolet),
  EXHALE("Exhale", "Release slowly through your mouth...", 0.85f, ElectricBlue),
  HOLD_OUT("Rest", "Relax and prepare for the next breath...", 0.85f, SuccessEmerald)
}

@Composable
fun BreathingExerciseDialog(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var phase by remember { mutableStateOf(BreathingPhase.INHALE) }
  var cycleCount by remember { mutableIntStateOf(1) }
  val scaleAnim = remember { Animatable(0.85f) }

  LaunchedEffect(Unit) {
    while (true) {
      // 1. INHALE (4 seconds)
      phase = BreathingPhase.INHALE
      scaleAnim.animateTo(
        targetValue = BreathingPhase.INHALE.targetScale,
        animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing)
      )

      // 2. HOLD IN (4 seconds)
      phase = BreathingPhase.HOLD_IN
      delay(4000)

      // 3. EXHALE (4 seconds)
      phase = BreathingPhase.EXHALE
      scaleAnim.animateTo(
        targetValue = BreathingPhase.EXHALE.targetScale,
        animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing)
      )

      // 4. HOLD OUT (4 seconds)
      phase = BreathingPhase.HOLD_OUT
      delay(4000)

      cycleCount++
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = modifier
        .fillMaxWidth()
        .testTag("dialog_breathing_exercise"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Header
        Box(modifier = Modifier.fillMaxWidth()) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Surface(
              shape = CircleShape,
              color = CyanAccent.copy(alpha = 0.15f),
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Spa,
                  contentDescription = null,
                  tint = CyanAccent,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Mindful Reset (4-4-4-4)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Box breathing to reduce impulse & restore clarity",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Animated Breathing Orb
        Box(
          modifier = Modifier
            .size(190.dp),
          contentAlignment = Alignment.Center
        ) {
          // Outer subtle halo
          Box(
            modifier = Modifier
              .size(180.dp)
              .scale(scaleAnim.value * 1.1f)
              .clip(CircleShape)
              .background(phase.color.copy(alpha = 0.12f))
          )

          // Main pulsing circle
          Box(
            modifier = Modifier
              .size(130.dp)
              .scale(scaleAnim.value)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors = listOf(
                    phase.color.copy(alpha = 0.85f),
                    phase.color.copy(alpha = 0.4f)
                  )
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = phase.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
            }
          }
        }

        // Phase guidance
        Text(
          text = phase.instruction,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Text(
            text = "Cycle $cycleCount • 4s rhythm",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }

        Button(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_finish_breathing"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("I'm Centered • Resume Focus", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
