package com.vjaykrsna.nanoai.feature.uiux.ui.sidebar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent

/**
 * Placeholder right sidebar panels container. Real implementation arrives in T027.
 */
@Composable
fun RightSidebarPanels(
  state: ShellUiState,
  onEvent: (ShellUiEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  @Suppress("UNUSED_PARAMETER")
  val ignored = onEvent
  Surface(modifier = modifier.testTag("right_sidebar_placeholder"), tonalElevation = 2.dp) {
    Column(modifier = Modifier.fillMaxSize(),) {
      Text(
        text = "Right sidebar pending",
        style = MaterialTheme.typography.titleSmall,
      )
      Text(
        text = "Active panel: ${state.layout.activeRightPanel ?: "None"}",
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}
