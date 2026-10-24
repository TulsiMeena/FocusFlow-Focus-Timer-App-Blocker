package com.example.apps.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apps.model.AppInfo
import com.example.apps.repository.InstalledAppsRepository
import com.example.apps.util.OverlayPermissionHelper
import com.example.apps.util.UsagePermissionHelper
import com.example.data.local.AppDatabase
import com.example.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InstalledAppsViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = InstalledAppsRepository(application)
  private val sessionRepository: SessionRepository
  private val smartPrefs = com.example.shield.SmartShieldPreferencesManager.getInstance(application)

  init {
    val database = AppDatabase.getInstance(application)
    sessionRepository = SessionRepository(
      database.sessionDao(),
      database.timerSettingsDao(),
      database.activeTimerDao(),
      database.blockedAppDao()
    )
  }

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _isUsageAccessGranted = MutableStateFlow(false)
  val isUsageAccessGranted: StateFlow<Boolean> = _isUsageAccessGranted.asStateFlow()

  private val _isOverlayPermissionGranted = MutableStateFlow(false)
  val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())

  val totalAppCount: StateFlow<Int> = _allApps.map { it.size }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val appDailyLimits: StateFlow<Map<String, Int>> = smartPrefs.appDailyLimits

  val totalScreenTimeTodayMinutes: StateFlow<Int> = _allApps.map { apps ->
    (apps.sumOf { it.usageDurationMillis } / 60_000L).toInt()
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val topUsedAppsToday: StateFlow<List<AppInfo>> = _allApps.map { apps ->
    apps.filter { it.usageDurationMillis >= 60_000L }
      .sortedByDescending { it.usageDurationMillis }
      .take(4)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _selectedCategory = MutableStateFlow(com.example.apps.model.AppCategory.ALL)
  val selectedCategory: StateFlow<com.example.apps.model.AppCategory> = _selectedCategory.asStateFlow()

  val filteredApps: StateFlow<List<AppInfo>> = combine(_allApps, _searchQuery, _selectedCategory) { apps, query, category ->
    val textFiltered = repository.filterApps(apps, query)
    if (category == com.example.apps.model.AppCategory.ALL) {
      textFiltered
    } else {
      textFiltered.filter { it.category == category }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val blockedPackageNames: StateFlow<Set<String>> = sessionRepository.blockedPackageNames
    .map { it.toSet() }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptySet()
    )

  val blockedAppsCount: StateFlow<Int> = blockedPackageNames
    .map { it.size }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0
    )

  val isMasterShieldEnabled: StateFlow<Boolean> = sessionRepository.timerSettings
    .map { it.isMasterShieldEnabled }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = true
    )

  fun setMasterShieldEnabled(enabled: Boolean) {
    viewModelScope.launch {
      sessionRepository.setMasterShieldEnabled(enabled)
      if (!enabled) {
        com.example.shield.FocusShieldService.stop(getApplication())
      }
    }
  }

  private val _showExplanationDialog = MutableStateFlow(false)
  val showExplanationDialog: StateFlow<Boolean> = _showExplanationDialog.asStateFlow()

  private val _showOverlayExplanationDialog = MutableStateFlow(false)
  val showOverlayExplanationDialog: StateFlow<Boolean> = _showOverlayExplanationDialog.asStateFlow()

  init {
    checkPermissionAndLoadApps(forceReload = true)
  }

  fun checkPermissionAndLoadApps(forceReload: Boolean = false) {
    val context = getApplication<Application>()
    val granted = UsagePermissionHelper.isUsageAccessGranted(context)
    val overlayGranted = OverlayPermissionHelper.isOverlayPermissionGranted(context)
    val permissionChanged = (granted != _isUsageAccessGranted.value) || (overlayGranted != _isOverlayPermissionGranted.value)
    _isUsageAccessGranted.value = granted
    _isOverlayPermissionGranted.value = overlayGranted

    if (_allApps.value.isEmpty() || forceReload || permissionChanged) {
      loadApps()
    }
  }

  fun loadApps() {
    _isLoading.value = true
    _errorMessage.value = null

    viewModelScope.launch {
      try {
        val apps = repository.getInstalledLaunchableApps()
        _allApps.value = apps
        _isLoading.value = false
        if (apps.isEmpty()) {
          _errorMessage.value = "No launchable applications detected on this device."
        }
      } catch (e: Exception) {
        _isLoading.value = false
        _errorMessage.value = "Failed to query installed apps: ${e.localizedMessage ?: "Unknown error"}"
      }
    }
  }

  fun toggleAppBlocked(packageName: String, appName: String) {
    viewModelScope.launch {
      val isBlocked = blockedPackageNames.value.contains(packageName)
      sessionRepository.setAppBlocked(packageName, appName, !isBlocked)
    }
  }

  fun setAppDailyLimit(packageName: String, appName: String, limitMinutes: Int) {
    smartPrefs.setAppDailyLimitMinutes(packageName, limitMinutes)
    if (limitMinutes > 0 && isMasterShieldEnabled.value) {
      val labels = _allApps.value.associate { it.packageName to it.appName } + (packageName to appName)
      com.example.shield.FocusShieldService.start(
        context = getApplication(),
        blockedPackages = blockedPackageNames.value.toList(),
        labelsMap = labels,
        isStrict = false
      )
    }
  }

  fun clearAllBlockedApps() {
    viewModelScope.launch {
      sessionRepository.clearAllBlockedApps()
    }
  }

  fun selectCategory(category: com.example.apps.model.AppCategory) {
    _selectedCategory.value = category
  }

  fun blockAppsByCategory(category: com.example.apps.model.AppCategory) {
    viewModelScope.launch {
      val appsToBlock = _allApps.value.filter { it.category == category }
      appsToBlock.forEach { app ->
        sessionRepository.setAppBlocked(app.packageName, app.appName, true)
      }
    }
  }

  fun blockAllDistractingApps() {
    viewModelScope.launch {
      val distractingCategories = setOf(
        com.example.apps.model.AppCategory.SOCIAL,
        com.example.apps.model.AppCategory.ENTERTAINMENT,
        com.example.apps.model.AppCategory.GAMES,
        com.example.apps.model.AppCategory.BROWSING_SHOPPING
      )
      val appsToBlock = _allApps.value.filter { it.category in distractingCategories }
      appsToBlock.forEach { app ->
        sessionRepository.setAppBlocked(app.packageName, app.appName, true)
      }
    }
  }

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
  }

  fun clearSearch() {
    _searchQuery.value = ""
  }

  fun showExplanationDialog() {
    _showExplanationDialog.value = true
  }

  fun dismissExplanationDialog() {
    _showExplanationDialog.value = false
  }

  fun openUsageSettings(context: Context): Boolean {
    _showExplanationDialog.value = false
    val success = UsagePermissionHelper.openUsageAccessSettings(context)
    if (!success) {
      _errorMessage.value = "Unable to open Android Usage Access settings automatically. Please open System Settings > Apps > Special app access > Usage access."
    }
    return success
  }

  fun showOverlayExplanationDialog() {
    _showOverlayExplanationDialog.value = true
  }

  fun dismissOverlayExplanationDialog() {
    _showOverlayExplanationDialog.value = false
  }

  fun openOverlaySettings(context: Context): Boolean {
    val success = OverlayPermissionHelper.openOverlaySettings(context)
    if (!success) {
      _errorMessage.value = "Unable to open Display Over Other Apps settings automatically. Please open System Settings > Apps > Special app access > Display over other apps."
    }
    return success
  }

  fun openOverlaySettingsList(context: Context): Boolean {
    val success = OverlayPermissionHelper.openOverlaySettingsList(context)
    if (!success) {
      _errorMessage.value = "Unable to open Display Over Other Apps settings list."
    }
    return success
  }

  fun openAppDetailsSettings(context: Context): Boolean {
    val success = OverlayPermissionHelper.openAppDetailsSettings(context)
    if (!success) {
      _errorMessage.value = "Unable to open App Info settings."
    }
    return success
  }
}
