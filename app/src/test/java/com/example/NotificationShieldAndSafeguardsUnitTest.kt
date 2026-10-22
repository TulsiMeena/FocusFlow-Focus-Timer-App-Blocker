package com.example

import com.example.data.local.entity.TimerSettingsEntity
import com.example.security.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationShieldAndSafeguardsUnitTest {

  @Test
  fun testPinHashingAndVerification() {
    val pin = "4567"
    val salt = SecurityHelper.generateSalt()
    assertTrue(salt.isNotEmpty())

    val hashed = SecurityHelper.hashPin(pin, salt)
    assertTrue(hashed.isNotEmpty())

    // Correct PIN matches
    val isCorrect = SecurityHelper.verifyPin(pin, salt, hashed)
    assertTrue("Valid PIN should verify successfully", isCorrect)

    // Wrong PIN does not match
    val isWrong = SecurityHelper.verifyPin("9999", salt, hashed)
    assertFalse("Incorrect PIN should fail verification", isWrong)

    // Empty PIN fails
    assertFalse(SecurityHelper.verifyPin("", salt, hashed))
    assertFalse(SecurityHelper.verifyPin(pin, "", hashed))
    assertFalse(SecurityHelper.verifyPin(pin, salt, ""))
  }

  @Test
  fun testPinSaltingProvidesDifferentHashesForSamePin() {
    val pin = "1234"
    val salt1 = SecurityHelper.generateSalt()
    val salt2 = SecurityHelper.generateSalt()
    assertNotEquals(salt1, salt2)

    val hash1 = SecurityHelper.hashPin(pin, salt1)
    val hash2 = SecurityHelper.hashPin(pin, salt2)
    assertNotEquals("Different salts must yield different hashes", hash1, hash2)
  }

  @Test
  fun testNotificationSettingsDefaults() {
    val defaultSettings = TimerSettingsEntity()

    assertTrue("Session completion notifications should default to enabled", defaultSettings.notifyOnSessionComplete)
    assertTrue("Shield distraction notifications should default to enabled", defaultSettings.notifyOnShieldAlert)
    assertTrue("Sound alerts should default to enabled", defaultSettings.soundAlertsEnabled)
    assertTrue("Vibration alerts should default to enabled", defaultSettings.vibrationAlertsEnabled)
    assertTrue("Focus reminders should default to enabled", defaultSettings.focusRemindersEnabled)
    assertFalse("Settings protection should default to disabled", defaultSettings.isSettingsProtectionEnabled)
    assertEquals("", defaultSettings.hashedPin)
    assertEquals("", defaultSettings.recoveryHint)
  }

  @Test
  fun testSettingsProtectionLifecycle() {
    val salt = SecurityHelper.generateSalt()
    val hashedPin = SecurityHelper.hashPin("7890", salt)
    val recoveryHint = "Pet's name"

    val protectedSettings = TimerSettingsEntity(
      isSettingsProtectionEnabled = true,
      hashedPin = hashedPin,
      salt = salt,
      recoveryHint = recoveryHint
    )

    assertTrue(protectedSettings.isSettingsProtectionEnabled)
    assertEquals(recoveryHint, protectedSettings.recoveryHint)
    assertTrue(SecurityHelper.verifyPin("7890", protectedSettings.salt, protectedSettings.hashedPin))
    assertFalse(SecurityHelper.verifyPin("0000", protectedSettings.salt, protectedSettings.hashedPin))

    // Unprotected state
    val disabledSettings = protectedSettings.copy(
      isSettingsProtectionEnabled = false,
      hashedPin = "",
      salt = "",
      recoveryHint = ""
    )
    assertFalse(disabledSettings.isSettingsProtectionEnabled)
    assertEquals("", disabledSettings.hashedPin)
    assertEquals("", disabledSettings.recoveryHint)
  }

  @Test
  fun testNotificationPreferencesCustomization() {
    val base = TimerSettingsEntity()
    val customized = base.copy(
      notifyOnSessionComplete = false,
      notifyOnShieldAlert = true,
      soundAlertsEnabled = false,
      vibrationAlertsEnabled = true,
      focusRemindersEnabled = false
    )

    assertFalse(customized.notifyOnSessionComplete)
    assertTrue(customized.notifyOnShieldAlert)
    assertFalse(customized.soundAlertsEnabled)
    assertTrue(customized.vibrationAlertsEnabled)
    assertFalse(customized.focusRemindersEnabled)
  }
}
