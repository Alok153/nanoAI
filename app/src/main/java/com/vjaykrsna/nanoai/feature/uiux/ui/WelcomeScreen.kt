package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.presentation.WelcomeUiState
import com.vjaykrsna.nanoai.ui.components.OfflineBanner
import com.vjaykrsna.nanoai.ui.components.OnboardingTooltip

@Composable
fun WelcomeScreen(
  state: WelcomeUiState,
  onGetStartedClick: () -> Unit,
  onExplore: () -> Unit,
  onSkip: () -> Unit,
  onTooltipHelp: () -> Unit,
  onTooltipDismiss: () -> Unit,
  onTooltipDontShow: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(modifier = modifier.fillMaxSize()) {
    val scrollState = rememberScrollState()
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(scrollState)
          .padding(horizontal = 24.dp, vertical = 32.dp)
          .semantics { contentDescription = "Welcome screen" },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      OfflineBanner(
        isOffline = state.offline,
        queuedActions = 0,
        onRetry = onGetStartedClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      )
      Text(
        text = "Welcome${state.displayName?.let { ", $it" } ?: ""} to nanoAI",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag("welcome_hero_title").semantics { heading() },
      )
      Text(
        text = "A focused interface for trustworthy AI workflows.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Column(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Welcome actions" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = onGetStartedClick,
          modifier = Modifier.fillMaxWidth().testTag("welcome_cta_get_started"),
        ) {
          Text("Get started", textAlign = TextAlign.Center)
        }
        OutlinedButton(
          onClick = onExplore,
          modifier = Modifier.fillMaxWidth().testTag("welcome_cta_explore"),
        ) {
          Text("Explore features", textAlign = TextAlign.Center)
        }
        TextButton(
          onClick = onSkip,
          enabled = state.skipEnabled,
          modifier = Modifier.testTag("welcome_skip"),
        ) {
          Text("Skip for now")
        }
      }
      if (state.showOnboarding) {
        OnboardingTooltip(
          message = "Tip: You can customize theme and layout anytime.",
          onDismiss = onTooltipDismiss,
          onDontShowAgain = onTooltipDontShow,
          onHelp = onTooltipHelp,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}
