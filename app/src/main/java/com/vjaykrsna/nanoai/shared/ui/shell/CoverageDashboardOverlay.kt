package com.vjaykrsna.nanoai.shared.ui.shell

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vjaykrsna.nanoai.core.coverage.presentation.CoverageDashboardViewModel
import com.vjaykrsna.nanoai.core.coverage.ui.CoverageDashboardScreen

private const val COVERAGE_CARD_WIDTH_FRACTION = 0.9f
private const val BACKDROP_ALPHA = 0.5f

@Composable
internal fun CoverageDashboardOverlay(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: CoverageDashboardViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = BACKDROP_ALPHA))
        .clickable(onClick = onDismiss),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier =
        Modifier.fillMaxWidth(COVERAGE_CARD_WIDTH_FRACTION).clickable(enabled = false) {
          // Block click-through to backdrop
        }
    ) {
      CoverageDashboardScreen(
        state = uiState,
        onRefresh = viewModel::refresh,
        onShareRequest = {
          val shareText = buildString {
            appendLine("Coverage Report - Build ${uiState.buildId}")
            appendLine("Generated: ${uiState.generatedAtIso}")
            appendLine()
            uiState.layers.forEach { layer ->
              appendLine(
                "${layer.layer.displayName}: ${layer.metric.roundedCoverage}% (Target: ${layer.metric.roundedThreshold}%)"
              )
            }
          }
          val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
              putExtra(Intent.EXTRA_TEXT, shareText)
              type = "text/plain"
            }
          val shareIntent = Intent.createChooser(sendIntent, null)
          context.startActivity(shareIntent)
        },
      )
    }
  }
}
