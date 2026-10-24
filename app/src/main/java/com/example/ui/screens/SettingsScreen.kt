package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.settings.SettingsViewModel
import com.example.ui.screens.settings.DndExplanationDialog
import com.example.ui.screens.settings.NotificationListenerExplanationDialog
import com.example.ui.screens.settings.PinRecoveryDialog
import com.example.ui.screens.settings.PinSetupDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Backup
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.screens.settings.RestoreConfirmationDialog
import kotlinx.coroutines.launch
import com.example.ui.screens.apps.OverlayAccessExplanationDialog
import com.example.ui.screens.settings.PinVerifyDialog
import com.example.ui.screens.settings.UninstallSafeguardsDialog
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.RadiantViolet
import com.example.ui.theme.SuccessEmerald

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateHome: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()

  // Auto-refresh DND and Overlay permissions when returning from Android System Settings
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.checkDndPermission()
        viewModel.checkOverlayPermission()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val settings by viewModel.timerSettings.collectAsStateWithLifecycle()
  val isDndGranted by viewModel.isDndAccessGranted.collectAsStateWithLifecycle()
  val isOverlayGranted by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()
  val isMasterShieldEnabled by viewModel.isMasterShieldEnabled.collectAsStateWithLifecycle()
  val isShieldServiceRunning by viewModel.isShieldServiceRunning.collectAsStateWithLifecycle()
  val blockedAppCount by viewModel.blockedAppCount.collectAsStateWithLifecycle()

  var showResetConfirmDialog by remember { mutableStateOf(false) }
  var showClearBlockersConfirmDialog by remember { mutableStateOf(false) }

  val showPinSetup by viewModel.showPinSetupDialog.collectAsStateWithLifecycle()
  val showPinVerify by viewModel.showPinVerifyDialog.collectAsStateWithLifecycle()
  val showPinRecovery by viewModel.showPinRecoveryDialog.collectAsStateWithLifecycle()
  val showUninstallInfo by viewModel.showUninstallInfoDialog.collectAsStateWithLifecycle()
  val showDndInfo by viewModel.showDndExplanationDialog.collectAsStateWithLifecycle()
  val showNotificationListenerInfo by viewModel.showNotificationListenerExplanationDialog.collectAsStateWithLifecycle()
  val showOverlayInfo by viewModel.showOverlayExplanationDialog.collectAsStateWithLifecycle()

  val showRestoreConfirm by viewModel.showRestoreConfirmDialog.collectAsStateWithLifecycle()
  val pendingBackupValidation by viewModel.pendingBackupValidation.collectAsStateWithLifecycle()
  val backupStatusMessage by viewModel.backupStatusMessage.collectAsStateWithLifecycle()

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      scope.launch {
        try {
          val json = viewModel.exportBackupJson()
          context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
          }
          viewModel.backupStatusMessage.value = "Backup successfully exported to file."
        } catch (e: Exception) {
          viewModel.backupStatusMessage.value = "Export failed: ${e.message}"
        }
      }
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      scope.launch {
        try {
          val content = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.reader(Charsets.UTF_8).readText()
          } ?: ""
          viewModel.importBackupContent(content)
        } catch (e: Exception) {
          viewModel.backupStatusMessage.value = "Failed to read backup file: ${e.message}"
        }
      }
    }
  }

  val scrollState = rememberScrollState()

  // Dialogs
  if (showRestoreConfirm && pendingBackupValidation != null) {
    RestoreConfirmationDialog(
      summary = pendingBackupValidation!!.summary,
      onDismiss = { viewModel.dismissRestoreDialog() },
      onConfirmRestore = { mode ->
        viewModel.executeRestore(mode)
      }
    )
  }
  if (showPinSetup) {
    PinSetupDialog(
      onDismiss = { viewModel.showPinSetupDialog.value = false },
      onPinConfigured = { pin, hint ->
        viewModel.enablePinProtection(pin, hint)
      }
    )
  }

  if (showPinVerify) {
    PinVerifyDialog(
      expectedHash = settings.hashedPin,
      salt = settings.salt,
      onDismiss = { viewModel.showPinVerifyDialog.value = false },
      onVerified = {
        viewModel.onPinVerifiedSuccessfully()
      },
      onForgotPinClicked = {
        viewModel.showPinVerifyDialog.value = false
        viewModel.showPinRecoveryDialog.value = true
      }
    )
  }

  if (showPinRecovery) {
    PinRecoveryDialog(
      recoveryHint = settings.recoveryHint,
      onDismiss = { viewModel.showPinRecoveryDialog.value = false },
      onPinReset = { newPin, newHint ->
        viewModel.resetPinViaEmergencyRecovery(newPin, newHint)
      }
    )
  }

  if (showUninstallInfo) {
    UninstallSafeguardsDialog(
      onDismiss = { viewModel.showUninstallInfoDialog.value = false },
      onOpenAppInfoSettings = {
        viewModel.showUninstallInfoDialog.value = false
        viewModel.openAppDetailsSettings(context)
      }
    )
  }

  if (showDndInfo) {
    DndExplanationDialog(
      onDismiss = { viewModel.showDndExplanationDialog.value = false },
      onOpenDndSettings = {
        viewModel.showDndExplanationDialog.value = false
        viewModel.openDndSettings(context)
      }
    )
  }

  if (showNotificationListenerInfo) {
    NotificationListenerExplanationDialog(
      onDismiss = { viewModel.showNotificationListenerExplanationDialog.value = false },
      onOpenSystemNotificationSettings = {
        viewModel.showNotificationListenerExplanationDialog.value = false
        viewModel.openNotificationSettings(context)
      }
    )
  }

  if (showOverlayInfo) {
    OverlayAccessExplanationDialog(
      onDismiss = { viewModel.showOverlayExplanationDialog.value = false },
      onOpenOverlaySettings = { viewModel.openOverlaySettings(context) },
      onOpenAppDetailsSettings = { viewModel.openAppDetailsSettings(context) },
      onOpenOverlayListSettings = { viewModel.openOverlaySettingsList(context) }
    )
  }

  if (showClearBlockersConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearBlockersConfirmDialog = false },
      title = { Text("Turn OFF All Blocked Apps?") },
      text = { Text("This will unblock all currently shielded apps ($blockedAppCount apps). You can re-select apps anytime.") },
      confirmButton = {
        Button(
          onClick = {
            showClearBlockersConfirmDialog = false
            viewModel.turnOffAllBlockedApps()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Turn OFF / Unblock All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearBlockersConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      title = { Text("Reset All Settings to OFF / Default?") },
      text = { Text("This will turn OFF strict mode, turn OFF all restrictions, and reset timer durations to defaults (25m / 5m).") },
      confirmButton = {
        Button(
          onClick = {
            showResetConfirmDialog = false
            viewModel.resetSettingsToDefaults()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Reset Settings")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onNavigateHome,
        modifier = Modifier.testTag("settings_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Home",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
      Column {
        Text(
          text = "Notification Shield & Safeguards",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "Distraction controls, in-app PIN, and privacy transparency",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // SECTION 0: MASTER POWER & OFF CONTROLS (Quick Setting OFF)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("master_power_card"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (isMasterShieldEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
      ),
      border = BorderStroke(
        1.5.dp,
        if (isMasterShieldEnabled) RadiantViolet.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error
      )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
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
              modifier = Modifier.size(46.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.PowerSettingsNew,
                  contentDescription = null,
                  tint = if (isMasterShieldEnabled) RadiantViolet else MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
            Column {
              Text(
                text = "Master App Blocker Power",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isMasterShieldEnabled) "Shield is ON (Active)" else "Shield is TURNED OFF",
                style = MaterialTheme.typography.bodySmall,
                color = if (isMasterShieldEnabled) SuccessEmerald else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Switch(
            checked = isMasterShieldEnabled,
            onCheckedChange = { checked ->
              viewModel.setMasterShieldEnabled(checked)
            },
            modifier = Modifier.testTag("toggle_master_shield_power"),
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.surface,
              checkedTrackColor = RadiantViolet,
              uncheckedThumbColor = MaterialTheme.colorScheme.surface,
              uncheckedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            )
          )
        }

        Text(
          text = if (isMasterShieldEnabled) {
            "Master shield is currently ON. Blocked apps ($blockedAppCount apps configured) will be restricted during active focus sessions."
          } else {
            "Master shield is currently TURNED OFF. All app restrictions are bypassed and no apps will be blocked."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // If active session is running, offer instant Turn OFF button
        if (isShieldServiceRunning) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Stop,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = "Active Focus Session Running",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onErrorContainer
                )
              }
              Text(
                text = "Tap below to immediately turn OFF the active focus session and stop the shield service.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
              Button(
                onClick = { viewModel.turnOffActiveFocus() },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_turn_off_active_focus"),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Turn OFF Active Focus Session", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Quick OFF Action Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = { viewModel.turnOffAllAlerts() },
            modifier = Modifier
              .weight(1f)
              .testTag("btn_turn_off_all_alerts"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Turn OFF Alerts", style = MaterialTheme.typography.labelSmall)
          }

          OutlinedButton(
            onClick = { viewModel.turnOnAllAlerts() },
            modifier = Modifier
              .weight(1f)
              .testTag("btn_turn_on_all_alerts"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Turn ON Alerts", style = MaterialTheme.typography.labelSmall)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (blockedAppCount > 0) {
            OutlinedButton(
              onClick = { showClearBlockersConfirmDialog = true },
              modifier = Modifier
                .weight(1f)
                .testTag("btn_turn_off_all_blocked_apps"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Unblock All ($blockedAppCount)", style = MaterialTheme.typography.labelSmall)
            }
          }

          OutlinedButton(
            onClick = { showResetConfirmDialog = true },
            modifier = Modifier
              .weight(1f)
              .testTag("btn_reset_settings_to_off"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset Settings", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    // 1c. Display Over Other Apps Permission & Troubleshooting Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("settings_overlay_permission_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (isOverlayGranted) SuccessEmerald.copy(alpha = 0.08f) else AmberStreak.copy(alpha = 0.08f)
      ),
      border = BorderStroke(
        1.dp,
        if (isOverlayGranted) SuccessEmerald.copy(alpha = 0.3f) else AmberStreak.copy(alpha = 0.4f)
      )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
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
              color = if (isOverlayGranted) SuccessEmerald.copy(alpha = 0.15f) else AmberStreak.copy(alpha = 0.15f),
              modifier = Modifier.size(40.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isOverlayGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                  contentDescription = null,
                  tint = if (isOverlayGranted) SuccessEmerald else AmberStreak,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            Column {
              Text(
                text = "Display Over Other Apps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isOverlayGranted) "Active • Full 60s Block Screen Supported" else "Required • For Full-Screen Interception",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOverlayGranted) SuccessEmerald else AmberStreak,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          if (!isOverlayGranted) {
            Button(
              onClick = { viewModel.openOverlaySettings(context) },
              colors = ButtonDefaults.buttonColors(containerColor = AmberStreak),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("btn_enable_overlay_settings")
            ) {
              Text("Enable", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Black)
            }
          }
        }

        if (!isOverlayGranted) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Switch turns OFF automatically in Android Settings?",
                style = MaterialTheme.typography.labelSmall,
                color = AmberStreak,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
              )
              TextButton(
                onClick = { viewModel.showOverlayExplanationDialog.value = true },
                modifier = Modifier.testTag("btn_fix_overlay_restricted_settings")
              ) {
                Text(
                  text = "Fix (3 Dots ⋮)",
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

    // Hero Overview Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("settings_hero_card"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(54.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = RadiantViolet,
                modifier = Modifier.size(28.dp)
              )
            }
          }
          Column {
            Text(
              text = "Notification Shield",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Privacy-Respecting Distraction Mitigation",
              style = MaterialTheme.typography.bodySmall,
              color = RadiantViolet,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Text(
          text = "Android protects your privacy by preventing ordinary apps from silently intercepting or muting other apps' notifications. Advanced Focus leverages native Android Do Not Disturb, timer completion alerts, and local session reminders for transparent, offline distraction management.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // SECTION 1: DO NOT DISTURB & SYSTEM NOTIFICATION INTEGRATION
    Text(
      text = "System Distraction Controls",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("dnd_integration_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = if (isDndGranted) SuccessEmerald else AmberStreak
            )
            Column {
              Text(
                text = "Android Do Not Disturb",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isDndGranted) "DND Access Granted in Android" else "DND Access Not Granted in System",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDndGranted) SuccessEmerald else AmberStreak,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        Text(
          text = "Do Not Disturb is Android's native system mechanism to silence calls, messages, and banners across all apps. Configure system DND access to enhance deep focus.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = { viewModel.openDndSettings(context) },
            modifier = Modifier
              .weight(1f)
              .testTag("open_dnd_settings_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Open DND Settings", style = MaterialTheme.typography.labelMedium)
          }

          OutlinedButton(
            onClick = { viewModel.showDndExplanationDialog.value = true },
            modifier = Modifier.testTag("dnd_learn_more_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Details", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    // Privacy & Notification Listener Disclosure Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("notification_privacy_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Notification Privacy & Offline Guarantee",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Why we don't inspect third-party notifications: 100% offline, zero tracking, zero message sniffing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedButton(
          onClick = { viewModel.showNotificationListenerExplanationDialog.value = true },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("privacy_disclosure_button")
        ) {
          Text("Read Policy", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    // SECTION 2: FOCUS NOTIFICATION PREFERENCES
    Text(
      text = "Focus Alert Preferences",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Focus Reminders
        SettingToggleRow(
          title = "Focus Session Reminders",
          subtitle = "Gentle ongoing alert while a focus session is in progress",
          icon = Icons.Default.Notifications,
          checked = settings.focusRemindersEnabled,
          onCheckedChange = { checked ->
            viewModel.updateNotificationPreferences(
              notifySessionComplete = settings.notifyOnSessionComplete,
              notifyShieldAlert = settings.notifyOnShieldAlert,
              soundAlertsEnabled = settings.soundAlertsEnabled,
              vibrationAlertsEnabled = settings.vibrationAlertsEnabled,
              focusRemindersEnabled = checked
            )
          },
          testTag = "toggle_focus_reminders"
        )

        // Session Complete Alert
        SettingToggleRow(
          title = "Session Completion Alerts",
          subtitle = "Notify with celebratory alert when focus or break completes",
          icon = Icons.Default.CheckCircle,
          checked = settings.notifyOnSessionComplete,
          onCheckedChange = { checked ->
            viewModel.updateNotificationPreferences(
              notifySessionComplete = checked,
              notifyShieldAlert = settings.notifyOnShieldAlert,
              soundAlertsEnabled = settings.soundAlertsEnabled,
              vibrationAlertsEnabled = settings.vibrationAlertsEnabled,
              focusRemindersEnabled = settings.focusRemindersEnabled
            )
          },
          testTag = "toggle_session_complete_alert"
        )

        // Focus Shield Distraction Alert
        SettingToggleRow(
          title = "Focus Shield Distraction Alerts",
          subtitle = "Show reminder notification when a blocked app is opened",
          icon = Icons.Default.Warning,
          checked = settings.notifyOnShieldAlert,
          onCheckedChange = { checked ->
            viewModel.updateNotificationPreferences(
              notifySessionComplete = settings.notifyOnSessionComplete,
              notifyShieldAlert = checked,
              soundAlertsEnabled = settings.soundAlertsEnabled,
              vibrationAlertsEnabled = settings.vibrationAlertsEnabled,
              focusRemindersEnabled = settings.focusRemindersEnabled
            )
          },
          testTag = "toggle_shield_distraction_alert"
        )

        // Sound Alerts
        SettingToggleRow(
          title = "Sound & Audio Cues",
          subtitle = "Play gentle chime on session complete or alert",
          icon = Icons.Default.VolumeUp,
          checked = settings.soundAlertsEnabled,
          onCheckedChange = { checked ->
            viewModel.updateNotificationPreferences(
              notifySessionComplete = settings.notifyOnSessionComplete,
              notifyShieldAlert = settings.notifyOnShieldAlert,
              soundAlertsEnabled = checked,
              vibrationAlertsEnabled = settings.vibrationAlertsEnabled,
              focusRemindersEnabled = settings.focusRemindersEnabled
            )
          },
          testTag = "toggle_sound_alerts"
        )

        // Vibration Alerts
        SettingToggleRow(
          title = "Haptic Vibration Alerts",
          subtitle = "Tactile vibration pulse on transition intervals",
          icon = Icons.Default.Vibration,
          checked = settings.vibrationAlertsEnabled,
          onCheckedChange = { checked ->
            viewModel.updateNotificationPreferences(
              notifySessionComplete = settings.notifyOnSessionComplete,
              notifyShieldAlert = settings.notifyOnShieldAlert,
              soundAlertsEnabled = settings.soundAlertsEnabled,
              vibrationAlertsEnabled = checked,
              focusRemindersEnabled = settings.focusRemindersEnabled
            )
          },
          testTag = "toggle_vibration_alerts"
        )

        OutlinedButton(
          onClick = { viewModel.openNotificationSettings(context) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("open_system_notification_settings_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Android App Notification Settings", style = MaterialTheme.typography.labelMedium)
        }
      }
    }

    // SECTION 3: IN-APP SETTINGS PROTECTION (PIN SAFEGUARDS)
    Text(
      text = "In-App Settings Protection",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("pin_protection_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = if (settings.isSettingsProtectionEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
              contentDescription = null,
              tint = if (settings.isSettingsProtectionEnabled) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
              Text(
                text = "In-App PIN Protection",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (settings.isSettingsProtectionEnabled) "Protected with 4–6 Digit PIN" else "Disabled (Direct Changes Allowed)",
                style = MaterialTheme.typography.bodySmall,
                color = if (settings.isSettingsProtectionEnabled) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Text(
          text = "Prevent impulsive modifications to timer settings, blocked app lists, or Strict Mode during focus sessions. Protected with salted SHA-256 local verification.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Text(
            text = "Platform Note: This safeguards in-app settings only and does not prevent Android system actions.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(10.dp)
          )
        }

        if (!settings.isSettingsProtectionEnabled) {
          Button(
            onClick = { viewModel.showPinSetupDialog.value = true },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("enable_pin_protection_button"),
            colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set Security PIN", style = MaterialTheme.typography.labelMedium)
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = {
                viewModel.executeWithProtection {
                  viewModel.showPinSetupDialog.value = true
                }
              },
              modifier = Modifier
                .weight(1f)
                .testTag("change_pin_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Change PIN", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
              onClick = {
                viewModel.executeWithProtection {
                  viewModel.disablePinProtection()
                }
              },
              modifier = Modifier
                .weight(1f)
                .testTag("disable_pin_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Disable PIN", style = MaterialTheme.typography.labelMedium)
            }
          }

          TextButton(
            onClick = { viewModel.showPinRecoveryDialog.value = true },
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .testTag("forgot_pin_button")
          ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Forgot PIN / Emergency Recovery Hint", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    // Strict Mode Setting Row
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("strict_mode_setting_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      SettingToggleRow(
        title = "Strict Focus Mode",
        subtitle = "Hides instant cancel button during sessions; requires long-press friction and delay",
        icon = Icons.Default.Security,
        checked = settings.isStrictModeEnabled,
        onCheckedChange = { checked ->
          viewModel.executeWithProtection {
            viewModel.setStrictMode(checked)
          }
        },
        testTag = "toggle_strict_mode_setting",
        modifier = Modifier.padding(16.dp)
      )
    }

    // SECTION 4: UNINSTALL SAFEGUARDS TRANSPARENCY
    Text(
      text = "Uninstall Safeguards Transparency",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("uninstall_safeguards_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = RadiantViolet
          )
          Text(
            text = "Android Platform Safeguard Architecture",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Text(
          text = "Standard Android operating system guarantees that users can uninstall any application at any time via Android Settings. Advanced Focus respects this design and never promises uninstall-proof protection. Instead, we offer in-app PIN protection and Strict Mode friction to support personal focus discipline.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = { viewModel.showUninstallInfoDialog.value = true },
            modifier = Modifier
              .weight(1f)
              .testTag("view_uninstall_safeguards_button"),
            colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Safeguards Overview", style = MaterialTheme.typography.labelMedium)
          }

          OutlinedButton(
            onClick = { viewModel.openAppDetailsSettings(context) },
            modifier = Modifier
              .weight(1f)
              .testTag("open_app_details_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("App Info Settings", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    // SECTION 5: DATA PRIVACY & LOCAL BACKUP
    Text(
      text = "Data Ownership & Offline Backup",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("backup_restore_card"),
      shape = RoundedCornerShape(16.dp),
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
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(Icons.Default.Backup, contentDescription = null, tint = RadiantViolet)
          Text(
            text = "Offline Backup & Restore",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Text(
          text = "Your data stays on your device. Export a validated JSON backup of your focus history, tasks, rewards, and settings using Android's Storage Access Framework, or restore from a previous backup file.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (backupStatusMessage != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = backupStatusMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = { viewModel.clearBackupStatusMessage() }) {
                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
              exportLauncher.launch("advanced_focus_backup_$timestamp.json")
            },
            modifier = Modifier
              .weight(1f)
              .testTag("create_backup_button"),
            colors = ButtonDefaults.buttonColors(containerColor = RadiantViolet),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Export Backup", style = MaterialTheme.typography.labelMedium)
          }

          OutlinedButton(
            onClick = {
              importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            modifier = Modifier
              .weight(1f)
              .testTag("restore_backup_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Restore Backup", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    // SECTION 6: ABOUT FOUNDER & DEVELOPER
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("about_founder_card"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      border = BorderStroke(1.dp, RadiantViolet.copy(alpha = 0.4f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = RadiantViolet
            )
            Column {
              Text(
                text = "Created by Amit Meena",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Founder & Lead Developer • Focus Shield Pro",
                style = MaterialTheme.typography.labelSmall,
                color = RadiantViolet,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          OutlinedButton(
            onClick = {
              com.example.timer.NotificationHelper(context).sendFounderWelcomeNotificationNow()
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("btn_send_founder_welcome")
          ) {
            Text("Welcome Note", style = MaterialTheme.typography.labelSmall)
          }
        }

        Text(
          text = "Engineered for deep work, zero distractions, and complete on-device privacy.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Return to Home
    Button(
      onClick = onNavigateHome,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("settings_return_home_button"),
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

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (checked) RadiantViolet else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp)
      )
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
      colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.surface,
        checkedTrackColor = RadiantViolet
      )
    )
  }
}
