package com.vjaykrsna.nanoai.coverage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private const val OFFLINE_FALLBACK_ANNOUNCEMENT =
  "Offline coverage fallback active. Showing cached metrics until device farm recovers."

/** Immutable state consumed by [CoverageDashboardScreen]. */
data class CoverageDashboardUiState(
  val buildId: String,
  val generatedAtIso: String,
  val isRefreshing: Boolean,
  val layers: List<LayerCoverageState>,
  val risks: List<RiskChipState>,
  val trendDelta: Map<TestLayer, Double>,
  val errorMessage: String?,
)

/** Simple value object describing coverage metrics per layer. */
data class LayerCoverageState(
  val layer: TestLayer,
  val metric: CoverageMetric,
)

/** Describes risk chips surfaced on the dashboard. */
data class RiskChipState(
  val riskId: String,
  val title: String,
  val severity: String,
  val status: String,
)

@Composable
fun CoverageDashboardScreen(
  state: CoverageDashboardUiState,
  onRefresh: () -> Unit,
  onRiskSelected: (RiskChipState) -> Unit = { _ -> },
  onShareRequested: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    HeaderSection(state = state, onRefresh = onRefresh, onShareRequested = onShareRequested)

    if (state.errorMessage != null) {
      ErrorBanner(message = state.errorMessage)
    }

    if (state.risks.isNotEmpty()) {
      RiskSection(risks = state.risks, onRiskSelected = onRiskSelected)
    }

    LayerList(layers = state.layers, trendDelta = state.trendDelta)
  }
}

@Composable
private fun HeaderSection(
  state: CoverageDashboardUiState,
  onRefresh: () -> Unit,
  onShareRequested: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "Coverage dashboard",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = "Build ${state.buildId} • Generated ${formatTimestamp(state.generatedAtIso)}",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    RowActions(
      isRefreshing = state.isRefreshing,
      onRefresh = onRefresh,
      onShareRequested = onShareRequested
    )
  }
}

@Composable
private fun RowActions(
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  onShareRequested: () -> Unit,
) {
  androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Button(onClick = onRefresh, enabled = !isRefreshing) {
      if (isRefreshing) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(text = if (isRefreshing) "Refreshing" else "Refresh")
    }
    TextButton(onClick = onShareRequested) { Text("Share report") }
  }
}

@Composable
private fun ErrorBanner(message: String) {
  Surface(
    modifier =
      Modifier.fillMaxWidth().testTag("coverage-dashboard-error-banner").semantics {
        contentDescription = OFFLINE_FALLBACK_ANNOUNCEMENT
      },
    color = MaterialTheme.colorScheme.errorContainer,
  ) {
    Text(
      text = message,
      modifier = Modifier.padding(16.dp),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onErrorContainer,
    )
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RiskSection(risks: List<RiskChipState>, onRiskSelected: (RiskChipState) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "Risks",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold
    )
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      risks.forEach { risk ->
        AssistChip(
          onClick = { onRiskSelected(risk) },
          label = { Text(risk.title) },
          modifier =
            Modifier.semantics {
              contentDescription =
                "Risk ${risk.riskId}, severity ${risk.severity}, status ${risk.status}"
            },
        )
      }
    }
  }
}

@Composable
private fun LayerList(layers: List<LayerCoverageState>, trendDelta: Map<TestLayer, Double>) {
  LazyColumn(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(layers, key = { layer -> layer.layer.name }) { layerState ->
      LayerCard(state = layerState, delta = trendDelta[layerState.layer])
      HorizontalDivider()
    }
  }
}

@Composable
private fun LayerCard(state: LayerCoverageState, delta: Double?) {
  val layer = state.layer
  val metric = state.metric
  val tag = "coverage-layer-${layer.machineName.replaceFirstChar { it.titlecase(Locale.US) }}"
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = layer.displayName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = "${formatPercent(metric.coverage)} • Target ${formatPercent(metric.threshold)}",
        modifier = Modifier.testTag(tag),
        style = MaterialTheme.typography.bodyMedium,
      )
      if (delta != null) {
        val deltaLabel = if (delta >= 0) "+${formatDecimal(delta)}" else formatDecimal(delta)
        Text(
          text = "Week-over-week ${deltaLabel}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private fun formatTimestamp(raw: String): String {
  return try {
    val parsed = OffsetDateTime.parse(raw)
    parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US))
  } catch (error: DateTimeParseException) {
    raw
  }
}

private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value)

private fun formatDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)
