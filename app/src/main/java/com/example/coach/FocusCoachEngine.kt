package com.example.coach

import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TaskEntity
import java.util.Calendar

data class CoachAdvice(
  val id: String,
  val category: String,
  val headline: String,
  val suggestion: String,
  val transparentTriggerReason: String,
  val iconName: String
)

object FocusCoachEngine {

  /**
   * Generates transparent, rule-based offline productivity advice.
   * Completely offline, deterministic, and free of medical/guilt claims.
   */
  fun generateRecommendations(
    completedSessions: List<SessionRecord>,
    todayTasks: List<TaskEntity>,
    todayFocusMinutes: Int,
    currentStreak: Int,
    currentCalendar: Calendar = Calendar.getInstance()
  ): List<CoachAdvice> {
    val list = mutableListOf<CoachAdvice>()

    // Case 1: Empty or insufficient session history -> Transparent Starter Guidance
    if (completedSessions.isEmpty()) {
      list.add(
        CoachAdvice(
          id = "starter_rhythm",
          category = "Starter Guide",
          headline = "Start With a Manageable 15-Minute Sprint",
          suggestion = "Starting is the hardest part. Begin with a single 15-minute or 25-minute session today on a single clear objective to build momentum.",
          transparentTriggerReason = "Shown because no completed focus sessions are recorded in your local history yet.",
          iconName = "timer"
        )
      )
      list.add(
        CoachAdvice(
          id = "starter_planner",
          category = "Planning",
          headline = "Pick One Core Priority in Planner",
          suggestion = "Use the Focus Planner tab to write down just 1 or 2 tasks for today. Assigning a planned duration creates clear boundaries for your work.",
          transparentTriggerReason = "Shown to help guide initial task organization.",
          iconName = "task"
        )
      )
      list.add(
        CoachAdvice(
          id = "starter_distraction",
          category = "Environment",
          headline = "Prepare a Distraction-Free Workspace",
          suggestion = "Silence unnecessary phone alerts using Android Do Not Disturb or configure blocked apps before starting your timer.",
          transparentTriggerReason = "General evidence-based preparation advice.",
          iconName = "shield"
        )
      )
      return list
    }

    val mostRecentSession = completedSessions.firstOrNull()
    val now = System.currentTimeMillis()

    // Case 2: Post-long session rest recommendation
    if (mostRecentSession != null) {
      val durationMins = mostRecentSession.actualDurationSeconds / 60
      val elapsedSinceEndMinutes = (now - mostRecentSession.endTimestamp) / 60_000
      if (durationMins >= 45 && elapsedSinceEndMinutes in 0..30) {
        list.add(
          CoachAdvice(
            id = "rest_after_deep_work",
            category = "Pacing & Recovery",
            headline = "Take a Real Rest Break (${durationMins}m Completed)",
            suggestion = "You just completed an intense $durationMins-minute session. Step away from your desk, stretch, and rest your eyes for 5–10 minutes before starting another sprint.",
            transparentTriggerReason = "Triggered because your last session was 45+ minutes and ended within the past 30 minutes.",
            iconName = "break"
          )
        )
      }
    }

    // Case 3: Planner integration - tasks remaining today
    val pendingTasks = todayTasks.filter { !it.isCompleted }
    if (pendingTasks.isNotEmpty()) {
      val topTask = pendingTasks.first()
      list.add(
        CoachAdvice(
          id = "target_pending_task",
          category = "Task Prioritization",
          headline = "Focus on \"${topTask.title}\"",
          suggestion = "You have ${pendingTasks.size} incomplete task(s) on today's planner. Commit your next timer to \"${topTask.title}\" (${topTask.plannedDurationMinutes}m planned).",
          transparentTriggerReason = "Triggered because you have active unfinished tasks scheduled for today.",
          iconName = "task"
        )
      )
    }

    // Case 4: Streak preservation encouragement
    val todayCompletedCount = completedSessions.count {
      val cal = Calendar.getInstance().apply { timeInMillis = it.startTimestamp }
      cal.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR)
    }

    if (currentStreak >= 1 && todayCompletedCount == 0) {
      list.add(
        CoachAdvice(
          id = "protect_streak",
          category = "Consistency",
          headline = "Keep Your $currentStreak-Day Streak Going",
          suggestion = "You have an active $currentStreak-day streak. Log at least one 15 or 25-minute focus session today to preserve your momentum.",
          transparentTriggerReason = "Triggered because you have an active streak of $currentStreak days and haven't logged a completed session today.",
          iconName = "streak"
        )
      )
    }

    // Case 5: Deep work volume pacing
    if (todayFocusMinutes >= 120) {
      list.add(
        CoachAdvice(
          id = "daily_volume_pacing",
          category = "Sustainable Energy",
          headline = "Strong Daily Volume (${todayFocusMinutes}m Today)",
          suggestion = "You've completed $todayFocusMinutes minutes of deep focus today. Research indicates high cognitive efficiency diminishes after 3–4 deep work hours. Prioritize restorative rest.",
          transparentTriggerReason = "Triggered because your completed focus time today exceeded 2 hours.",
          iconName = "timer"
        )
      )
    } else if (todayFocusMinutes in 1..40) {
      list.add(
        CoachAdvice(
          id = "momentum_building",
          category = "Momentum",
          headline = "Great Start Today (${todayFocusMinutes}m Logged)",
          suggestion = "You've successfully overcome starting inertia today. A second session later will solidify today's progress.",
          transparentTriggerReason = "Triggered because you started strong today with under 40 minutes logged so far.",
          iconName = "timer"
        )
      )
    }

    // Default habit tip if list is small
    if (list.size < 2) {
      list.add(
        CoachAdvice(
          id = "evidence_based_tip",
          category = "Focus Habit",
          headline = "Single-Tasking Principle",
          suggestion = "Avoid toggling between communications and work during a focus block. Keep your phone facing down and note sudden thoughts on paper to review later.",
          transparentTriggerReason = "Core offline productivity recommendation.",
          iconName = "info"
        )
      )
    }

    return list
  }
}
