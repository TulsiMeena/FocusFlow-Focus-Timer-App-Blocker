package com.example.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {
  companion object {
    const val CHANNEL_ID = "advanced_focus_timer_alerts"
    const val SERVICE_CHANNEL_ID = "advanced_focus_shield_service"
    const val NOTIFICATION_ID = 1001
    const val SERVICE_NOTIFICATION_ID = 1005
  }

  init {
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val timerChannel = NotificationChannel(
        CHANNEL_ID,
        "Timer Completion Alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alerts when a focus session or break timer has completed"
        enableVibration(true)
      }

      val serviceChannel = NotificationChannel(
        SERVICE_CHANNEL_ID,
        "Focus Shield Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Ongoing notification displayed while Focus Shield is actively monitoring"
        setShowBadge(false)
      }

      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      manager?.createNotificationChannel(timerChannel)
      manager?.createNotificationChannel(serviceChannel)
    }
  }

  fun sendTimerCompleteNotification(title: String, message: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      )
      if (permission != PackageManager.PERMISSION_GRANTED) {
        return
      }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(title)
      .setContentText(message)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    } catch (_: SecurityException) {
      // Gracefully handle if permission was revoked
    }
  }

  fun sendFocusShieldAlertNotification(appName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      )
      if (permission != PackageManager.PERMISSION_GRANTED) {
        return
      }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      1,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Focus Shield Alert ⚠️")
      .setContentText("$appName was opened during your active focus session. Tap to return to Advanced Focus.")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(1002, notification)
    } catch (_: SecurityException) {
    }
  }

  fun sendRedirectToFocusNotification(appName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      )
      if (permission != PackageManager.PERMISSION_GRANTED) {
        return
      }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(MainActivity.EXTRA_NAVIGATE_TO, com.example.navigation.Screen.Focus.route)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      2,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Your Focus Matters • Return to Session")
      .setContentText("$appName paused. 60-second timer elapsed — tap to resume your Focus session.")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .addAction(
        R.drawable.ic_launcher_foreground,
        "Open Focus Timer",
        pendingIntent
      )
      .build()

    try {
      NotificationManagerCompat.from(context).notify(1003, notification)
    } catch (_: SecurityException) {
    }
  }

  fun sendFocusReminderNotification(remainingMinutes: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      )
      if (permission != PackageManager.PERMISSION_GRANTED) {
        return
      }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      2,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Focus Session Active ⏳")
      .setContentText("Stay in the zone! Session is in progress ($remainingMinutes min remaining).")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(1003, notification)
    } catch (_: SecurityException) {
    }
  }

  fun cancelFocusReminderNotification() {
    try {
      NotificationManagerCompat.from(context).cancel(1003)
    } catch (_: SecurityException) {
    }
  }

  fun buildForegroundServiceNotification(
    blockedCount: Int,
    isStrictMode: Boolean
  ): Notification {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      3,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val strictStatus = if (isStrictMode) "Strict Mode ON" else "Standard Mode"

    return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Focus Shield Active 🛡️")
      .setContentText("$blockedCount apps restricted • $strictStatus")
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setContentIntent(pendingIntent)
      .build()
  }
}
