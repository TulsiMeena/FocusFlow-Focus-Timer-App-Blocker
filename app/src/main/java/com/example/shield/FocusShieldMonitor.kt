package com.example.shield

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.apps.util.OverlayPermissionHelper
import com.example.apps.util.UsagePermissionHelper
import com.example.timer.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Diagnostics snapshot representing the real-time operational status of the app blocking shield.
 */
data class ShieldDiagnostics(
  val isServiceActive: Boolean = false,
  val isUsageAccessGranted: Boolean = false,
  val isOverlayPermissionGranted: Boolean = false,
  val isNotificationPermissionGranted: Boolean = false,
  val blockedPackageCount: Int = 0,
  val isTimerActive: Boolean = false,
  val isStrictMode: Boolean = false,
  val lastDetectedPackage: String? = null,
  val lastDetectedEvent: String? = null,
  val lastAlertTimestamp: Long = 0L,
  val pollCount: Long = 0L,
  val statusMessage: String = "Idle",
  val capabilityLevel: String = "Disabled" // "Full Blocking Active", "Notification Only", "Disabled"
)

class FocusShieldMonitor(
  private val context: Context,
  private val scope: CoroutineScope,
  private val notificationHelper: NotificationHelper,
  private val onDistractionDetected: (appName: String, packageName: String) -> Unit
) {

  companion object {
    private const val TAG = "FocusShieldMonitor"
  }

  private val _diagnostics = MutableStateFlow(ShieldDiagnostics())
  val diagnostics: StateFlow<ShieldDiagnostics> = _diagnostics.asStateFlow()

  val isMonitoring: Boolean
    get() = FocusShieldService.isServiceRunning.value

  init {
    // Observe FocusShieldService real-time state changes
    scope.launch {
      FocusShieldService.isServiceRunning.collect { running ->
        refreshDiagnostics(isServiceRunning = running)
      }
    }
    scope.launch {
      FocusShieldService.lastDetectedEvent.collect { event ->
        if (event != null) {
          val current = _diagnostics.value
          _diagnostics.value = current.copy(
            lastDetectedEvent = event,
            lastAlertTimestamp = System.currentTimeMillis()
          )
        }
      }
    }
    scope.launch {
      FocusShieldService.lastDetectedPackage.collect { pkg ->
        if (pkg != null) {
          val current = _diagnostics.value
          _diagnostics.value = current.copy(lastDetectedPackage = pkg)
        }
      }
    }
  }

  fun startMonitoring(
    blockedPackages: Set<String>,
    packageNamesToLabels: Map<String, String>,
    isStrictMode: Boolean = false
  ) {
    val usageGranted = UsagePermissionHelper.isUsageAccessGranted(context)
    val overlayGranted = OverlayPermissionHelper.isOverlayPermissionGranted(context)
    val notifGranted = isNotificationGranted()

    if (!usageGranted) {
      Log.w(TAG, "Cannot start shield monitor: Usage stats access not granted")
      _diagnostics.value = ShieldDiagnostics(
        isServiceActive = false,
        isUsageAccessGranted = false,
        isOverlayPermissionGranted = overlayGranted,
        isNotificationPermissionGranted = notifGranted,
        blockedPackageCount = blockedPackages.size,
        isTimerActive = true,
        isStrictMode = isStrictMode,
        statusMessage = "Usage Access permission missing - cannot detect opened apps",
        capabilityLevel = "Disabled"
      )
      return
    }

    if (blockedPackages.isEmpty()) {
      Log.i(TAG, "No packages configured to shield")
      _diagnostics.value = ShieldDiagnostics(
        isServiceActive = false,
        isUsageAccessGranted = true,
        isOverlayPermissionGranted = overlayGranted,
        isNotificationPermissionGranted = notifGranted,
        blockedPackageCount = 0,
        isTimerActive = true,
        isStrictMode = isStrictMode,
        statusMessage = "No apps selected in App Shield list",
        capabilityLevel = "Disabled"
      )
      return
    }

    // Start real Foreground Service
    FocusShieldService.start(
      context = context,
      blockedPackages = blockedPackages.toList(),
      labelsMap = packageNamesToLabels,
      isStrict = isStrictMode
    )

    val capability = if (overlayGranted) {
      "Full Blocking Active"
    } else {
      "Notification Only"
    }

    val statusMsg = if (overlayGranted) {
      "Full Protection Active: Monitoring ${blockedPackages.size} restricted apps"
    } else {
      "Alerts Only: Grant 'Display over other apps' to enable blocking screen"
    }

    _diagnostics.value = ShieldDiagnostics(
      isServiceActive = true,
      isUsageAccessGranted = true,
      isOverlayPermissionGranted = overlayGranted,
      isNotificationPermissionGranted = notifGranted,
      blockedPackageCount = blockedPackages.size,
      isTimerActive = true,
      isStrictMode = isStrictMode,
      statusMessage = statusMsg,
      capabilityLevel = capability
    )
  }

  fun stopMonitoring() {
    FocusShieldService.stop(context)
    val usageGranted = UsagePermissionHelper.isUsageAccessGranted(context)
    val overlayGranted = OverlayPermissionHelper.isOverlayPermissionGranted(context)
    val notifGranted = isNotificationGranted()

    _diagnostics.value = _diagnostics.value.copy(
      isServiceActive = false,
      isTimerActive = false,
      isUsageAccessGranted = usageGranted,
      isOverlayPermissionGranted = overlayGranted,
      isNotificationPermissionGranted = notifGranted,
      statusMessage = "Shield Inactive",
      capabilityLevel = "Disabled"
    )
  }

  fun updateDiagnostics(isTimerActive: Boolean, isStrictMode: Boolean) {
    refreshDiagnostics(
      isTimerActive = isTimerActive,
      isStrictMode = isStrictMode
    )
  }

  private fun refreshDiagnostics(
    isServiceRunning: Boolean = FocusShieldService.isServiceRunning.value,
    isTimerActive: Boolean = _diagnostics.value.isTimerActive,
    isStrictMode: Boolean = _diagnostics.value.isStrictMode
  ) {
    val usageGranted = UsagePermissionHelper.isUsageAccessGranted(context)
    val overlayGranted = OverlayPermissionHelper.isOverlayPermissionGranted(context)
    val notifGranted = isNotificationGranted()
    val blockedCount = FocusShieldService.activeBlockedCount.value.coerceAtLeast(_diagnostics.value.blockedPackageCount)

    val capability = when {
      !isTimerActive -> "Disabled"
      !usageGranted -> "Disabled (Usage Access Missing)"
      blockedCount == 0 -> "Disabled (No Apps Selected)"
      isServiceRunning && overlayGranted -> "Full Blocking Active"
      isServiceRunning && !overlayGranted -> "Notification Only (Overlay Missing)"
      else -> "Inactive"
    }

    val statusMsg = when {
      !isTimerActive -> "Focus session is not running"
      !usageGranted -> "Usage Access permission missing - cannot detect opened apps"
      blockedCount == 0 -> "No apps selected in App Shield list"
      isServiceRunning && overlayGranted -> "Active: Monitoring $blockedCount apps (Interception Screen Ready)"
      isServiceRunning && !overlayGranted -> "Active: Monitoring $blockedCount apps (Display Over Apps required for blocking screen)"
      else -> "Shield Inactive"
    }

    _diagnostics.value = _diagnostics.value.copy(
      isServiceActive = isServiceRunning,
      isUsageAccessGranted = usageGranted,
      isOverlayPermissionGranted = overlayGranted,
      isNotificationPermissionGranted = notifGranted,
      isTimerActive = isTimerActive,
      isStrictMode = isStrictMode,
      blockedPackageCount = blockedCount,
      statusMessage = statusMsg,
      capabilityLevel = capability
    )
  }

  private fun isNotificationGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }
}
