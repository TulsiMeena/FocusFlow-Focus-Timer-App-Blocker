package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundscapeType
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet

@Composable
fun SoundscapePlayerCard(
  currentSoundscape: SoundscapeType,
  isPlaying: Boolean,
  volume: Float,
  onSelectSoundscape: (SoundscapeType) -> Unit,
  onTogglePlay: () -> Unit,
  onVolumeChange: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var showVolumeSlider by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_soundscape_player"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = if (isPlaying) RadiantViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(40.dp)
          ) {
            IconButton(
              onClick = onTogglePlay,
              modifier = Modifier.testTag("btn_toggle_soundscape")
            ) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause Soundscape" else "Play Soundscape",
                tint = if (isPlaying) RadiantViolet else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
              )
            }
          }

          Column {
            Text(
              text = if (currentSoundscape == SoundscapeType.NONE) "Focus Soundscape" else currentSoundscape.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (isPlaying) "Playing offline audio • Procedural synthesis" else "Select a calming background sound",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = if (isPlaying) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(
          onClick = { showVolumeSlider = !showVolumeSlider },
          modifier = Modifier.testTag("btn_toggle_volume_slider")
        ) {
          Icon(
            imageVector = if (volume > 0.5f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
            contentDescription = "Volume",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // Volume Slider
      AnimatedVisibility(visible = showVolumeSlider) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.VolumeDown,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier
              .weight(1f)
              .testTag("slider_sound_volume"),
            colors = SliderDefaults.colors(
              thumbColor = RadiantViolet,
              activeTrackColor = RadiantViolet
            )
          )
          Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Soundscape Selector Chips
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(SoundscapeType.entries.filter { it != SoundscapeType.NONE }) { sound ->
          val isSelected = currentSoundscape == sound
          FilterChip(
            selected = isSelected,
            onClick = {
              if (isSelected && isPlaying) {
                onTogglePlay()
              } else {
                onSelectSoundscape(sound)
              }
            },
            label = { Text(sound.title, style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
              Icon(
                imageVector = sound.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isSelected) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = RadiantViolet.copy(alpha = 0.2f),
              selectedLabelColor = RadiantViolet
            ),
            modifier = Modifier.testTag("soundscape_${sound.id}")
          )
        }
      }
    }
  }
}
