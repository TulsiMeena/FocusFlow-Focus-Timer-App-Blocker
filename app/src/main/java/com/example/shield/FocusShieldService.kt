package com.example.shield

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.apps.util.OverlayPermissionHelper
import com.example.apps.util.UsagePermissionHelper
import com.example.timer.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service that enforces App Blocking and Focus Shield protection
 * during active focus sessions.
 */
class FocusShieldService : Service() {

  companion object {
    private const val TAG = "FocusShieldService"

    const val ACTION_START = "com.example.shield.action.START"
    const val ACTION_STOP = "com.example.shield.action.STOP"
    const val ACTION_UPDATE = "com.example.shield.action.UPDATE"

    const val EXTRA_BLOCKED_PACKAGES = "EXTRA_BLOCKED_PACKAGES"
    const val EXTRA_LABELS_KEYS = "EXTRA_LABELS_KEYS"
    const val EXTRA_LABELS_VALUES = "EXTRA_LABELS_VALUES"
    const val EXTRA_IS_STRICT = "EXTRA_IS_STRICT"

    private const val POLL_INTERVAL_MS = 500L
    private const val QUERY_WINDOW_MS = 10_000L
    private const val INTERVENTION_COOLDOWN_MS = 3_000L

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _lastDetectedEvent = MutableStateFlow<String?>(null)
    val lastDetectedEvent: StateFlow<String?> = _lastDetectedEvent.asStateFlow()

    private val _lastDetectedPackage = MutableStateFlow<String?>(null)
    val lastDetectedPackage: StateFlow<String?> = _lastDetectedPackage.asStateFlow()

    private val _activeBlockedCount = MutableStateFlow(0)
    val activeBlockedCount: StateFlow<Int> = _activeBlockedCount.asStateFlow()

    fun start(
      context: Context,
      blockedPackages: List<String>,
      labelsMap: Map<String, String>,
      isStrict: Boolean
    ) {
      val intent = Intent(context, FocusShieldService::class.java).apply {
        action = ACTION_START
        putStringArrayListExtra(EXTRA_BLOCKED_PACKAGES, ArrayList(blockedPackages))
        putStringArrayListExtra(EXTRA_LABELS_KEYS, ArrayList(labelsMap.keys))
        putStringArrayListExtra(EXTRA_LABELS_VALUES, ArrayList(labelsMap.values))
        putExtra(EXTRA_IS_STRICT, isStrict)
      }

      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to start FocusShieldService", e)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, FocusShieldService::class.java).apply {
        action = ACTION_STOP
      }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to stop FocusShieldService", e)
      }
    }

    fun update(
      context: Context,
      blockedPackages: List<String>,
      labelsMap: Map<String, String>,
      isStrict: Boolean
    ) {
      if (!_isServiceRunning.value) return
      val intent = Intent(context, FocusShieldService::class.java).apply {
        action = ACTION_UPDATE
        putStringArrayListExtra(EXTRA_BLOCKED_PACKAGES, ArrayList(blockedPackages))
        putStringArrayListExtra(EXTRA_LABELS_KEYS, ArrayList(labelsMap.keys))
        putStringArrayListExtra(EXTRA_LABELS_VALUES, ArrayList(labelsMap.values))
        putExtra(EXTRA_IS_STRICT, isStrict)
      }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to update FocusShieldService", e)
      }
    }
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private lateinit var notificationHelper: NotificationHelper
  private lateinit var overlayManager: FocusBlockOverlayManager
  private var monitorJob: Job? = null

  private var blockedPackageSet: Set<String> = emptySet()
  private var labelsMap: Map<String, String> = emptyMap()
  private var isStrictMode: Boolean = false

  private var lastAlertTimeMillis = 0L
  private var lastAlertedPackage = ""
  private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

  override fun onCreate() {
    super.onCreate()
    notificationHelper = NotificationHelper(applicationContext)
    overlayManager = FocusBlockOverlayManager(applicationContext)
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent == null) return START_NOT_STICKY

    when (intent.action) {
      ACTION_STOP -> {
        Log.i(TAG, "Stopping FocusShieldService")
        stopMonitoring()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        _isServiceRunning.value = false
        return START_NOT_STICKY
      }

      ACTION_START, ACTION_UPDATE -> {
        val packages = intent.getStringArrayListExtra(EXTRA_BLOCKED_PACKAGES) ?: arrayListOf()
        val keys = intent.getStringArrayListExtra(EXTRA_LABELS_KEYS) ?: arrayListOf()
        val values = intent.getStringArrayListExtra(EXTRA_LABELS_VALUES) ?: arrayListOf()
        val strict = intent.getBooleanExtra(EXTRA_IS_STRICT, false)

        val reconstructedLabels = mutableMapOf<String, String>()
        for (i in 0 until minOf(keys.size, values.size)) {
          reconstructedLabels[keys[i]] = values[i]
        }

        blockedPackageSet = packages.toSet()
        labelsMap = reconstructedLabels
        isStrictMode = strict
        _activeBlockedCount.value = blockedPackageSet.size

        // Start Foreground with persistent notification
        val notification = notificationHelper.buildForegroundServiceNotification(
          blockedCount = blockedPackageSet.size,
          isStrictMode = isStrictMode
        )

        try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
              this,
              NotificationHelper.SERVICE_NOTIFICATION_ID,
              notification,
              ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
          } else {
            startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed startForeground", e)
          startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true
        startMonitoring()
      }
    }

    return START_STICKY
  }

  private fun isTransientSystemUiOrIme(pkg: String): Boolean {
    return pkg == "com.android.systemui" ||
      pkg == "android" ||
      pkg.contains("inputmethod") ||
      pkg.contains("keyboard")
  }

  private fun startMonitoring() {
    monitorJob?.cancel()

    if (blockedPackageSet.isEmpty()) {
      Log.i(TAG, "No blocked packages configured for monitoring")
      overlayManager.hideBlockOverlay()
      return
    }

    val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    if (usageStatsManager == null) {
      Log.e(TAG, "UsageStatsManager service unavailable")
      return
    }

    monitorJob = serviceScope.launch {
      Log.d(TAG, "Monitoring loop started for ${blockedPackageSet.size} apps (strict=$isStrictMode)")
      while (isActive) {
        val now = System.currentTimeMillis()

        if (UsagePermissionHelper.isUsageAccessGranted(applicationContext)) {
          var foregroundPackage: String? = null

          // 1. Check UsageEvents with a 30-second window
          val events = try {
            usageStatsManager.queryEvents(now - 30_000L, now)
          } catch (e: Exception) {
            null
          }

          if (events != null) {
            val event = UsageEvents.Event()
            var latestEventTime = 0L
            while (events.hasNextEvent()) {
              events.getNextEvent(event)
              val pkg = event.packageName
              if (pkg != null && pkg != packageName && !isTransientSystemUiOrIme(pkg)) {
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                  if (event.timeStamp >= latestEventTime) {
                    latestEventTime = event.timeStamp
                    foregroundPackage = pkg
                  }
                }
              }
            }
          }

          // 2. Fallback to queryUsageStats if no foreground event identified
          if (foregroundPackage == null) {
            try {
              val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                now - 60_000L,
                now
              )
              val recent = statsList
                ?.filter { it.packageName != null && it.packageName != packageName && !isTransientSystemUiOrIme(it.packageName) }
                ?.maxByOrNull { it.lastTimeUsed }
              if (recent != null && (now - recent.lastTimeUsed) < 60_000L) {
                foregroundPackage = recent.packageName
              }
            } catch (_: Exception) {
            }
          }

          // 3. Match against blocked packages
          if (foregroundPackage != null && blockedPackageSet.contains(foregroundPackage)) {
            val appLabel = labelsMap[foregroundPackage] ?: foregroundPackage
            val timeSinceLast = now - lastAlertTimeMillis

            if (timeSinceLast > INTERVENTION_COOLDOWN_MS || lastAlertedPackage != foregroundPackage || !overlayManager.isShowing) {
              lastAlertTimeMillis = now
              lastAlertedPackage = foregroundPackage
              _lastDetectedPackage.value = foregroundPackage

              val timestampStr = timeFormat.format(Date(now))
              Log.i(TAG, "Restricted app intercepted: $appLabel ($foregroundPackage) [strict=$isStrictMode]")

              val hasOverlayPermission = OverlayPermissionHelper.isOverlayPermissionGranted(applicationContext)

              if (hasOverlayPermission) {
                // Tier 1: System-level full-screen WindowManager overlay covering the restricted app
                overlayManager.showBlockOverlay(appLabel, foregroundPackage, isStrictMode)

                // Launch BlockScreenActivity for platforms permitting background activity starts
                val blockIntent = Intent(applicationContext, BlockScreenActivity::class.java).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                  putExtra(BlockScreenActivity.EXTRA_BLOCKED_APP_NAME, appLabel)
                  putExtra(BlockScreenActivity.EXTRA_BLOCKED_PACKAGE_NAME, foregroundPackage)
                  putExtra(BlockScreenActivity.EXTRA_IS_STRICT, isStrictMode)
                }
                try {
                  startActivity(blockIntent)
                } catch (_: Exception) {
                }

                _lastDetectedEvent.value = "Blocked $appLabel at $timestampStr (Active Interception)"
              } else {
                // Tier 2: Overlay permission not granted - attempt activity launch, navigate home and send high-priority alert
                val blockIntent = Intent(applicationContext, BlockScreenActivity::class.java).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                  putExtra(BlockScreenActivity.EXTRA_BLOCKED_APP_NAME, appLabel)
                  putExtra(BlockScreenActivity.EXTRA_BLOCKED_PACKAGE_NAME, foregroundPackage)
                  putExtra(BlockScreenActivity.EXTRA_IS_STRICT, isStrictMode)
                }
                var activityStarted = false
                try {
                  startActivity(blockIntent)
                  activityStarted = true
                } catch (_: Exception) {
                }

                if (!activityStarted) {
                  overlayManager.goToHomeScreen()
                }
                notificationHelper.sendFocusShieldAlertNotification(appLabel)
                _lastDetectedEvent.value = "Alerted for $appLabel at $timestampStr (Interception active)"
              }
            }
          } else {
            // Foreground package is not in the blocked set (e.g. user returned to home or focus app)
            if (overlayManager.isShowing && foregroundPackage != null && !blockedPackageSet.contains(foregroundPackage)) {
              overlayManager.hideBlockOverlay()
              lastAlertedPackage = ""
            }
          }
        }

        delay(POLL_INTERVAL_MS)
      }
    }
  }

  private fun stopMonitoring() {
    monitorJob?.cancel()
    monitorJob = null
    lastAlertedPackage = ""
    overlayManager.hideBlockOverlay()
  }

  override fun onDestroy() {
    super.onDestroy()
    stopMonitoring()
    overlayManager.hideBlockOverlay()
    serviceScope.cancel()
    _isServiceRunning.value = false
    Log.i(TAG, "FocusShieldService destroyed")
  }
}
