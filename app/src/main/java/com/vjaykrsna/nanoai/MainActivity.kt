package com.vjaykrsna.nanoai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.JankStats
import com.vjaykrsna.nanoai.feature.uiux.presentation.AppViewModel
import com.vjaykrsna.nanoai.ui.navigation.NavigationScaffold
import com.vjaykrsna.nanoai.ui.theme.NanoAITheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the nanoAI application.
 *
 * This activity hosts the entire Compose UI and sets up the navigation scaffold.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private var jankStats: JankStats? = null

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    jankStats =
      JankStats.createAndTrack(window) { frameData ->
        if (frameData.isJank) {
          val durationMs = frameData.frameDurationUiNanos / 1_000_000f
          Log.w(JANK_TAG, "Jank frame detected: duration=${"%.2f".format(durationMs)}ms")
        }
      }
    jankStats?.isTrackingEnabled = false
    setContent {
      val windowSizeClass = calculateWindowSizeClass(activity = this@MainActivity)
      val appViewModel: AppViewModel = hiltViewModel()
      val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()

      NanoAITheme(themePreference = appUiState.themePreference) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          if (appUiState.isHydrating) {
            AppHydrationState(isOffline = appUiState.offline)
          } else {
            NavigationScaffold(
              appState = appUiState,
              windowSizeClass = windowSizeClass,
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    jankStats?.isTrackingEnabled = true
  }

  override fun onPause() {
    jankStats?.isTrackingEnabled = false
    super.onPause()
  }

  override fun onDestroy() {
    jankStats?.isTrackingEnabled = false
    jankStats = null
    super.onDestroy()
  }

  private companion object {
    const val JANK_TAG = "NanoAI-Jank"
  }
}

@Composable
private fun AppHydrationState(isOffline: Boolean, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize().testTag("app_hydration_state"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      CircularProgressIndicator()
      Spacer(modifier = Modifier.height(16.dp))
      val message =
        if (isOffline) "Reconnecting to cached workspace…" else "Loading your workspace…"
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
