package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analytics.StatisticsViewModel
import com.example.ui.screens.stats.AnalyticsSection
import com.example.ui.screens.stats.FocusCoachSection
import com.example.ui.screens.stats.PlannerSection
import com.example.ui.screens.stats.RewardsSection
import com.example.ui.theme.RadiantViolet

@Composable
fun StatisticsScreen(
  viewModel: StatisticsViewModel,
  onNavigateHome: () -> Unit,
  modifier: Modifier = Modifier
) {
  val selectedTab by viewModel.selectedTabIndex.collectAsStateWithLifecycle()
  val analyticsSummary by viewModel.analyticsSummary.collectAsStateWithLifecycle()
  val filteredSessions by viewModel.filteredSessions.collectAsStateWithLifecycle()
  val historyFilter by viewModel.historyFilter.collectAsStateWithLifecycle()

  val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
  val todayTasks by viewModel.todayTasks.collectAsStateWithLifecycle()

  val totalPoints by viewModel.totalPoints.collectAsStateWithLifecycle()
  val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
  val milestones by viewModel.milestones.collectAsStateWithLifecycle()
  val rewardTransactions by viewModel.rewardTransactions.collectAsStateWithLifecycle()

  val coachRecommendations by viewModel.coachRecommendations.collectAsStateWithLifecycle()

  val scrollState = rememberScrollState()

  LaunchedEffect(selectedTab) {
    if (selectedTab == 2) {
      viewModel.refreshMilestones()
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onNavigateHome,
        modifier = Modifier.testTag("stats_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Home",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
      Column {
        Text(
          text = "Focus Insights & Growth",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "Performance trends, daily planner, milestones & focus coach",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Top Navigation Tabs
    val tabs = listOf(
      "Analytics" to Icons.Default.BarChart,
      "Planner" to Icons.Default.Checklist,
      "Rewards" to Icons.Default.EmojiEvents,
      "Coach" to Icons.Default.Psychology
    )

    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
      contentColor = RadiantViolet,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = RadiantViolet
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("stats_tab_row")
    ) {
      tabs.forEachIndexed { index, (label, icon) ->
        Tab(
          selected = selectedTab == index,
          onClick = { viewModel.setSelectedTab(index) },
          text = { Text(label, style = MaterialTheme.typography.labelMedium) },
          icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.testTag("stats_tab_$index")
        )
      }
    }

    // Tab Contents
    when (selectedTab) {
      0 -> {
        AnalyticsSection(
          summary = analyticsSummary,
          filteredSessions = filteredSessions,
          currentFilter = historyFilter,
          onFilterSelected = { viewModel.setHistoryFilter(it) }
        )
      }

      1 -> {
        PlannerSection(
          todayTasks = todayTasks,
          allTasks = allTasks,
          onToggleTask = { viewModel.toggleTaskCompletion(it) },
          onDeleteTask = { viewModel.deleteTask(it) },
          onSaveTask = { title, notes, date, duration ->
            viewModel.createTask(title, notes, date, duration)
          },
          onUpdateTask = { viewModel.updateTask(it) }
        )
      }

      2 -> {
        RewardsSection(
          totalPoints = totalPoints,
          currentStreak = currentStreak,
          milestones = milestones,
          transactions = rewardTransactions
        )
      }

      3 -> {
        FocusCoachSection(
          recommendations = coachRecommendations
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Return to Home Dashboard
    Button(
      onClick = onNavigateHome,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("stats_return_home_button"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.primary
      ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Text(
        text = "Return to Home Dashboard",
        style = MaterialTheme.typography.labelLarge
      )
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
