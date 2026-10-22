package com.example.ui.screens.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

@Composable
fun AppIconImage(
  drawable: Drawable?,
  appName: String,
  modifier: Modifier = Modifier
) {
  val bitmap = remember(drawable) {
    drawable?.let { d ->
      try {
        if (d is BitmapDrawable && d.bitmap != null) {
          d.bitmap
        } else {
          val width = if (d.intrinsicWidth > 0) d.intrinsicWidth.coerceAtMost(128) else 96
          val height = if (d.intrinsicHeight > 0) d.intrinsicHeight.coerceAtMost(128) else 96
          val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
          val canvas = Canvas(bmp)
          d.setBounds(0, 0, canvas.width, canvas.height)
          d.draw(canvas)
          bmp
        }
      } catch (_: Exception) {
        null
      }
    }
  }

  if (bitmap != null) {
    Image(
      bitmap = bitmap.asImageBitmap(),
      contentDescription = "$appName icon",
      modifier = modifier.clip(RoundedCornerShape(10.dp))
    )
  } else {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = modifier
    ) {
      Box(contentAlignment = Alignment.Center) {
        val initial = appName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Text(
          text = initial,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}

@Composable
fun UsageAccessExplanationDialog(
  onDismiss: () -> Unit,
  onOpenSettings: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = null,
        tint = RadiantViolet,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Usage Access Permission",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Why is this permission needed?",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Usage Access allows Advanced Focus to calculate daily screen-time for installed applications, helping you understand digital distractions alongside your focus sessions.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.LockClock,
              contentDescription = null,
              tint = SuccessEmerald,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "100% Private & Local. No data is ever transmitted, logged remotely, or shared.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
        Text(
          text = "Tap 'Open Android Settings' to find Advanced Focus and enable Usage Access.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onOpenSettings,
        colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
        modifier = Modifier.testTag("open_usage_settings_button")
      ) {
        Text("Open Android Settings")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dismiss_explanation_button")
      ) {
        Text("Not Now")
      }
    }
  )
}

@Composable
fun OverlayAccessExplanationDialog(
  onDismiss: () -> Unit,
  onOpenOverlaySettings: () -> Unit,
  onOpenAppDetailsSettings: () -> Unit,
  onOpenOverlayListSettings: () -> Unit
) {
  val scrollState = androidx.compose.foundation.rememberScrollState()

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = null,
        tint = AmberStreak,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Display Over Other Apps",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Why is this required?",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "This allows Focus App to show the full-screen blocking UI with the 60-second timer when you attempt to open a restricted application during an active session.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Troubleshooting card for "Turns ON then immediately OFF"
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AmberStreak.copy(alpha = 0.12f),
          border = androidx.compose.foundation.BorderStroke(1.dp, AmberStreak.copy(alpha = 0.4f))
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AmberStreak,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Switch turns OFF automatically?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Text(
              text = "On Android 13, 14 & 15, Android blocks sensitive permissions on test / sideloaded apps under 'Restricted Settings'. Follow these 2 quick steps to fix it:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )

            // Step 1
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surface
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "Step 1: Allow Restricted Settings",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = RadiantViolet
                )
                Text(
                  text = "Tap 'Open App Info' below → Tap the 3 dots (⋮) in the top-right corner → Tap 'Allow restricted settings' → Enter your device PIN/fingerprint.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Step 2
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surface
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "Step 2: Turn ON Display Over Apps",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = RadiantViolet
                )
                Text(
                  text = "Now tap 'Open Display Over Apps' below and toggle it ON. It will now stay ON permanently!",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Text(
              text = "Xiaomi / Redmi / Vivo / Oppo users: Also check App Info → Other Permissions → enable 'Display pop-up windows while running in the background'.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    confirmButton = {
      Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onOpenAppDetailsSettings,
          colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
          modifier = Modifier.testTag("open_app_info_for_restricted_settings")
        ) {
          Text("1. Open App Info (3 Dots ⋮)")
        }
        androidx.compose.material3.OutlinedButton(
          onClick = onOpenOverlaySettings,
          modifier = Modifier.testTag("open_overlay_settings_from_dialog")
        ) {
          Text("2. Open Display Over Apps")
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dismiss_overlay_explanation_button")
      ) {
        Text("Done / Close")
      }
    }
  )
}

