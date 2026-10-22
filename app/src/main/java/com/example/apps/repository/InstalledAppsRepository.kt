package com.example.apps.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.apps.model.AppInfo
import com.example.apps.util.UsagePermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class InstalledAppsRepository(private val context: Context) {

  private val packageManager: PackageManager = context.packageManager
  private val usageStatsManager: UsageStatsManager? =
    context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

  suspend fun getInstalledLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolveInfos = try {
      packageManager.queryIntentActivities(mainIntent, 0)
    } catch (_: Exception) {
      emptyList()
    }

    val isUsageGranted = UsagePermissionHelper.isUsageAccessGranted(context)
    val usageMap = if (isUsageGranted && usageStatsManager != null) {
      fetchTodayUsageMap()
    } else {
      emptyMap()
    }

    val seenPackages = mutableSetOf<String>()
    val appsList = mutableListOf<AppInfo>()

    for (resolveInfo in resolveInfos) {
      val packageName = resolveInfo.activityInfo?.packageName ?: continue
      if (seenPackages.contains(packageName)) continue
      seenPackages.add(packageName)

      val label = try {
        resolveInfo.loadLabel(packageManager).toString().ifBlank { packageName }
      } catch (_: Exception) {
        packageName
      }

      val icon = try {
        resolveInfo.loadIcon(packageManager)
      } catch (_: Exception) {
        null
      }

      val isSystem = try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
      } catch (_: Exception) {
        false
      }

      val usageDuration = usageMap[packageName] ?: 0L

      appsList.add(
        AppInfo(
          packageName = packageName,
          appName = label,
          icon = icon,
          usageDurationMillis = usageDuration,
          isSystemApp = isSystem
        )
      )
    }

    // Sort apps: prioritize apps with active usage today, otherwise alphabetical
    appsList.sortedWith(
      compareByDescending<AppInfo> { it.usageDurationMillis }
        .thenBy { it.appName.lowercase() }
    )
  }

  private fun fetchTodayUsageMap(): Map<String, Long> {
    val manager = usageStatsManager ?: return emptyMap()
    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = calendar.timeInMillis
    val now = System.currentTimeMillis()

    return try {
      val statsList = manager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startOfDay,
        now
      ) ?: emptyList()

      val map = mutableMapOf<String, Long>()
      for (stat in statsList) {
        val current = map[stat.packageName] ?: 0L
        map[stat.packageName] = current + stat.totalTimeInForeground
      }
      map
    } catch (_: Exception) {
      emptyMap()
    }
  }

  fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return apps
    return apps.filter {
      it.appName.contains(trimmed, ignoreCase = true) ||
        it.packageName.contains(trimmed, ignoreCase = true)
    }
  }
}
