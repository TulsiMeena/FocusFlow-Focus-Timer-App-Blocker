package com.example.ui.components

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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.RadiantViolet

@Composable
fun FounderWelcomeDialog(
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("founder_welcome_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = Color(0xFF101322)
      ),
      border = BorderStroke(
        1.5.dp,
        Brush.linearGradient(
          colors = listOf(RadiantViolet, CyanAccent)
        )
      )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Shield Emblem
        Surface(
          shape = CircleShape,
          color = Color(0xFF181C32),
          border = BorderStroke(2.dp, CyanAccent.copy(alpha = 0.7f)),
          modifier = Modifier.size(68.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = "Focus Shield",
              tint = CyanAccent,
              modifier = Modifier.size(36.dp)
            )
          }
        }

        // Badge
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = RadiantViolet.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, RadiantViolet.copy(alpha = 0.45f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Verified,
              contentDescription = null,
              tint = CyanAccent,
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = "WELCOME FROM APP FOUNDER",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = CyanAccent
            )
          }
        }

        Text(
          text = "Welcome to Focus Shield!",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.ExtraBold
          ),
          color = Color.White,
          textAlign = TextAlign.Center
        )

        Text(
          text = "Namaste! Main Amit Meena, Founder of Focus Shield, aapka swagat karta hoon. Yeh app aapke dhyan (focus) aur keemti samay ko distracting apps se bachane ke liye banaya gaya hai. Apna focus target set karein aur bina kisi rukawat ke apne goals achieve karein!",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFCBD5E1),
          textAlign = TextAlign.Center,
          lineHeight = 21.sp
        )

        // Founder Signature Card
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF161A2E),
          border = BorderStroke(1.dp, Color(0xFF283052)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "Amit Meena",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
              )
              Text(
                text = "Founder & Developer • Focus Shield",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent
              )
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = RadiantViolet.copy(alpha = 0.2f)
            ) {
              Text(
                text = "v2.4 Pro",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Button(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("founder_welcome_start_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = RadiantViolet,
            contentColor = Color.White
          )
        ) {
          Text(
            text = "Let's Start Focusing →",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}
