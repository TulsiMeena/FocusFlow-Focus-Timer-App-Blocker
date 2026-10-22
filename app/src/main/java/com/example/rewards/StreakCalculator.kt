package com.example.rewards

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StreakCalculator {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

  /**
   * Calculates the consecutive daily focus streak.
   *
   * Rules:
   * 1. A day qualifies if at least one completed focus session occurred on that calendar day ("yyyy-MM-dd").
   * 2. If the user has completed a focus session today, today counts as day 1 + consecutive preceding days.
   * 3. If the user has not yet completed a focus session today, but completed one yesterday, the streak remains active (counts from yesterday backwards).
   * 4. If neither today nor yesterday has a completed session, the active streak is 0.
   */
  fun calculateStreak(
    completedDates: Set<String>,
    currentCalendar: Calendar = Calendar.getInstance()
  ): Int {
    if (completedDates.isEmpty()) return 0

    val todayCalendar = currentCalendar.clone() as Calendar
    val todayStr = dateFormat.format(todayCalendar.time)

    val yesterdayCalendar = currentCalendar.clone() as Calendar
    yesterdayCalendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = dateFormat.format(yesterdayCalendar.time)

    var count = 0
    val checkCalendar = currentCalendar.clone() as Calendar

    if (completedDates.contains(todayStr)) {
      // Streak includes today
      count = 1
      checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
      while (completedDates.contains(dateFormat.format(checkCalendar.time))) {
        count++
        checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
      }
    } else if (completedDates.contains(yesterdayStr)) {
      // Streak intact pending today's session
      count = 1
      checkCalendar.add(Calendar.DAY_OF_YEAR, -2)
      while (completedDates.contains(dateFormat.format(checkCalendar.time))) {
        count++
        checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
      }
    } else {
      count = 0
    }

    return count
  }

  fun getTodayString(calendar: Calendar = Calendar.getInstance()): String {
    return dateFormat.format(calendar.time)
  }
}
