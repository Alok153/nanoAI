package com.vjaykrsna.nanoai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingTooltip(
  message: String,
  onDismiss: () -> Unit,
  onDontShowAgain: () -> Unit,
  onHelp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier.fillMaxWidth().testTag("onboarding_tooltip_container").semantics {
        contentDescription = "Contextual help"
        liveRegion = LiveRegionMode.Polite
      },
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { heading() },
      )
      Spacer(modifier = Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("onboarding_tooltip_dismiss"),
        ) {
          Text("Dismiss")
        }
        TextButton(
          onClick = onDontShowAgain,
          modifier = Modifier.testTag("onboarding_tooltip_dont_show_again"),
        ) {
          Text("Don't show again")
        }
        TextButton(
          onClick = onHelp,
          modifier = Modifier.testTag("onboarding_tooltip_help_entry"),
        ) {
          Text("Help")
        }
      }
    }
  }
}
