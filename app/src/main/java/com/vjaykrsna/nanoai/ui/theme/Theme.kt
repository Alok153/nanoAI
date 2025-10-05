package com.vjaykrsna.nanoai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
  )

data class NanoAISpacing(
  val xs: Dp = 4.dp,
  val sm: Dp = 8.dp,
  val md: Dp = 16.dp,
  val lg: Dp = 24.dp,
  val xl: Dp = 32.dp,
  val xxl: Dp = 40.dp,
)

data class NanoAIElevation(
  val level0: Dp = 0.dp,
  val level1: Dp = 1.dp,
  val level2: Dp = 3.dp,
  val level3: Dp = 6.dp,
  val level4: Dp = 8.dp,
  val level5: Dp = 12.dp,
)

val LocalNanoAISpacing = staticCompositionLocalOf { NanoAISpacing() }
val LocalNanoAIElevation = staticCompositionLocalOf { NanoAIElevation() }

object NanoAIThemeDefaults {
  val spacing: NanoAISpacing
    @Composable @ReadOnlyComposable get() = LocalNanoAISpacing.current

  val elevation: NanoAIElevation
    @Composable @ReadOnlyComposable get() = LocalNanoAIElevation.current
}

@Composable
fun NanoAITheme(
  themePreference: ThemePreference = ThemePreference.SYSTEM,
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val systemDarkTheme = isSystemInDarkTheme()
  val darkTheme =
    when (themePreference) {
      ThemePreference.SYSTEM -> systemDarkTheme
      ThemePreference.DARK -> true
      ThemePreference.LIGHT -> false
    }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) {
          dynamicDarkColorScheme(context)
        } else {
          dynamicLightColorScheme(context)
        }
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    val activity = view.context as Activity
    val window = activity.window
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(window, false)
      window.statusBarColor = colorScheme.surface.toArgb()
      window.navigationBarColor = colorScheme.surface.toArgb()
      WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  CompositionLocalProvider(
    LocalNanoAISpacing provides NanoAISpacing(),
    LocalNanoAIElevation provides NanoAIElevation(),
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = NanoAITypography,
      content = content,
    )
  }
}
