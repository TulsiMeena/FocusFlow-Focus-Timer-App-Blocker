package com.example.audio

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.vector.ImageVector

enum class SoundscapeType(
  val id: String,
  val title: String,
  val description: String,
  val icon: ImageVector
) {
  NONE("none", "Silent", "No soundscape", Icons.Default.GraphicEq),
  WHITE_NOISE("white_noise", "White Noise", "Gentle frequency masking", Icons.Default.GraphicEq),
  BROWN_NOISE("brown_noise", "Brown Noise", "Deep warm rumbling static", Icons.Default.GraphicEq),
  RAIN("rain", "Gentle Rain", "Soothing precipitation droplets", Icons.Default.WaterDrop),
  OCEAN("ocean", "Ocean Waves", "Rhythmic surging tidal swell", Icons.Default.Waves),
  BINAURAL("binaural", "432Hz Alpha", "Pure focus & mental clarity tone", Icons.Default.Bedtime),
  FOREST("forest", "Forest Wind", "Calming canopy breeze", Icons.Default.Forest),
  CAFE("cafe", "Cozy Ambience", "Subtle warm coffee shop hum", Icons.Default.Coffee)
}
