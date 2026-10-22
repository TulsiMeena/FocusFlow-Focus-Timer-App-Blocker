package com.example.apps.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

object UsagePermissionHelper {

  fun isUsageAccessGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
      )
    } else {
      @Suppress("DEPRECATION")
      appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
      )
    }
    return mode == AppOpsManager.MODE_ALLOWED
  }

  fun openUsageAccessSettings(context: Context): Boolean {
    // Attempt package-specific usage access first if supported, then fallback to general settings
    val directIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    if (canResolveIntent(context, directIntent)) {
      try {
        context.startActivity(directIntent)
        return true
      } catch (_: Exception) {
        // Fall back below
      }
    }

    val genericIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    return try {
      context.startActivity(genericIntent)
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun canResolveIntent(context: Context, intent: Intent): Boolean {
    return intent.resolveActivity(context.packageManager) != null
  }
}
