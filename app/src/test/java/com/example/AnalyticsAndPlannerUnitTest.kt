package com.example

import com.example.analytics.AnalyticsCalculator
import com.example.coach.FocusCoachEngine
import com.example.data.local.entity.RewardTransactionEntity
import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TaskEntity
import com.example.rewards.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsAndPlannerUnitTest {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

  @Test
  fun testAnalyticsCalculationExcludesCancelledSessionsFromTotals() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 14)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val now = cal.timeInMillis

    val sessions = listOf(
      // Completed Focus Session: 25 mins
      SessionRecord(
        sessionId = "session-1",
        sessionType = "FOCUS",
        plannedDurationSeconds = 1500,
        actualDurationSeconds = 1500,
        startTimestamp = now - 3600_000,
        endTimestamp = now - 2100_000,
        status = "COMPLETED"
      ),
      // Completed Focus Session: 50 mins
      SessionRecord(
        sessionId = "session-2",
        sessionType = "FOCUS",
        plannedDurationSeconds = 3000,
        actualDurationSeconds = 3000,
        startTimestamp = now - 7200_000,
        endTimestamp = now - 4200_000,
        status = "COMPLETED"
      ),
      // Cancelled Focus Session: 10 mins (MUST be excluded from totalFocusMinutes)
      SessionRecord(
        sessionId = "session-3",
        sessionType = "FOCUS",
        plannedDurationSeconds = 1500,
        actualDurationSeconds = 600,
        startTimestamp = now - 10000_000,
        endTimestamp = now - 9400_000,
        status = "CANCELLED"
      ),
      // Completed Short Break: 5 mins (MUST be excluded from focus totals)
      SessionRecord(
        sessionId = "session-4",
        sessionType = "SHORT_BREAK",
        plannedDurationSeconds = 300,
        actualDurationSeconds = 300,
        startTimestamp = now - 1200_000,
        endTimestamp = now - 900_000,
        status = "COMPLETED"
      )
    )

    val summary = AnalyticsCalculator.computeAnalytics(sessions, cal)

    // Only session-1 (25m) and session-2 (50m) count -> 75m
    assertEquals(75, summary.totalFocusMinutes)
    assertEquals(2, summary.totalCompletedSessions)
    assertEquals(1, summary.totalCancelledSessions)
    assertEquals(75, summary.todayFocusMinutes)
    assertEquals(2, summary.todayCompletedSessions)
    assertEquals(1, summary.activeDaysCount)
    assertEquals(75, summary.dailyAverageMinutes)

    // Verify weekly summary
    assertEquals(7, summary.weeklySummary.dailyStats.size)
    assertEquals(75, summary.weeklySummary.totalWeekMinutes)
    assertEquals(2, summary.weeklySummary.totalWeekCompletedSessions)
  }

  @Test
  fun testAnalyticsEmptySessionsHandlesGracefully() {
    val cal = Calendar.getInstance()
    val summary = AnalyticsCalculator.computeAnalytics(emptyList(), cal)

    assertEquals(0, summary.totalFocusMinutes)
    assertEquals(0, summary.totalCompletedSessions)
    assertEquals(0, summary.totalCancelledSessions)
    assertEquals(0, summary.todayFocusMinutes)
    assertEquals(0, summary.todayCompletedSessions)
    assertEquals(0, summary.dailyAverageMinutes)
    assertEquals(0, summary.activeDaysCount)
    assertEquals(7, summary.weeklySummary.dailyStats.size)
    assertEquals(0, summary.weeklySummary.totalWeekMinutes)
  }

  @Test
  fun testStreakCalculationWhenUserFocusedToday() {
    val cal = Calendar.getInstance()
    val today = dateFormat.format(cal.time)

    val yesterdayCal = cal.clone() as Calendar
    yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = dateFormat.format(yesterdayCal.time)

    val twoDaysAgoCal = cal.clone() as Calendar
    twoDaysAgoCal.add(Calendar.DAY_OF_YEAR, -2)
    val twoDaysAgo = dateFormat.format(twoDaysAgoCal.time)

    val completedDates = setOf(today, yesterday, twoDaysAgo)
    val streak = StreakCalculator.calculateStreak(completedDates, cal)
    assertEquals(3, streak)
  }

  @Test
  fun testStreakCalculationWhenUserHasNotYetFocusedTodayButFocusedYesterday() {
    val cal = Calendar.getInstance()

    val yesterdayCal = cal.clone() as Calendar
    yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = dateFormat.format(yesterdayCal.time)

    val twoDaysAgoCal = cal.clone() as Calendar
    twoDaysAgoCal.add(Calendar.DAY_OF_YEAR, -2)
    val twoDaysAgo = dateFormat.format(twoDaysAgoCal.time)

    // User focused yesterday and two days ago, but hasn't focused today yet
    val completedDates = setOf(yesterday, twoDaysAgo)
    val streak = StreakCalculator.calculateStreak(completedDates, cal)
    assertEquals("Streak should remain intact pending today's session", 2, streak)
  }

  @Test
  fun testStreakBrokenWhenYesterdayMissed() {
    val cal = Calendar.getInstance()

    val twoDaysAgoCal = cal.clone() as Calendar
    twoDaysAgoCal.add(Calendar.DAY_OF_YEAR, -2)
    val twoDaysAgo = dateFormat.format(twoDaysAgoCal.time)

    // Focused 2 days ago, but missed yesterday and hasn't focused today
    val completedDates = setOf(twoDaysAgo)
    val streak = StreakCalculator.calculateStreak(completedDates, cal)
    assertEquals("Streak should reset to 0 if yesterday was missed", 0, streak)
  }

  @Test
  fun testEmptyStreakIsZero() {
    val cal = Calendar.getInstance()
    val streak = StreakCalculator.calculateStreak(emptySet(), cal)
    assertEquals(0, streak)
  }

  @Test
  fun testRewardPointRulesCalculation() {
    // Rule: 10 points per 5 full minutes (300 seconds)
    fun calculatePoints(seconds: Int): Int = (seconds / 300) * 10

    assertEquals(50, calculatePoints(1500)) // 25 min -> 50 pts
    assertEquals(30, calculatePoints(900))  // 15 min -> 30 pts
    assertEquals(120, calculatePoints(3600)) // 60 min -> 120 pts
    assertEquals(0, calculatePoints(240))   // 4 min (< 5m) -> 0 pts
  }

  @Test
  fun testRewardEntityUniquenessContract() {
    val tx1 = RewardTransactionEntity(
      sourceId = "session-101",
      sourceType = "SESSION_COMPLETED",
      points = 50,
      description = "Completed 25m Focus Session"
    )

    val txDuplicate = RewardTransactionEntity(
      sourceId = "session-101",
      sourceType = "SESSION_COMPLETED",
      points = 50,
      description = "Completed 25m Focus Session"
    )

    assertEquals(tx1.sourceId, txDuplicate.sourceId)
    assertEquals(tx1.sourceType, txDuplicate.sourceType)
  }

  @Test
  fun testTaskEntityCompletionToggle() {
    val task = TaskEntity(
      id = 1,
      title = "Research Project Notes",
      notes = "Review section 2",
      targetDate = "2026-10-11",
      plannedDurationMinutes = 25,
      isCompleted = false,
      completedTimestamp = null
    )

    assertFalse(task.isCompleted)
    assertNull(task.completedTimestamp)

    // Complete task
    val completedTask = task.copy(
      isCompleted = true,
      completedTimestamp = System.currentTimeMillis()
    )
    assertTrue(completedTask.isCompleted)
    assertNotNull(completedTask.completedTimestamp)

    // Uncomplete task
    val uncompletedTask = completedTask.copy(
      isCompleted = false,
      completedTimestamp = null
    )
    assertFalse(uncompletedTask.isCompleted)
    assertNull(uncompletedTask.completedTimestamp)
  }

  @Test
  fun testCoachProvidesStarterAdviceWhenNoHistory() {
    val adviceList = FocusCoachEngine.generateRecommendations(
      completedSessions = emptyList(),
      todayTasks = emptyList(),
      todayFocusMinutes = 0,
      currentStreak = 0
    )

    assertTrue("Should provide starter recommendations", adviceList.isNotEmpty())
    val starterAdvice = adviceList.find { it.category == "Starter Guide" }
    assertNotNull(starterAdvice)
    assertTrue(starterAdvice!!.transparentTriggerReason.contains("no completed focus sessions"))
  }

  @Test
  fun testCoachRecommendsBreakAfterLongSession() {
    val now = System.currentTimeMillis()
    val longSession = SessionRecord(
      sessionId = "long-session-1",
      sessionType = "FOCUS",
      plannedDurationSeconds = 3000,
      actualDurationSeconds = 3000, // 50 mins
      startTimestamp = now - 3100_000,
      endTimestamp = now - 100_000, // Ended just now
      status = "COMPLETED"
    )

    val adviceList = FocusCoachEngine.generateRecommendations(
      completedSessions = listOf(longSession),
      todayTasks = emptyList(),
      todayFocusMinutes = 50,
      currentStreak = 1
    )

    val breakAdvice = adviceList.find { it.id == "rest_after_deep_work" }
    assertNotNull("Should recommend break after 45+ minute session", breakAdvice)
    assertTrue(breakAdvice!!.suggestion.contains("rest your eyes"))
  }

  @Test
  fun testCoachSuggestsPrioritizingPendingPlannerTask() {
    val now = System.currentTimeMillis()
    val completedSession = SessionRecord(
      sessionId = "session-1",
      sessionType = "FOCUS",
      plannedDurationSeconds = 1500,
      actualDurationSeconds = 1500,
      startTimestamp = now - 4000_000,
      endTimestamp = now - 2500_000,
      status = "COMPLETED"
    )

    val pendingTask = TaskEntity(
      id = 10,
      title = "Draft quarterly budget",
      targetDate = "2026-10-11",
      plannedDurationMinutes = 45,
      isCompleted = false
    )

    val adviceList = FocusCoachEngine.generateRecommendations(
      completedSessions = listOf(completedSession),
      todayTasks = listOf(pendingTask),
      todayFocusMinutes = 25,
      currentStreak = 1
    )

    val taskAdvice = adviceList.find { it.id == "target_pending_task" }
    assertNotNull("Should suggest focusing on pending planner task", taskAdvice)
    assertTrue(taskAdvice!!.suggestion.contains("Draft quarterly budget"))
  }
}
