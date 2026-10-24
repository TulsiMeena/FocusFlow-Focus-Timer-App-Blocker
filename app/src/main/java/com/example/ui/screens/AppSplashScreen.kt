package com.example.ui.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val SplashMidnight = Color(0xFF05060B)
private val SplashDeepNavy = Color(0xFF0B0E1A)
private val SplashNeonPurple = Color(0xFF9D4EDD)
private val SplashRadiantViolet = Color(0xFF8B5CF6)
private val SplashElectricCyan = Color(0xFF00E5FF)
private val SplashSurfaceCard = Color(0xFF121526)

@Composable
fun AppSplashScreen(
  onSplashFinished: () -> Unit
) {
  var progress by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(Unit) {
    val totalSteps = 36
    for (step in 1..totalSteps) {
      delay(40L)
      progress = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
    }
    delay(180L)
    onSplashFinished()
  }

  val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
  val outerRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 6000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "outerRotation"
  )

  val innerRotation by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "innerRotation"
  )

  val pulseGlow by infiniteTransition.animateFloat(
    initialValue = 0.45f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseGlow"
  )

  val statusLabel = when {
    progress < 0.35f -> "Initializing Focus Engine..."
    progress < 0.72f -> "Activating Shield Interception..."
    progress < 0.95f -> "Loading Analytics & Workspace..."
    else -> "Ready • Launching Focus Shield"
  }

  val percentInt = (progress * 100).toInt().coerceIn(0, 100)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            SplashMidnight,
            SplashDeepNavy,
            SplashMidnight
          )
        )
      )
      .testTag("app_splash_screen")
  ) {
    // Ambient Background Cyber Auras
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            SplashNeonPurple.copy(alpha = 0.16f * pulseGlow),
            Color.Transparent
          ),
          center = Offset(size.width * 0.5f, size.height * 0.38f),
          radius = size.minDimension * 0.65f
        ),
        center = Offset(size.width * 0.5f, size.height * 0.38f),
        radius = size.minDimension * 0.65f
      )

      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            SplashElectricCyan.copy(alpha = 0.12f * pulseGlow),
            Color.Transparent
          ),
          center = Offset(size.width * 0.5f, size.height * 0.75f),
          radius = size.minDimension * 0.55f
        ),
        center = Offset(size.width * 0.5f, size.height * 0.75f),
        radius = size.minDimension * 0.55f
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 28.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Pill
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = SplashSurfaceCard.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, SplashElectricCyan.copy(alpha = 0.35f)),
        modifier = Modifier.padding(top = 12.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = SplashElectricCyan,
            modifier = Modifier.size(15.dp)
          )
          Text(
            text = "ADVANCED FOCUS & APP BLOCKER",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = SplashElectricCyan
          )
        }
      }

      // Center Emblem & Title
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(210.dp)
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val outerStroke = 5.dp.toPx()
            val innerStroke = 3.5.dp.toPx()
            val outerDiameter = size.minDimension - outerStroke * 2
            val innerDiameter = size.minDimension * 0.76f

            // Subtle static ring
            drawCircle(
              color = SplashNeonPurple.copy(alpha = 0.14f),
              radius = outerDiameter / 2f,
              style = Stroke(width = outerStroke)
            )

            // Rotating Outer Cyber Ring
            rotate(degrees = outerRotation) {
              drawArc(
                brush = Brush.sweepGradient(
                  0.0f to Color.Transparent,
                  0.25f to SplashElectricCyan,
                  0.5f to SplashNeonPurple,
                  0.75f to Color.Transparent,
                  1.0f to Color.Transparent
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(outerStroke, outerStroke),
                size = Size(outerDiameter, outerDiameter),
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
              )
            }

            // Counter-Rotating Inner Ring
            val innerOffset = (size.minDimension - innerDiameter) / 2f
            rotate(degrees = innerRotation) {
              drawArc(
                brush = Brush.sweepGradient(
                  0.0f to SplashNeonPurple.copy(alpha = 0.85f),
                  0.45f to SplashElectricCyan.copy(alpha = 0.85f),
                  1.0f to Color.Transparent
                ),
                startAngle = 45f,
                sweepAngle = 200f,
                useCenter = false,
                topLeft = Offset(innerOffset, innerOffset),
                size = Size(innerDiameter, innerDiameter),
                style = Stroke(width = innerStroke, cap = StrokeCap.Round)
              )
            }
          }

          // Center Glowing Shield Core
          Surface(
            shape = CircleShape,
            color = SplashSurfaceCard,
            border = BorderStroke(
              2.dp,
              Brush.linearGradient(
                listOf(
                  SplashElectricCyan.copy(alpha = pulseGlow),
                  SplashNeonPurple.copy(alpha = pulseGlow)
                )
              )
            ),
            modifier = Modifier.size(104.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Focus Shield Emblem",
                tint = SplashElectricCyan,
                modifier = Modifier.size(52.dp)
              )
            }
          }
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "FOCUS SHIELD",
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 3.sp,
              fontSize = 30.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
          )

          Text(
            text = "Protect Your Attention • Master Your Time",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Medium,
              letterSpacing = 0.5.sp
            ),
            color = Color(0xFFA5B4FC),
            textAlign = TextAlign.Center
          )

          Text(
            text = "Founded & Crafted by Amit Meena",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 0.8.sp
            ),
            color = SplashElectricCyan.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
          )
        }
      }

      // Bottom Progress Section
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = SplashSurfaceCard.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, SplashNeonPurple.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 420.dp)
          .padding(bottom = 12.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = statusLabel,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = Color.White
            )
            Text(
              text = "$percentInt%",
              style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              ),
              color = SplashElectricCyan
            )
          }

          // Custom Gradient Progress Bar
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF1E2238))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0.04f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(
                      SplashRadiantViolet,
                      SplashNeonPurple,
                      SplashElectricCyan
                    )
                  )
                )
            )
          }
        }
      }
    }
  }
}
