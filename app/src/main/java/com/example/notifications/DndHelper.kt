package com.example.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object DndHelper {

  fun isDndAccessGranted(context: Context): Boolean {
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      notificationManager.isNotificationPolicyAccessGranted
    } else {
      true
    }
  }

  fun openDndSettings(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      return tryLaunchIntent(context, intent)
    } else {
      return openNotificationSettings(context)
    }
  }

  fun openNotificationSettings(context: Context): Boolean {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    } else {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    }
    return tryLaunchIntent(context, intent)
  }

  fun openAndroidAppDetailsSettings(context: Context): Boolean {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    return tryLaunchIntent(context, intent)
  }

  private fun tryLaunchIntent(context: Context, intent: Intent): Boolean {
    return try {
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        true
      } else {
        context.startActivity(intent)
        true
      }
    } catch (_: Exception) {
      false
    }
  }
}
