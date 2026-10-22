package com.example.garden

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Yard
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SuccessEmerald

enum class TreeStage(
  val title: String,
  val emoji: String,
  val description: String,
  val color: Color
) {
  SEED("Dormant Seed", "🌱", "Planted in soil, beginning deep work", AmberStreak),
  SPROUT("Sprouting Stem", "🌿", "Gaining momentum and growing roots", ElectricBlue),
  SAPLING("Thriving Sapling", "🪴", "Shielded against distractions", SuccessEmerald),
  BLOOMING("Majestic Bonsai", "🌳", "Fully realized focus & achievement", Color(0xFF10B981)),
  WITHERED("Withered Tree", "🥀", "Session abandoned before completion", Color(0xFFEF4444))
}

fun calculateTreeStage(progress: Float, isWithered: Boolean = false): TreeStage {
  if (isWithered) return TreeStage.WITHERED
  return when {
    progress < 0.25f -> TreeStage.SEED
    progress < 0.60f -> TreeStage.SPROUT
    progress < 0.95f -> TreeStage.SAPLING
    else -> TreeStage.BLOOMING
  }
}

@Composable
fun FocusTreeWidget(
  progress: Float,
  isWithered: Boolean = false,
  modifier: Modifier = Modifier
) {
  val stage = calculateTreeStage(progress, isWithered)
  val scaleAnim by animateFloatAsState(
    targetValue = (0.75f + (progress * 0.35f)).coerceIn(0.75f, 1.15f),
    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
    label = "tree_scale"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("widget_focus_tree"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Tree Avatar / Growth Stage Circle
      Surface(
        shape = CircleShape,
        color = stage.color.copy(alpha = 0.15f),
        border = BorderStroke(1.5.dp, stage.color.copy(alpha = 0.4f)),
        modifier = Modifier.size(64.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.scale(scaleAnim)
        ) {
          Text(
            text = stage.emoji,
            fontSize = 32.sp
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stage.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = stage.color.copy(alpha = 0.15f)
          ) {
            Text(
              text = "${(progress * 100).toInt()}% Growth",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = stage.color,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stage.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = stage.color,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
      }
    }
  }
}
