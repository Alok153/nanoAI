package com.vjaykrsna.nanoai.shared.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val COVERAGE_CARD_WIDTH_FRACTION = 0.8f

@Composable
internal fun CoverageDashboardOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  // TODO: Implement proper coverage dashboard overlay with ViewModel
  // For now, show a placeholder
  Box(
    modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
    contentAlignment = Alignment.Center,
  ) {
    Card(modifier = Modifier.fillMaxWidth(COVERAGE_CARD_WIDTH_FRACTION), onClick = onDismiss) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(text = "Coverage Dashboard", style = MaterialTheme.typography.headlineSmall)
        Text(
          text = "This is a placeholder. Tap to dismiss.",
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}
