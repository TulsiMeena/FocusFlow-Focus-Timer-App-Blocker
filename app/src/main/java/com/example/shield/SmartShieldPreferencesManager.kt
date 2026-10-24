package com.example.shield

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MathUnlockChallenge(
  val expression: String,
  val expectedAnswer: Int,
  val hint: String
)

/**
 * Centralized manager for Advanced Focus Shield features:
 * 1. Smart Unlock Challenge (2-minute temporary bypass after solving a brain puzzle)
 * 2. Daily App Screen-Time Limits (per-package daily minute caps)
 * 4. Flip-to-Focus Sensor Auto-Trigger preferences
 * 5. Bedtime / Sleep Shield schedule & auto-lock state
 */
class SmartShieldPreferencesManager private constructor(context: Context) {

  companion object {
    private const val PREFS_NAME = "smart_shield_advanced_prefs_v1"

    private const val KEY_SMART_CHALLENGE_ENABLED = "smart_challenge_enabled"
    private const val KEY_SLEEP_SHIELD_ENABLED = "sleep_shield_enabled"
    private const val KEY_SLEEP_START_HOUR = "sleep_start_hour"
    private const val KEY_SLEEP_START_MINUTE = "sleep_start_minute"
    private const val KEY_SLEEP_END_HOUR = "sleep_end_hour"
    private const val KEY_SLEEP_END_MINUTE = "sleep_end_minute"

    private const val KEY_FLIP_TO_FOCUS_ENABLED = "flip_to_focus_enabled"
    private const val KEY_FLIP_PICKUP_ALERT_ENABLED = "flip_pickup_alert_enabled"

    private const val KEY_PREFIX_DAILY_LIMIT = "app_daily_limit_"
    private const val KEY_PREFIX_TEMP_UNLOCK = "app_temp_unlock_until_"

    @Volatile
    private var INSTANCE: SmartShieldPreferencesManager? = null

    fun getInstance(context: Context): SmartShieldPreferencesManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: SmartShieldPreferencesManager(context.applicationContext).also {
          INSTANCE = it
        }
      }
    }
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  // 1. Smart Unlock Challenge State
  private val _isSmartChallengeEnabled = MutableStateFlow(
    prefs.getBoolean(KEY_SMART_CHALLENGE_ENABLED, true)
  )
  val isSmartChallengeEnabled: StateFlow<Boolean> = _isSmartChallengeEnabled.asStateFlow()

  // 5. Bedtime / Sleep Shield State
  private val _isSleepShieldEnabled = MutableStateFlow(
    prefs.getBoolean(KEY_SLEEP_SHIELD_ENABLED, false)
  )
  val isSleepShieldEnabled: StateFlow<Boolean> = _isSleepShieldEnabled.asStateFlow()

  private val _sleepStartHour = MutableStateFlow(prefs.getInt(KEY_SLEEP_START_HOUR, 23))
  val sleepStartHour: StateFlow<Int> = _sleepStartHour.asStateFlow()

  private val _sleepStartMinute = MutableStateFlow(prefs.getInt(KEY_SLEEP_START_MINUTE, 0))
  val sleepStartMinute: StateFlow<Int> = _sleepStartMinute.asStateFlow()

  private val _sleepEndHour = MutableStateFlow(prefs.getInt(KEY_SLEEP_END_HOUR, 6))
  val sleepEndHour: StateFlow<Int> = _sleepEndHour.asStateFlow()

  private val _sleepEndMinute = MutableStateFlow(prefs.getInt(KEY_SLEEP_END_MINUTE, 0))
  val sleepEndMinute: StateFlow<Int> = _sleepEndMinute.asStateFlow()

  // 4. Flip-to-Focus State
  private val _isFlipToFocusEnabled = MutableStateFlow(
    prefs.getBoolean(KEY_FLIP_TO_FOCUS_ENABLED, false)
  )
  val isFlipToFocusEnabled: StateFlow<Boolean> = _isFlipToFocusEnabled.asStateFlow()

  private val _isFlipPickupAlertEnabled = MutableStateFlow(
    prefs.getBoolean(KEY_FLIP_PICKUP_ALERT_ENABLED, true)
  )
  val isFlipPickupAlertEnabled: StateFlow<Boolean> = _isFlipPickupAlertEnabled.asStateFlow()

  // 2. Per-App Daily Time Limits (packageName -> limit in minutes)
  private val _appDailyLimits = MutableStateFlow(loadAllDailyLimits())
  val appDailyLimits: StateFlow<Map<String, Int>> = _appDailyLimits.asStateFlow()

  private fun loadAllDailyLimits(): Map<String, Int> {
    val result = mutableMapOf<String, Int>()
    for ((key, value) in prefs.all) {
      if (key.startsWith(KEY_PREFIX_DAILY_LIMIT) && value is Int && value > 0) {
        val pkg = key.removePrefix(KEY_PREFIX_DAILY_LIMIT)
        if (pkg.isNotBlank()) {
          result[pkg] = value
        }
      }
    }
    return result
  }

  fun setSmartChallengeEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_SMART_CHALLENGE_ENABLED, enabled).apply()
    _isSmartChallengeEnabled.value = enabled
  }

  fun setSleepShieldEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_SLEEP_SHIELD_ENABLED, enabled).apply()
    _isSleepShieldEnabled.value = enabled
  }

  fun setSleepSchedule(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
    val sh = startHour.coerceIn(0, 23)
    val sm = startMinute.coerceIn(0, 59)
    val eh = endHour.coerceIn(0, 23)
    val em = endMinute.coerceIn(0, 59)
    prefs.edit()
      .putInt(KEY_SLEEP_START_HOUR, sh)
      .putInt(KEY_SLEEP_START_MINUTE, sm)
      .putInt(KEY_SLEEP_END_HOUR, eh)
      .putInt(KEY_SLEEP_END_MINUTE, em)
      .apply()
    _sleepStartHour.value = sh
    _sleepStartMinute.value = sm
    _sleepEndHour.value = eh
    _sleepEndMinute.value = em
  }

  fun setFlipToFocusEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_FLIP_TO_FOCUS_ENABLED, enabled).apply()
    _isFlipToFocusEnabled.value = enabled
  }

  fun setFlipPickupAlertEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_FLIP_PICKUP_ALERT_ENABLED, enabled).apply()
    _isFlipPickupAlertEnabled.value = enabled
  }

  fun setAppDailyLimitMinutes(packageName: String, limitMinutes: Int) {
    if (packageName.isBlank()) return
    val key = KEY_PREFIX_DAILY_LIMIT + packageName
    if (limitMinutes <= 0) {
      prefs.edit().remove(key).apply()
    } else {
      prefs.edit().putInt(key, limitMinutes).apply()
    }
    _appDailyLimits.value = loadAllDailyLimits()
  }

  fun getAppDailyLimitMinutes(packageName: String): Int {
    return _appDailyLimits.value[packageName] ?: 0
  }

  /**
   * Grants a temporary 2-minute emergency pass after the user solves the Smart Brain Challenge.
   */
  fun grantTemporaryUnlock(packageName: String, durationMinutes: Int = 2) {
    if (packageName.isBlank()) return
    val expiryMillis = System.currentTimeMillis() + (durationMinutes * 60_000L)
    prefs.edit().putLong(KEY_PREFIX_TEMP_UNLOCK + packageName, expiryMillis).apply()
    FocusShieldService.notifyOverlayActionTaken()
  }

  fun isPackageTemporarilyUnlocked(packageName: String): Boolean {
    if (packageName.isBlank()) return false
    val expiryMillis = prefs.getLong(KEY_PREFIX_TEMP_UNLOCK + packageName, 0L)
    return System.currentTimeMillis() < expiryMillis
  }

  fun isCurrentlyInSleepWindow(): Boolean {
    if (!_isSleepShieldEnabled.value) return false
    val cal = Calendar.getInstance()
    val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val startMinutes = _sleepStartHour.value * 60 + _sleepStartMinute.value
    val endMinutes = _sleepEndHour.value * 60 + _sleepEndMinute.value

    return if (startMinutes <= endMinutes) {
      nowMinutes in startMinutes until endMinutes
    } else {
      // Overnight window (e.g., 23:00 to 06:00)
      nowMinutes >= startMinutes || nowMinutes < endMinutes
    }
  }

  fun formatSleepWindow(): String {
    return "${formatTime12h(_sleepStartHour.value, _sleepStartMinute.value)} – ${formatTime12h(_sleepEndHour.value, _sleepEndMinute.value)}"
  }

  private fun formatTime12h(hour24: Int, minute: Int): String {
    val period = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
      hour24 == 0 -> 12
      hour24 > 12 -> hour24 - 12
      else -> hour24
    }
    return String.format(Locale.US, "%02d:%02d %s", hour12, minute, period)
  }

  /**
   * Generates a multi-step arithmetic challenge for the Smart Unlock Challenge.
   */
  fun generateMathChallenge(): MathUnlockChallenge {
    val a = Random.nextInt(14, 39)
    val b = Random.nextInt(6, 12)
    val c = Random.nextInt(11, 45)
    val product = a * b
    val total = product + c
    return MathUnlockChallenge(
      expression = "$a × $b + $c = ?",
      expectedAnswer = total,
      hint = "Step 1: $a × $b = $product, then add $c"
    )
  }
}
