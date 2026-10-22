package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
  val route: String,
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  data object Home : Screen(
    route = "home",
    title = "Home",
    selectedIcon = Icons.Filled.Home,
    unselectedIcon = Icons.Outlined.Home,
    testTag = "nav_home"
  )

  data object Focus : Screen(
    route = "focus",
    title = "Focus",
    selectedIcon = Icons.Filled.HourglassTop,
    unselectedIcon = Icons.Outlined.HourglassTop,
    testTag = "nav_focus"
  )

  data object AppBlocker : Screen(
    route = "app_blocker",
    title = "Apps",
    selectedIcon = Icons.Filled.Security,
    unselectedIcon = Icons.Outlined.Security,
    testTag = "nav_apps"
  )

  data object Statistics : Screen(
    route = "statistics",
    title = "Stats",
    selectedIcon = Icons.Filled.BarChart,
    unselectedIcon = Icons.Outlined.BarChart,
    testTag = "nav_stats"
  )

  data object Settings : Screen(
    route = "settings",
    title = "Settings",
    selectedIcon = Icons.Filled.Settings,
    unselectedIcon = Icons.Outlined.Settings,
    testTag = "nav_settings"
  )

  companion object {
    // Custom getter prevents circular class initialization NPE during <clinit>
    val bottomNavItems: List<Screen>
      get() = listOf(Home, Focus, AppBlocker, Statistics, Settings)
  }
}
