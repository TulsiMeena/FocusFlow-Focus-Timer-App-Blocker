package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.analytics.StatisticsViewModel
import com.example.apps.viewmodel.InstalledAppsViewModel
import com.example.navigation.Screen
import com.example.settings.SettingsViewModel
import com.example.timer.FocusTimerViewModel
import com.example.ui.components.AppBottomBar
import com.example.ui.screens.AppBlockerScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.ui.screens.AppSplashScreen
import com.example.ui.theme.AdvancedFocusTheme

class MainActivity : ComponentActivity() {
  companion object {
    const val EXTRA_NAVIGATE_TO = "EXTRA_NAVIGATE_TO"
  }

  private val targetRouteState = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    targetRouteState.value = intent.getStringExtra(EXTRA_NAVIGATE_TO)

    setContent {
      AdvancedFocusTheme {
        val currentTargetRoute = targetRouteState.value
        var isSplashCompleted by rememberSaveable {
          mutableStateOf(!currentTargetRoute.isNullOrBlank())
        }

        LaunchedEffect(currentTargetRoute) {
          if (!currentTargetRoute.isNullOrBlank()) {
            isSplashCompleted = true
          }
        }

        Crossfade(
          targetState = isSplashCompleted,
          animationSpec = tween(durationMillis = 450),
          label = "splash_to_main_transition"
        ) { splashDone ->
          if (!splashDone) {
            AppSplashScreen(
              onSplashFinished = {
                isSplashCompleted = true
              }
            )
          } else {
            MainAppContent(targetRoute = currentTargetRoute)
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.getStringExtra(EXTRA_NAVIGATE_TO)?.let { route ->
      targetRouteState.value = route
    }
  }
}

@Composable
fun MainAppContent(targetRoute: String? = null) {
  val context = LocalContext.current
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val focusViewModel: FocusTimerViewModel = viewModel()
  val installedAppsViewModel: InstalledAppsViewModel = viewModel()
  val settingsViewModel: SettingsViewModel = viewModel()
  val statisticsViewModel: StatisticsViewModel = viewModel()

  val notifLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = {
      focusViewModel.checkPermissions()
    }
  )

  LaunchedEffect(Unit) {
    focusViewModel.checkPermissions()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          android.Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  LaunchedEffect(targetRoute) {
    if (!targetRoute.isNullOrBlank()) {
      navController.navigate(targetRoute) {
        popUpTo(navController.graph.findStartDestination().id) {
          saveState = true
        }
        launchSingleTop = true
        restoreState = true
      }
    }
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      AppBottomBar(
        currentRoute = currentRoute,
        onNavigate = { targetScreen ->
          navController.navigate(targetScreen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
              saveState = true
            }
            launchSingleTop = true
            restoreState = true
          }
        }
      )
    },
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = Screen.Home.route,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      composable(Screen.Home.route) {
        HomeScreen(
          viewModel = focusViewModel,
          onNavigateToScreen = { targetScreen ->
            navController.navigate(targetScreen.route) {
              popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
              }
              launchSingleTop = true
              restoreState = true
            }
          }
        )
      }

      composable(Screen.Focus.route) {
        FocusScreen(
          viewModel = focusViewModel,
          onNavigateHome = {
            navController.navigate(Screen.Home.route) {
              popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
              }
              launchSingleTop = true
            }
          }
        )
      }

      composable(Screen.AppBlocker.route) {
        AppBlockerScreen(
          viewModel = installedAppsViewModel,
          onNavigateHome = {
            navController.navigate(Screen.Home.route) {
              popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
              }
              launchSingleTop = true
            }
          }
        )
      }

      composable(Screen.Statistics.route) {
        StatisticsScreen(
          viewModel = statisticsViewModel,
          onNavigateHome = {
            navController.navigate(Screen.Home.route) {
              popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
              }
              launchSingleTop = true
            }
          }
        )
      }

      composable(Screen.Settings.route) {
        SettingsScreen(
          viewModel = settingsViewModel,
          onNavigateHome = {
            navController.navigate(Screen.Home.route) {
              popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
              }
              launchSingleTop = true
            }
          }
        )
      }
    }
  }
}
