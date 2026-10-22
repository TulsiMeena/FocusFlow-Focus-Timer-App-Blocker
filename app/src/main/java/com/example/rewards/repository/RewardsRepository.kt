package com.example.rewards.repository

import com.example.data.local.dao.RewardDao
import com.example.data.local.entity.RewardTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class MilestoneProgress(
  val id: String,
  val title: String,
  val description: String,
  val bonusPoints: Int,
  val isEarned: Boolean,
  val currentProgress: Int,
  val targetProgress: Int
)

class RewardsRepository(private val rewardDao: RewardDao) {

  val allRewards: Flow<List<RewardTransactionEntity>> = rewardDao.getAllRewards()

  val totalPoints: Flow<Int> = rewardDao.getTotalPointsFlow()

  /**
   * Awards points for a completed focus session.
   * Rule: 10 points per 5 full minutes of focus time (e.g. 25 min = 50 pts).
   * Prevents duplicates via unique (sourceId, sourceType) database index and pre-check.
   */
  suspend fun awardSessionCompleted(sessionId: String, durationSeconds: Int): Int =
    withContext(Dispatchers.IO) {
      if (sessionId.isBlank() || durationSeconds < 300) return@withContext 0

      val sourceType = "SESSION_COMPLETED"
      if (rewardDao.hasReward(sessionId, sourceType)) {
        return@withContext 0 // Already awarded!
      }

      val points = (durationSeconds / 300) * 10
      val minutes = durationSeconds / 60
      val transaction = RewardTransactionEntity(
        sourceId = sessionId,
        sourceType = sourceType,
        points = points,
        description = "Completed $minutes min Focus Session"
      )
      val rowId = rewardDao.insertReward(transaction)
      if (rowId > 0) points else 0
    }

  /**
   * Awards points for completing a daily planned task.
   * Rule: 25 points per completed task.
   */
  suspend fun awardTaskCompleted(taskId: Long, taskTitle: String): Int =
    withContext(Dispatchers.IO) {
      if (taskId <= 0) return@withContext 0
      val sourceId = "task_$taskId"
      val sourceType = "TASK_COMPLETED"

      if (rewardDao.hasReward(sourceId, sourceType)) {
        return@withContext 0
      }

      val points = 25
      val transaction = RewardTransactionEntity(
        sourceId = sourceId,
        sourceType = sourceType,
        points = points,
        description = "Completed Planned Task: $taskTitle"
      )
      val rowId = rewardDao.insertReward(transaction)
      if (rowId > 0) points else 0
    }

  /**
   * Evaluates milestone achievements based on completed sessions count, total minutes, and current streak.
   */
  suspend fun checkAndAwardMilestones(
    completedSessionsCount: Int,
    totalFocusMinutes: Int,
    currentStreak: Int
  ) = withContext(Dispatchers.IO) {
    // Milestone 1: First Session (50 pts)
    if (completedSessionsCount >= 1) {
      awardMilestone("milestone_first_session", "First Focus Completed", 50)
    }
    // Milestone 2: 5 Sessions (100 pts)
    if (completedSessionsCount >= 5) {
      awardMilestone("milestone_5_sessions", "Deep Work Explorer (5 Sessions)", 100)
    }
    // Milestone 3: 100 Minutes (150 pts)
    if (totalFocusMinutes >= 100) {
      awardMilestone("milestone_100_minutes", "Century Club (100 Focus Minutes)", 150)
    }
    // Milestone 4: 300 Minutes (300 pts)
    if (totalFocusMinutes >= 300) {
      awardMilestone("milestone_300_minutes", "Focus Marathon (300 Focus Minutes)", 300)
    }
    // Milestone 5: 3-Day Streak (200 pts)
    if (currentStreak >= 3) {
      awardMilestone("milestone_streak_3", "3-Day Consistency Streak", 200)
    }
    // Milestone 6: 7-Day Streak (500 pts)
    if (currentStreak >= 7) {
      awardMilestone("milestone_streak_7", "7-Day Mastery Streak", 500)
    }
  }

  private suspend fun awardMilestone(id: String, title: String, points: Int) {
    val sourceType = "MILESTONE_UNLOCKED"
    if (!rewardDao.hasReward(id, sourceType)) {
      rewardDao.insertReward(
        RewardTransactionEntity(
          sourceId = id,
          sourceType = sourceType,
          points = points,
          description = "Milestone Unlocked: $title"
        )
      )
    }
  }

  suspend fun getMilestoneProgressList(
    completedSessionsCount: Int,
    totalFocusMinutes: Int,
    currentStreak: Int
  ): List<MilestoneProgress> = withContext(Dispatchers.IO) {
    listOf(
      MilestoneProgress(
        id = "milestone_first_session",
        title = "First Step",
        description = "Complete your 1st focus session",
        bonusPoints = 50,
        isEarned = completedSessionsCount >= 1,
        currentProgress = completedSessionsCount.coerceAtMost(1),
        targetProgress = 1
      ),
      MilestoneProgress(
        id = "milestone_5_sessions",
        title = "Deep Work Explorer",
        description = "Complete 5 focus sessions",
        bonusPoints = 100,
        isEarned = completedSessionsCount >= 5,
        currentProgress = completedSessionsCount.coerceAtMost(5),
        targetProgress = 5
      ),
      MilestoneProgress(
        id = "milestone_100_minutes",
        title = "Century Club",
        description = "Accumulate 100 total focus minutes",
        bonusPoints = 150,
        isEarned = totalFocusMinutes >= 100,
        currentProgress = totalFocusMinutes.coerceAtMost(100),
        targetProgress = 100
      ),
      MilestoneProgress(
        id = "milestone_300_minutes",
        title = "Focus Marathon",
        description = "Accumulate 300 total focus minutes",
        bonusPoints = 300,
        isEarned = totalFocusMinutes >= 300,
        currentProgress = totalFocusMinutes.coerceAtMost(300),
        targetProgress = 300
      ),
      MilestoneProgress(
        id = "milestone_streak_3",
        title = "Consistency Habit",
        description = "Reach a 3-day consecutive focus streak",
        bonusPoints = 200,
        isEarned = currentStreak >= 3,
        currentProgress = currentStreak.coerceAtMost(3),
        targetProgress = 3
      ),
      MilestoneProgress(
        id = "milestone_streak_7",
        title = "Focus Mastery",
        description = "Reach a 7-day consecutive focus streak",
        bonusPoints = 500,
        isEarned = currentStreak >= 7,
        currentProgress = currentStreak.coerceAtMost(7),
        targetProgress = 7
      )
    )
  }
}
