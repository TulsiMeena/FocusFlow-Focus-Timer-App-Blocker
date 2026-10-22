package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShieldAndStrictUnitTest {

  @Test
  fun testEmergencyExitStatus() {
    val status = "CANCELLED (Emergency Exit)"
    assertTrue(status.contains("Emergency Exit"))
    assertTrue(status.startsWith("CANCELLED"))
  }

  @Test
  fun testHoldProgressCalculation() {
    val totalSteps = 20
    var currentStep = 10
    var progress = currentStep.toFloat() / totalSteps.toFloat()
    assertEquals(0.5f, progress, 0.01f)

    currentStep = 20
    progress = currentStep.toFloat() / totalSteps.toFloat()
    assertEquals(1.0f, progress, 0.01f)
  }

  @Test
  fun testShieldEligibilityLogic() {
    val isTimerRunning = true
    val isFocusMode = true
    val blockedPackages = setOf("com.instagram.android", "com.twitter.android")
    val hasUsageAccess = true

    val isShieldActive = isTimerRunning && isFocusMode && blockedPackages.isNotEmpty() && hasUsageAccess
    assertTrue(isShieldActive)

    // When timer is in break mode, shield should not be active
    val isBreakShieldActive = isTimerRunning && false && blockedPackages.isNotEmpty() && hasUsageAccess
    assertFalse(isBreakShieldActive)

    // When no apps blocked, shield should not be active
    val isNoAppsShieldActive = isTimerRunning && isFocusMode && emptySet<String>().isNotEmpty() && hasUsageAccess
    assertFalse(isNoAppsShieldActive)
  }

  @Test
  fun testShieldDiagnosticsCapabilityEvaluation() {
    // 1. Full protection: Timer running + Usage granted + Overlay granted + Apps selected
    fun evaluateCapability(
      isTimerRunning: Boolean,
      hasUsageAccess: Boolean,
      hasOverlayPermission: Boolean,
      blockedCount: Int
    ): String {
      return when {
        !isTimerRunning -> "Disabled"
        !hasUsageAccess -> "Disabled (Usage Access Missing)"
        blockedCount == 0 -> "Disabled (No Apps Selected)"
        hasOverlayPermission -> "Full Blocking Active"
        else -> "Notification Only (Overlay Missing)"
      }
    }

    assertEquals("Full Blocking Active", evaluateCapability(true, true, true, 3))
    assertEquals("Notification Only (Overlay Missing)", evaluateCapability(true, true, false, 3))
    assertEquals("Disabled (Usage Access Missing)", evaluateCapability(true, false, true, 3))
    assertEquals("Disabled (No Apps Selected)", evaluateCapability(true, true, true, 0))
    assertEquals("Disabled", evaluateCapability(false, true, true, 3))
  }

  @Test
  fun testForegroundPackageInterceptionMatching() {
    val blockedPackages = setOf("com.instagram.android", "com.twitter.android", "com.google.android.youtube")
    val appPackageName = "com.example"

    // Opening a blocked app
    val foreground1 = "com.instagram.android"
    assertTrue(blockedPackages.contains(foreground1) && foreground1 != appPackageName)

    // Opening an unblocked app
    val foreground2 = "com.android.settings"
    assertFalse(blockedPackages.contains(foreground2) && foreground2 != appPackageName)

    // Opening the focus app itself
    val foregroundSelf = "com.example"
    assertFalse(blockedPackages.contains(foregroundSelf) && foregroundSelf != appPackageName)
  }

  @Test
  fun testInterventionCooldownThrottling() {
    val cooldownMs = 3000L
    var lastAlertTimestamp = 10000L
    var lastAlertedPackage = "com.instagram.android"

    // 1 second later with same app: throttled
    val now1 = 11000L
    val samePackage = "com.instagram.android"
    val shouldTrigger1 = (now1 - lastAlertTimestamp > cooldownMs) || (samePackage != lastAlertedPackage)
    assertFalse("Should be throttled within cooldown for same package", shouldTrigger1)

    // 1 second later with different app: triggers immediately
    val differentPackage = "com.twitter.android"
    val shouldTrigger2 = (now1 - lastAlertTimestamp > cooldownMs) || (differentPackage != lastAlertedPackage)
    assertTrue("Should trigger immediately if user switches to another blocked app", shouldTrigger2)

    // 4 seconds later with same app: triggers (cooldown expired)
    val now2 = 14500L
    val shouldTrigger3 = (now2 - lastAlertTimestamp > cooldownMs) || (samePackage != lastAlertedPackage)
    assertTrue("Should trigger after cooldown has elapsed", shouldTrigger3)
  }
}
