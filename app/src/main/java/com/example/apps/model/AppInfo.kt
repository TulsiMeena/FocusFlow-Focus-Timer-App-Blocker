package com.example.apps.model

import android.graphics.drawable.Drawable

data class AppInfo(
  val packageName: String,
  val appName: String,
  val icon: Drawable? = null,
  val usageDurationMillis: Long = 0L,
  val isSystemApp: Boolean = false,
  val category: AppCategory = AppCategory.fromPackageAndName(packageName, appName)
) {
  val formattedUsage: String
    get() = if (usageDurationMillis <= 0L) {
      "No usage today"
    } else {
      val totalMinutes = usageDurationMillis / (1000 * 60)
      val hours = totalMinutes / 60
      val mins = totalMinutes % 60
      if (hours > 0) {
        "${hours}h ${mins}m today"
      } else if (mins > 0) {
        "${mins}m today"
      } else {
        "< 1m today"
      }
    }
}
