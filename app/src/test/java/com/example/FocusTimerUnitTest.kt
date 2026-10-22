package com.example

import com.example.timer.TimerMode
import com.example.timer.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerUnitTest {

  @Test
  fun testTimerModeDefaultsAndPresets() {
    assertEquals(25, TimerMode.FOCUS.defaultMinutes)
    assertEquals(5, TimerMode.SHORT_BREAK.defaultMinutes)
    assertEquals(15, TimerMode.LONG_BREAK.defaultMinutes)

    assertTrue(TimerMode.FOCUS.presets.contains(25))
    assertTrue(TimerMode.FOCUS.presets.contains(60))
    assertTrue(TimerMode.SHORT_BREAK.presets.contains(5))
  }

  @Test
  fun testAccurateRemainingTimeCalculation() {
    val durationSeconds = 25 * 60 // 1500s
    val startTime = 1_000_000L
    val targetEndTime = startTime + (durationSeconds * 1000L) // 2_500_000L

    // Case 1: At exact start
    val nowStart = startTime
    val remainingAtStart = ((targetEndTime - nowStart + 999) / 1000).toInt()
    assertEquals(1500, remainingAtStart)

    // Case 2: Halfway through (750s elapsed)
    val nowMid = startTime + (750 * 1000L)
    val remainingAtMid = ((targetEndTime - nowMid + 999) / 1000).toInt()
    assertEquals(750, remainingAtMid)

    // Case 3: Target reached
    val nowEnd = targetEndTime
    val remainingAtEnd = ((targetEndTime - nowEnd + 999) / 1000).toInt()
    assertEquals(0, remainingAtEnd)

    // Case 4: Target passed
    val nowPast = targetEndTime + 5000L
    val remainingPast = ((targetEndTime - nowPast + 999) / 1000).toInt()
    assertTrue(remainingPast <= 0)
  }

  @Test
  fun testPauseAndResumeTargetRecalculation() {
    val durationSeconds = 1500
    val start = 1_000_000L
    val originalTarget = start + (durationSeconds * 1000L)

    // User pauses after 300 seconds
    val pauseTime = start + (300 * 1000L)
    val remainingOnPause = ((originalTarget - pauseTime + 999) / 1000).toInt()
    assertEquals(1200, remainingOnPause)

    // User resumes 10 minutes later (600 seconds of real-world delay)
    val resumeTime = pauseTime + (600 * 1000L)
    val newTargetEndTime = resumeTime + (remainingOnPause * 1000L)

    // Immediately after resume, remaining seconds should still be exactly 1200
    val remainingAtResume = ((newTargetEndTime - resumeTime + 999) / 1000).toInt()
    assertEquals(1200, remainingAtResume)
  }

  @Test
  fun testCancellationElapsedCalculation() {
    val totalSeconds = 25 * 60
    val remainingSeconds = 10 * 60 // 15 minutes elapsed
    val elapsedSeconds = (totalSeconds - remainingSeconds).coerceAtLeast(0)
    assertEquals(15 * 60, elapsedSeconds)
  }

  @Test
  fun testFocusPresetsValidation() {
    val presets = com.example.presets.FocusPreset.ALL_PRESETS
    assertTrue(presets.isNotEmpty())
    val pomodoro = presets.firstOrNull { it.id == "pomodoro" }
    org.junit.Assert.assertNotNull(pomodoro)
    assertEquals(25, pomodoro?.focusMinutes)
    assertEquals(5, pomodoro?.breakMinutes)
  }

  @Test
  fun testAppCategorization() {
    val socialCat = com.example.apps.model.AppCategory.fromPackageAndName("com.instagram.android", "Instagram")
    assertEquals(com.example.apps.model.AppCategory.SOCIAL, socialCat)

    val gameCat = com.example.apps.model.AppCategory.fromPackageAndName("com.supercell.clashofclans", "Clash of Clans")
    assertEquals(com.example.apps.model.AppCategory.GAMES, gameCat)

    val entCat = com.example.apps.model.AppCategory.fromPackageAndName("com.google.android.youtube", "YouTube")
    assertEquals(com.example.apps.model.AppCategory.ENTERTAINMENT, entCat)
  }

  @Test
  fun testFocusTreeStages() {
    val seed = com.example.garden.calculateTreeStage(0.1f)
    assertEquals(com.example.garden.TreeStage.SEED, seed)

    val sprout = com.example.garden.calculateTreeStage(0.4f)
    assertEquals(com.example.garden.TreeStage.SPROUT, sprout)

    val sapling = com.example.garden.calculateTreeStage(0.7f)
    assertEquals(com.example.garden.TreeStage.SAPLING, sapling)

    val blooming = com.example.garden.calculateTreeStage(0.98f)
    assertEquals(com.example.garden.TreeStage.BLOOMING, blooming)

    val withered = com.example.garden.calculateTreeStage(0.5f, isWithered = true)
    assertEquals(com.example.garden.TreeStage.WITHERED, withered)
  }

  @Test
  fun testDailyFocusQuotes() {
    val quote = com.example.quotes.FocusQuotesRepository.getDailyQuote()
    org.junit.Assert.assertNotNull(quote)
    assertTrue(quote.text.isNotBlank())
    assertTrue(quote.author.isNotBlank())
  }
}
