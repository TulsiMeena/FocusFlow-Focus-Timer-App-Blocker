package com.example.analytics

import com.example.data.local.entity.SessionRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayFocusStat(
  val dayLabel: String,      // e.g. "Mon", "Tue"
  val fullDateLabel: String, // e.g. "Oct 11"
  val dateKey: String,       // e.g. "2026-10-11"
  val focusMinutes: Int,
  val completedSessionsCount: Int
)

data class WeeklySummary(
  val totalWeekMinutes: Int,
  val totalWeekCompletedSessions: Int,
  val dailyStats: List<DayFocusStat> // 7 days ending today
)

data class AnalyticsSummary(
  val totalFocusMinutes: Int,
  val totalCompletedSessions: Int,
  val totalCancelledSessions: Int,
  val todayFocusMinutes: Int,
  val todayCompletedSessions: Int,
  val dailyAverageMinutes: Int,
  val activeDaysCount: Int,
  val weeklySummary: WeeklySummary
)

object AnalyticsCalculator {

  private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  private val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
  private val fullDateLabelFormat = SimpleDateFormat("MMM d", Locale.getDefault())

  fun computeAnalytics(
    allSessions: List<SessionRecord>,
    calendar: Calendar = Calendar.getInstance()
  ): AnalyticsSummary {
    val completedFocusSessions = allSessions.filter {
      it.sessionType == "FOCUS" && it.status == "COMPLETED"
    }

    val cancelledFocusSessions = allSessions.filter {
      it.sessionType == "FOCUS" && it.status != "COMPLETED"
    }

    val totalFocusMinutes = completedFocusSessions.sumOf { it.actualDurationSeconds } / 60
    val totalCompletedSessions = completedFocusSessions.size
    val totalCancelledSessions = cancelledFocusSessions.size

    val todayKey = dateKeyFormat.format(calendar.time)

    val todayCompletedSessions = completedFocusSessions.filter {
      dateKeyFormat.format(Date(it.startTimestamp)) == todayKey
    }
    val todayFocusMinutes = todayCompletedSessions.sumOf { it.actualDurationSeconds } / 60
    val todayCompletedCount = todayCompletedSessions.size

    // Distinct calendar days with at least one completed focus session
    val distinctActiveDays = completedFocusSessions
      .map { dateKeyFormat.format(Date(it.startTimestamp)) }
      .toSet()

    val activeDaysCount = distinctActiveDays.size
    val dailyAverageMinutes = if (activeDaysCount > 0) {
      totalFocusMinutes / activeDaysCount
    } else {
      0
    }

    // Compute Last 7 Days (ending today)
    val dailyStats = mutableListOf<DayFocusStat>()
    val tempCal = calendar.clone() as Calendar
    tempCal.add(Calendar.DAY_OF_YEAR, -6) // Start 6 days ago

    for (i in 0 until 7) {
      val dayKey = dateKeyFormat.format(tempCal.time)
      val dayLabel = dayLabelFormat.format(tempCal.time)
      val fullDateLabel = fullDateLabelFormat.format(tempCal.time)

      val sessionsOnDay = completedFocusSessions.filter {
        dateKeyFormat.format(Date(it.startTimestamp)) == dayKey
      }
      val minutesOnDay = sessionsOnDay.sumOf { it.actualDurationSeconds } / 60

      dailyStats.add(
        DayFocusStat(
          dayLabel = dayLabel,
          fullDateLabel = fullDateLabel,
          dateKey = dayKey,
          focusMinutes = minutesOnDay,
          completedSessionsCount = sessionsOnDay.size
        )
      )
      tempCal.add(Calendar.DAY_OF_YEAR, 1)
    }

    val totalWeekMinutes = dailyStats.sumOf { it.focusMinutes }
    val totalWeekCompletedSessions = dailyStats.sumOf { it.completedSessionsCount }

    val weeklySummary = WeeklySummary(
      totalWeekMinutes = totalWeekMinutes,
      totalWeekCompletedSessions = totalWeekCompletedSessions,
      dailyStats = dailyStats
    )

    return AnalyticsSummary(
      totalFocusMinutes = totalFocusMinutes,
      totalCompletedSessions = totalCompletedSessions,
      totalCancelledSessions = totalCancelledSessions,
      todayFocusMinutes = todayFocusMinutes,
      todayCompletedSessions = todayCompletedCount,
      dailyAverageMinutes = dailyAverageMinutes,
      activeDaysCount = activeDaysCount,
      weeklySummary = weeklySummary
    )
  }
}
