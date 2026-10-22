package com.example.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coach.CoachAdvice
import com.example.coach.FocusCoachEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entity.RewardTransactionEntity
import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.SessionRepository
import com.example.planner.repository.PlannerRepository
import com.example.rewards.StreakCalculator
import com.example.rewards.repository.MilestoneProgress
import com.example.rewards.repository.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

  private val sessionRepository: SessionRepository
  private val plannerRepository: PlannerRepository
  private val rewardsRepository: RewardsRepository

  init {
    val database = AppDatabase.getInstance(application)
    sessionRepository = SessionRepository(
      database.sessionDao(),
      database.timerSettingsDao(),
      database.activeTimerDao(),
      database.blockedAppDao(),
      database.rewardDao()
    )
    plannerRepository = PlannerRepository(database.taskDao())
    rewardsRepository = RewardsRepository(database.rewardDao())
  }

  // Selected Tab in Stats Screen: 0 = Analytics, 1 = Planner, 2 = Rewards, 3 = Coach
  private val _selectedTabIndex = MutableStateFlow(0)
  val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

  fun setSelectedTab(index: Int) {
    _selectedTabIndex.value = index.coerceIn(0, 3)
  }

  // Session History Filter: "ALL", "COMPLETED", "CANCELLED"
  private val _historyFilter = MutableStateFlow("ALL")
  val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

  fun setHistoryFilter(filter: String) {
    _historyFilter.value = filter
  }

  val allSessions: StateFlow<List<SessionRecord>> = sessionRepository.allSessions
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val filteredSessions: StateFlow<List<SessionRecord>> = combine(
    allSessions,
    _historyFilter
  ) { sessions, filter ->
    when (filter) {
      "COMPLETED" -> sessions.filter { it.status == "COMPLETED" }
      "CANCELLED" -> sessions.filter { it.status != "COMPLETED" }
      else -> sessions
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Computed Analytics Summary
  val analyticsSummary: StateFlow<AnalyticsSummary> = allSessions.combine(_historyFilter) { sessions, _ ->
    AnalyticsCalculator.computeAnalytics(sessions)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = AnalyticsCalculator.computeAnalytics(emptyList())
  )

  // Planner Tasks
  val allTasks: StateFlow<List<TaskEntity>> = plannerRepository.allTasks
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  private val todayKey: String
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

  val todayTasks: StateFlow<List<TaskEntity>> = allTasks.combine(_selectedTabIndex) { tasks, _ ->
    tasks.filter { it.targetDate == todayKey }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Rewards & Streaks
  val totalPoints: StateFlow<Int> = rewardsRepository.totalPoints
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

  val rewardTransactions: StateFlow<List<RewardTransactionEntity>> = rewardsRepository.allRewards
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val currentStreak: StateFlow<Int> = allSessions.combine(_selectedTabIndex) { sessions, _ ->
    val completedDates = sessions
      .filter { it.sessionType == "FOCUS" && it.status == "COMPLETED" }
      .map { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.startTimestamp)) }
      .toSet()
    StreakCalculator.calculateStreak(completedDates)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0
  )

  private val _milestones = MutableStateFlow<List<MilestoneProgress>>(emptyList())
  val milestones: StateFlow<List<MilestoneProgress>> = _milestones.asStateFlow()

  fun refreshMilestones() {
    viewModelScope.launch {
      val summary = analyticsSummary.value
      val streak = currentStreak.value
      rewardsRepository.checkAndAwardMilestones(
        completedSessionsCount = summary.totalCompletedSessions,
        totalFocusMinutes = summary.totalFocusMinutes,
        currentStreak = streak
      )
      _milestones.value = rewardsRepository.getMilestoneProgressList(
        completedSessionsCount = summary.totalCompletedSessions,
        totalFocusMinutes = summary.totalFocusMinutes,
        currentStreak = streak
      )
    }
  }

  // Focus Coach Recommendations
  val coachRecommendations: StateFlow<List<CoachAdvice>> = combine(
    allSessions,
    todayTasks,
    currentStreak
  ) { sessions, tasks, streak ->
    val completedOnly = sessions.filter { it.sessionType == "FOCUS" && it.status == "COMPLETED" }
    val todayMins = completedOnly.filter {
      SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.startTimestamp)) == todayKey
    }.sumOf { it.actualDurationSeconds } / 60

    FocusCoachEngine.generateRecommendations(
      completedSessions = completedOnly,
      todayTasks = tasks,
      todayFocusMinutes = todayMins,
      currentStreak = streak,
      currentCalendar = Calendar.getInstance()
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = FocusCoachEngine.generateRecommendations(emptyList(), emptyList(), 0, 0)
  )

  // Dialog States for Planner
  val showCreateTaskDialog = MutableStateFlow(false)
  val taskBeingEdited = MutableStateFlow<TaskEntity?>(null)

  init {
    refreshMilestones()
  }

  fun createTask(title: String, notes: String, targetDate: String, plannedDuration: Int) {
    viewModelScope.launch {
      val effectiveDate = if (targetDate.isBlank()) todayKey else targetDate
      plannerRepository.createTask(title, notes, effectiveDate, plannedDuration)
      showCreateTaskDialog.value = false
    }
  }

  fun updateTask(task: TaskEntity) {
    viewModelScope.launch {
      plannerRepository.updateTask(task)
      taskBeingEdited.value = null
    }
  }

  fun toggleTaskCompletion(task: TaskEntity) {
    viewModelScope.launch {
      val updated = plannerRepository.toggleTaskCompletion(task.id)
      if (updated != null && updated.isCompleted) {
        rewardsRepository.awardTaskCompleted(updated.id, updated.title)
        refreshMilestones()
      }
    }
  }

  fun deleteTask(taskId: Long) {
    viewModelScope.launch {
      plannerRepository.deleteTask(taskId)
    }
  }
}
