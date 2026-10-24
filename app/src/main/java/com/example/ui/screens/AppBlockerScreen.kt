package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.apps.model.AppInfo
import com.example.apps.viewmodel.InstalledAppsViewModel
import com.example.ui.screens.apps.AppIconImage
import com.example.ui.screens.apps.OverlayAccessExplanationDialog
import com.example.ui.screens.apps.UsageAccessExplanationDialog
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

@Composable
fun AppBlockerScreen(
  viewModel: InstalledAppsViewModel,
  onNavigateHome: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val isUsageGranted by viewModel.isUsageAccessGranted.collectAsStateWithLifecycle()
  val isOverlayGranted by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()
  val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
  val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
  val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
  val totalAppCount by viewModel.totalAppCount.collectAsStateWithLifecycle()
  val blockedPackageNames by viewModel.blockedPackageNames.collectAsStateWithLifecycle()
  val blockedAppsCount by viewModel.blockedAppsCount.collectAsStateWithLifecycle()
  val isMasterShieldEnabled by viewModel.isMasterShieldEnabled.collectAsStateWithLifecycle()
  val showExplanationDialog by viewModel.showExplanationDialog.collectAsStateWithLifecycle()
  val showOverlayExplanationDialog by viewModel.showOverlayExplanationDialog.collectAsStateWithLifecycle()
  val appDailyLimits by viewModel.appDailyLimits.collectAsStateWithLifecycle()
  val totalScreenTimeTodayMinutes by viewModel.totalScreenTimeTodayMinutes.collectAsStateWithLifecycle()
  val topUsedAppsToday by viewModel.topUsedAppsToday.collectAsStateWithLifecycle()

  var showOnlyBlocked by remember { mutableStateOf(false) }
  var selectedAppForLimit by remember { mutableStateOf<AppInfo?>(null) }

  // Lifecycle observer: Automatically recheck permission when returning from System Settings
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.checkPermissionAndLoadApps(forceReload = false)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Usage Access Explanation Dialog
  if (showExplanationDialog) {
    UsageAccessExplanationDialog(
      onDismiss = { viewModel.dismissExplanationDialog() },
      onOpenSettings = { viewModel.openUsageSettings(context) }
    )
  }

  // Overlay Permission Explanation & Troubleshooting Dialog
  if (showOverlayExplanationDialog) {
    OverlayAccessExplanationDialog(
      onDismiss = { viewModel.dismissOverlayExplanationDialog() },
      onOpenOverlaySettings = { viewModel.openOverlaySettings(context) },
      onOpenAppDetailsSettings = { viewModel.openAppDetailsSettings(context) },
      onOpenOverlayListSettings = { viewModel.openOverlaySettingsList(context) }
    )
  }

  if (selectedAppForLimit != null) {
    val targetApp = selectedAppForLimit!!
    val currentLimit = appDailyLimits[targetApp.packageName] ?: 0
    AppDailyLimitDialog(
      app = targetApp,
      currentLimitMinutes = currentLimit,
      onDismiss = { selectedAppForLimit = null },
      onSelectLimit = { limitMins ->
        viewModel.setAppDailyLimit(targetApp.packageName, targetApp.appName, limitMins)
        selectedAppForLimit = null
      }
    )
  }

  val displayList = if (showOnlyBlocked) {
    filteredApps.filter { blockedPackageNames.contains(it.packageName) }
  } else {
    filteredApps
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        IconButton(
          onClick = onNavigateHome,
          modifier = Modifier.testTag("apps_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Home",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
        Column {
          Text(
            text = "App Shield & Installed Apps",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Select apps to restrict during Focus sessions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      IconButton(
        onClick = { viewModel.loadApps() },
        modifier = Modifier.testTag("refresh_apps_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Refresh list",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // 1b. Master Shield Power Switch (ON / OFF Option)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("app_blocker_power_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (isMasterShieldEnabled) {
          MaterialTheme.colorScheme.surfaceVariant
        } else {
          MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        }
      ),
      border = BorderStroke(
        1.5.dp,
        if (isMasterShieldEnabled) RadiantViolet.copy(alpha = 0.4f) else MaterialTheme.colorScheme.error
      )
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = if (isMasterShieldEnabled) RadiantViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = if (isMasterShieldEnabled) RadiantViolet else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Column {
            Text(
              text = "App Blocker Shield",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (isMasterShieldEnabled) "Status: ON (Active Protection)" else "Status: TURNED OFF (Bypassed)",
              style = MaterialTheme.typography.bodySmall,
              color = if (isMasterShieldEnabled) RadiantViolet else MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Switch(
          checked = isMasterShieldEnabled,
          onCheckedChange = { checked ->
            viewModel.setMasterShieldEnabled(checked)
          },
          modifier = Modifier.testTag("toggle_app_blocker_master_power"),
          colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.surface,
            checkedTrackColor = RadiantViolet,
            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
            uncheckedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
          )
        )
      }
    }

    // 2. Permission Status Cards
    UsageAccessStatusCard(
      isGranted = isUsageGranted,
      onRequestPermission = { viewModel.showExplanationDialog() }
    )

    OverlayStatusCard(
      isGranted = isOverlayGranted,
      onRequestPermission = { viewModel.openOverlaySettings(context) },
      onShowHelp = { viewModel.showOverlayExplanationDialog() }
    )

    // 3. Search Field
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { viewModel.onSearchQueryChange(it) },
      placeholder = {
        Text("Search apps by name or package...")
      },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { viewModel.clearSearch() }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("app_search_input"),
      shape = RoundedCornerShape(14.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
      )
    )

  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    // 4. Primary Filter Chips (All vs Shielded) & Batch Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      FilterChip(
        selected = !showOnlyBlocked,
        onClick = { showOnlyBlocked = false },
        label = { Text("All ($totalAppCount)") },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          selectedLabelColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.testTag("filter_all_apps")
      )

      FilterChip(
        selected = showOnlyBlocked,
        onClick = { showOnlyBlocked = true },
        label = { Text("Shielded ($blockedAppsCount)") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = if (showOnlyBlocked) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = RadiantViolet.copy(alpha = 0.2f),
          selectedLabelColor = RadiantViolet
        ),
        modifier = Modifier.testTag("filter_shielded_apps")
      )

      Spacer(modifier = Modifier.weight(1f))

      if (blockedAppsCount > 0) {
        TextButton(
          onClick = { viewModel.clearAllBlockedApps() },
          modifier = Modifier.testTag("clear_all_shielded_button")
        ) {
          Text(
            text = "Clear All",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    }

    // 4b. Category Filter Chips & Quick Shield Button
    androidx.compose.foundation.lazy.LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      item {
        Button(
          onClick = { viewModel.blockAllDistractingApps() },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("btn_block_all_distractions")
        ) {
          Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Shield All Distractions", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
      }

      items(com.example.apps.model.AppCategory.entries.toTypedArray()) { cat ->
        FilterChip(
          selected = selectedCategory == cat,
          onClick = { viewModel.selectCategory(cat) },
          label = { Text(cat.displayName) },
          leadingIcon = {
            Icon(
              imageVector = cat.icon,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = if (selectedCategory == cat) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = RadiantViolet.copy(alpha = 0.15f),
            selectedLabelColor = RadiantViolet
          ),
          modifier = Modifier.testTag("filter_cat_${cat.name.lowercase()}")
        )
      }
    }

    // 5. Content List / Loading / Error / Empty States
    when {
      isLoading -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(
              color = RadiantViolet,
              modifier = Modifier.size(36.dp)
            )
            Text(
              text = "Scanning launchable applications...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      errorMessage != null && displayList.isEmpty() -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.padding(16.dp)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = AmberStreak, modifier = Modifier.size(32.dp))
              Text(
                text = "Notice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Button(
                onClick = { viewModel.loadApps() },
                colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet)
              ) {
                Text("Retry")
              }
            }
          }
        }
      }

      displayList.isEmpty() -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (showOnlyBlocked) Icons.Default.Shield else Icons.Default.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(36.dp)
            )
            Text(
              text = if (showOnlyBlocked) "No apps currently shielded" else "No matching apps found",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = if (showOnlyBlocked) "Tap the shield button next to any app below to restrict it during focus sessions." else "Try searching with a different keyword.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      else -> {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item(key = "daily_screen_time_analyzer_header") {
            DailyScreenTimeAnalyzerCard(
              totalMinutesToday = totalScreenTimeTodayMinutes,
              activeLimitsCount = appDailyLimits.size,
              topApps = topUsedAppsToday,
              isUsageGranted = isUsageGranted
            )
          }

          items(
            items = displayList,
            key = { it.packageName }
          ) { app ->
            val isBlocked = blockedPackageNames.contains(app.packageName)
            val dailyLimitMins = appDailyLimits[app.packageName] ?: 0
            AppItemRow(
              app = app,
              isUsageGranted = isUsageGranted,
              isBlocked = isBlocked,
              dailyLimitMinutes = dailyLimitMins,
              onToggleBlock = {
                viewModel.toggleAppBlocked(app.packageName, app.appName)
              },
              onConfigureLimit = {
                selectedAppForLimit = app
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DailyScreenTimeAnalyzerCard(
  totalMinutesToday: Int,
  activeLimitsCount: Int,
  topApps: List<AppInfo>,
  isUsageGranted: Boolean
) {
  val hours = totalMinutesToday / 60
  val mins = totalMinutesToday % 60
  val formattedTotal = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 4.dp)
      .testTag("daily_screen_time_analyzer_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.45f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "📊 Today's Screen-Time & App Limits",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (isUsageGranted) {
              "Real-time usage tracker • Auto-locks when daily limit is reached"
            } else {
              "Enable Usage Access above to track real daily app screen time"
            },
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = ElectricBlue.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f))
        ) {
          Text(
            text = "$formattedTotal today",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }

      if (topApps.isNotEmpty()) {
        val maxAppMillis = topApps.maxOf { it.usageDurationMillis }.coerceAtLeast(60_000L)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          topApps.forEach { topApp ->
            val appMins = (topApp.usageDurationMillis / 60_000L).toInt()
            val ratio = (topApp.usageDurationMillis.toFloat() / maxAppMillis.toFloat()).coerceIn(0.08f, 1f)
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = topApp.appName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(96.dp)
              )
              androidx.compose.material3.LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                  .weight(1f)
                  .height(6.dp),
                color = ElectricBlue,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
              )
              Text(
                text = "${appMins}m",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ElectricBlue
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Tap '⏱️ Limit' on any app below to set a daily cap",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "$activeLimitsCount app limit(s) active",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = RadiantViolet
        )
      }
    }
  }
}

@Composable
private fun AppDailyLimitDialog(
  app: AppInfo,
  currentLimitMinutes: Int,
  onDismiss: () -> Unit,
  onSelectLimit: (Int) -> Unit
) {
  val usedMins = (app.usageDurationMillis / 60_000L).toInt()
  val presets = listOf(15, 30, 45, 60, 90, 120)

  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.Default.LockClock, contentDescription = null, tint = ElectricBlue)
        Column {
          Text(
            text = "Daily Time Limit • ${app.appName}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Used today: ${usedMins}m",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricBlue
          )
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Automatically lock ${app.appName} for the rest of the day once its daily screen-time limit is reached:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        presets.chunked(3).forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            rowItems.forEach { mins ->
              val isSelected = currentLimitMinutes == mins
              OutlinedButton(
                onClick = { onSelectLimit(mins) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                  1.dp,
                  if (isSelected) ElectricBlue else MaterialTheme.colorScheme.outlineVariant
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (isSelected) ElectricBlue.copy(alpha = 0.18f) else Color.Transparent,
                  contentColor = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface
                )
              ) {
                Text(
                  text = "${mins}m/day",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      if (currentLimitMinutes > 0) {
        TextButton(onClick = { onSelectLimit(0) }) {
          Text("Remove Limit", color = MaterialTheme.colorScheme.error)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
private fun UsageAccessStatusCard(
  isGranted: Boolean,
  onRequestPermission: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("usage_status_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isGranted) SuccessEmerald.copy(alpha = 0.08f) else RadiantViolet.copy(alpha = 0.08f)
    ),
    border = BorderStroke(
      1.dp,
      if (isGranted) SuccessEmerald.copy(alpha = 0.3f) else RadiantViolet.copy(alpha = 0.3f)
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = if (isGranted) SuccessEmerald.copy(alpha = 0.15f) else RadiantViolet.copy(alpha = 0.15f),
          modifier = Modifier.size(38.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Security,
              contentDescription = null,
              tint = if (isGranted) SuccessEmerald else RadiantViolet,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Column {
          Text(
            text = if (isGranted) "Focus Shield Supported" else "Usage Access Required for Shield",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = if (isGranted) {
              "Monitors foreground app switches to alert on distractions."
            } else {
              "Grant permission to detect when restricted apps are opened."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (!isGranted) {
        Button(
          onClick = onRequestPermission,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
          modifier = Modifier.testTag("grant_usage_access_button")
        ) {
          Text(
            text = "Enable",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun AppItemRow(
  app: AppInfo,
  isUsageGranted: Boolean,
  isBlocked: Boolean,
  dailyLimitMinutes: Int,
  onToggleBlock: () -> Unit,
  onConfigureLimit: () -> Unit
) {
  val usedMinsToday = (app.usageDurationMillis / 60_000L).toInt()
  val isLimitReached = (dailyLimitMinutes > 0 && usedMinsToday >= dailyLimitMinutes)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("app_item_${app.packageName}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isBlocked || isLimitReached) RadiantViolet.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
    ),
    border = BorderStroke(
      1.dp,
      if (isLimitReached) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
      else if (isBlocked) RadiantViolet.copy(alpha = 0.4f)
      else MaterialTheme.colorScheme.outlineVariant
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AppIconImage(
          drawable = app.icon,
          appName = app.appName,
          modifier = Modifier.size(42.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = app.appName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
              Text(
                text = app.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = if (isUsageGranted) app.formattedUsage else app.packageName,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = if (isLimitReached) MaterialTheme.colorScheme.error
              else if (isUsageGranted && app.usageDurationMillis > 0) ElectricBlue
              else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (dailyLimitMinutes > 0) ElectricBlue.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
              border = BorderStroke(
                1.dp,
                if (dailyLimitMinutes > 0) ElectricBlue.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier
                .clickable(onClick = onConfigureLimit)
                .testTag("btn_limit_${app.packageName}")
            ) {
              Text(
                text = if (isLimitReached) {
                  "🔴 ${dailyLimitMinutes}m Limit Hit"
                } else if (dailyLimitMinutes > 0) {
                  "⏱️ ${dailyLimitMinutes}m/day"
                } else {
                  "⏱️ Set Limit"
                },
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLimitReached) MaterialTheme.colorScheme.error
                else if (dailyLimitMinutes > 0) ElectricBlue
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Shield Toggle Button
      Button(
        onClick = onToggleBlock,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isBlocked) RadiantViolet else MaterialTheme.colorScheme.surfaceContainerHigh,
          contentColor = if (isBlocked) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        border = if (isBlocked) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.testTag("toggle_shield_${app.packageName}")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = if (isBlocked) Icons.Default.Shield else Icons.Outlined.Shield,
            contentDescription = null,
            tint = if (isBlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = if (isBlocked) "Shielded" else "Shield",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun OverlayStatusCard(
  isGranted: Boolean,
  onRequestPermission: () -> Unit,
  onShowHelp: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("overlay_status_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isGranted) SuccessEmerald.copy(alpha = 0.08f) else AmberStreak.copy(alpha = 0.08f)
    ),
    border = BorderStroke(
      1.dp,
      if (isGranted) SuccessEmerald.copy(alpha = 0.3f) else AmberStreak.copy(alpha = 0.3f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = if (isGranted) SuccessEmerald.copy(alpha = 0.15f) else AmberStreak.copy(alpha = 0.15f),
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                tint = if (isGranted) SuccessEmerald else AmberStreak,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Column {
            Text(
              text = if (isGranted) "Display Over Apps Enabled" else "Display Over Apps Required",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (isGranted) {
                "Full blocking screen will intercept restricted apps during focus."
              } else {
                "Required by Android to display the blocking screen over other apps."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (!isGranted) {
          Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = AmberStreak),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("request_overlay_permission_button")
          ) {
            Text(
              text = "Enable",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color.Black
            )
          }
        }
      }

      if (!isGranted) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Switch turns OFF in Settings automatically?",
              style = MaterialTheme.typography.labelSmall,
              color = AmberStreak,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.weight(1f)
            )
            TextButton(
              onClick = onShowHelp,
              modifier = Modifier.testTag("overlay_troubleshoot_help_button")
            ) {
              Text(
                text = "Fix / 3 Dots (⋮)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = RadiantViolet
              )
            }
          }
        }
      }
    }
  }
}

