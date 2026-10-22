package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = RadiantViolet,
  onPrimary = DarkGraphiteBackground,
  primaryContainer = DarkGraphiteContainerHigh,
  onPrimaryContainer = RadiantViolet,
  secondary = ElectricBlue,
  onSecondary = DarkGraphiteBackground,
  secondaryContainer = DarkGraphiteContainerHigh,
  onSecondaryContainer = ElectricBlue,
  tertiary = CyanAccent,
  onTertiary = DarkGraphiteBackground,
  background = DarkGraphiteBackground,
  onBackground = DarkTextPrimary,
  surface = DarkGraphiteSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkGraphiteSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkGraphiteOutline,
  outlineVariant = DarkGraphiteOutlineVariant,
  surfaceContainer = DarkGraphiteSurfaceVariant,
  surfaceContainerHigh = DarkGraphiteContainerHigh,
  surfaceContainerHighest = DarkGraphiteContainerHighest
)

private val LightColorScheme = lightColorScheme(
  primary = DeepViolet,
  onPrimary = LightSurface,
  primaryContainer = LightContainerHigh,
  onPrimaryContainer = DeepViolet,
  secondary = ElectricBlue,
  onSecondary = LightSurface,
  secondaryContainer = LightContainerHigh,
  onSecondaryContainer = ElectricBlue,
  tertiary = CyanAccent,
  onTertiary = LightSurface,
  background = LightBackground,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightTextSecondary,
  outline = LightOutline,
  outlineVariant = LightOutlineVariant,
  surfaceContainer = LightSurfaceVariant,
  surfaceContainerHigh = LightContainerHigh,
  surfaceContainerHighest = LightContainerHighest
)

@Composable
fun AdvancedFocusTheme(
  // Dark theme is the default design foundation per requirements
  darkTheme: Boolean = true,
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
