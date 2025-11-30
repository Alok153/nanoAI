package com.vjaykrsna.nanoai.shared.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun CoverageDashboardOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  // No-op in release builds; coverage dashboard is only available to debug builds.
}
