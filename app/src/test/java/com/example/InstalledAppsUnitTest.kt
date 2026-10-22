package com.example

import com.example.apps.model.AppInfo
import com.example.apps.repository.InstalledAppsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledAppsUnitTest {

  @Test
  fun testFormattedUsageString() {
    val zeroUsageApp = AppInfo(
      packageName = "com.test.app1",
      appName = "Test App 1",
      usageDurationMillis = 0L
    )
    assertEquals("No usage today", zeroUsageApp.formattedUsage)

    val secondsApp = AppInfo(
      packageName = "com.test.app2",
      appName = "Test App 2",
      usageDurationMillis = 35_000L // 35 seconds
    )
    assertEquals("< 1m today", secondsApp.formattedUsage)

    val minutesApp = AppInfo(
      packageName = "com.test.app3",
      appName = "Test App 3",
      usageDurationMillis = 25 * 60 * 1000L // 25 mins
    )
    assertEquals("25m today", minutesApp.formattedUsage)

    val hoursApp = AppInfo(
      packageName = "com.test.app4",
      appName = "Test App 4",
      usageDurationMillis = (2 * 60 + 15) * 60 * 1000L // 2h 15m
    )
    assertEquals("2h 15m today", hoursApp.formattedUsage)
  }

  @Test
  fun testAppFilteringLogic() {
    // Testing filtering logic as pure function
    val testApps = listOf(
      AppInfo(packageName = "com.instagram.android", appName = "Instagram", usageDurationMillis = 5000L),
      AppInfo(packageName = "com.twitter.android", appName = "X (Twitter)", usageDurationMillis = 10000L),
      AppInfo(packageName = "com.google.android.youtube", appName = "YouTube", usageDurationMillis = 0L),
      AppInfo(packageName = "com.slack", appName = "Slack", usageDurationMillis = 2000L)
    )

    fun filter(apps: List<AppInfo>, query: String): List<AppInfo> {
      val trimmed = query.trim()
      if (trimmed.isEmpty()) return apps
      return apps.filter {
        it.appName.contains(trimmed, ignoreCase = true) ||
          it.packageName.contains(trimmed, ignoreCase = true)
      }
    }

    // Exact name match
    val result1 = filter(testApps, "Instagram")
    assertEquals(1, result1.size)
    assertEquals("Instagram", result1[0].appName)

    // Case-insensitive partial name match
    val result2 = filter(testApps, "tube")
    assertEquals(1, result2.size)
    assertEquals("YouTube", result2[0].appName)

    // Package name match
    val result3 = filter(testApps, "com.twitter")
    assertEquals(1, result3.size)
    assertEquals("X (Twitter)", result3[0].appName)

    // Empty query returns all
    val resultEmpty = filter(testApps, "   ")
    assertEquals(4, resultEmpty.size)

    // Non-matching query returns empty list
    val resultNone = filter(testApps, "Netflix")
    assertTrue(resultNone.isEmpty())
  }
}
