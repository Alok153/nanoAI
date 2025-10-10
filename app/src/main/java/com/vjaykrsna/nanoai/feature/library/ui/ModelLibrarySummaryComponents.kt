package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibrarySummary

@Composable
internal fun LibraryHeader(summary: ModelLibrarySummary) {
  LibrarySummaryRow(summary = summary)
}

@Composable
internal fun LibrarySummaryRow(summary: ModelLibrarySummary) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SummaryCard(
      title = "Installed",
      value = summary.installed.toString(),
      modifier = Modifier.weight(1f)
    )
    SummaryCard(
      title = "Storage",
      value = formatSize(summary.installedBytes),
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun SummaryCard(
  title: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
