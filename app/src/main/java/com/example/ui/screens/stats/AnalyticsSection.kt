package com.example.ui.screens.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analytics.AnalyticsSummary
import com.example.analytics.DayFocusStat
import com.example.data.local.entity.SessionRecord
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

@Composable
fun AnalyticsSection(
  summary: AnalyticsSummary,
  filteredSessions: List<SessionRecord>,
  currentFilter: String,
  onFilterSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // 4 Core Metrics Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      val totalHours = summary.totalFocusMinutes / 60
      val remainingMinutes = summary.totalFocusMinutes % 60
      val formattedTotal = if (totalHours > 0) "${totalHours}h ${remainingMinutes}m" else "${remainingMinutes}m"

      StatMetricCard(
        title = "Total Focus",
        value = formattedTotal,
        subtitle = "${summary.activeDaysCount} active days",
        icon = Icons.Default.QueryBuilder,
        accentColor = ElectricBlue,
        modifier = Modifier.weight(1f),
        testTag = "metric_total_focus_time"
      )

      StatMetricCard(
        title = "Completed",
        value = "${summary.totalCompletedSessions}",
        subtitle = "Sessions finished",
        icon = Icons.Default.CheckCircle,
        accentColor = SuccessEmerald,
        modifier = Modifier.weight(1f),
        testTag = "metric_completed_sessions"
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      StatMetricCard(
        title = "Daily Average",
        value = "${summary.dailyAverageMinutes}m",
        subtitle = "Per active day",
        icon = Icons.Default.TrendingUp,
        accentColor = RadiantViolet,
        modifier = Modifier.weight(1f),
        testTag = "metric_daily_average"
      )

      StatMetricCard(
        title = "Today's Focus",
        value = "${summary.todayFocusMinutes}m",
        subtitle = "${summary.todayCompletedSessions} sessions today",
        icon = Icons.Default.HourglassBottom,
        accentColor = CyanAccent,
        modifier = Modifier.weight(1f),
        testTag = "metric_today_focus"
      )
    }

    // 7-Day Visual Bar Chart
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("weekly_focus_chart_card"),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DateRange,
              contentDescription = null,
              tint = CyanAccent,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "Last 7 Days Focus Distribution",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Text(
            text = "${summary.weeklySummary.totalWeekMinutes}m total",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = CyanAccent
          )
        }

        // Visual Bars
        WeeklyBarChart(
          dailyStats = summary.weeklySummary.dailyStats,
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
        )

        // Summary note
        if (summary.totalCancelledSessions > 0) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = AmberStreak,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "${summary.totalCancelledSessions} cancelled session(s) tracked separately and excluded from totals.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Session History Review
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Session History Log",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("ALL", "COMPLETED", "CANCELLED").forEach { filter ->
          FilterChip(
            selected = currentFilter == filter,
            onClick = { onFilterSelected(filter) },
            label = { Text(filter.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = RadiantViolet.copy(alpha = 0.2f),
              selectedLabelColor = RadiantViolet
            ),
            modifier = Modifier.testTag("filter_chip_${filter.lowercase()}")
          )
        }
      }
    }

    if (filteredSessions.isEmpty()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("empty_history_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.HourglassTop,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp)
          )
          Text(
            text = "No focus sessions found for this filter",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Completed and cancelled sessions are saved automatically to your local database.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        filteredSessions.take(15).forEach { session ->
          SessionHistoryItemCard(session = session)
        }
      }
    }
  }
}

@Composable
private fun WeeklyBarChart(
  dailyStats: List<DayFocusStat>,
  modifier: Modifier = Modifier
) {
  val maxMinutes = (dailyStats.maxOfOrNull { it.focusMinutes } ?: 0).coerceAtLeast(60)

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom
  ) {
    dailyStats.forEach { stat ->
      val ratio = (stat.focusMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.04f, 1f)
      val isToday = stat == dailyStats.lastOrNull()

      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
      ) {
        Text(
          text = if (stat.focusMinutes > 0) "${stat.focusMinutes}m" else "0",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          color = if (isToday) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
          modifier = Modifier
            .width(18.dp)
            .height((100 * ratio).dp)
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(
              if (isToday) ElectricBlue else if (stat.focusMinutes > 0) RadiantViolet else MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = stat.dayLabel,
          style = MaterialTheme.typography.labelSmall,
          fontSize = 11.sp,
          color = if (isToday) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
        )
      }
    }
  }
}

@Composable
private fun StatMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
  testTag: String
) {
  Card(
    modifier = modifier.testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
          shape = CircleShape,
          color = accentColor.copy(alpha = 0.15f),
          modifier = Modifier.size(28.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun SessionHistoryItemCard(
  session: SessionRecord,
  modifier: Modifier = Modifier
) {
  val isCompleted = session.status == "COMPLETED"
  val durationMins = session.actualDurationSeconds / 60

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = if (isCompleted) SuccessEmerald.copy(alpha = 0.15f) else AmberStreak.copy(alpha = 0.15f),
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.HourglassBottom,
              contentDescription = null,
              tint = if (isCompleted) SuccessEmerald else AmberStreak,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Column {
          Text(
            text = "${session.sessionType.lowercase().replaceFirstChar { it.uppercase() }} • ${durationMins}m logged",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (session.dateFormatted.isNotBlank()) session.dateFormatted else "Session #${session.id}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isCompleted) SuccessEmerald.copy(alpha = 0.15f) else AmberStreak.copy(alpha = 0.15f)
      ) {
        Text(
          text = session.status,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (isCompleted) SuccessEmerald else AmberStreak,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
  }
}
