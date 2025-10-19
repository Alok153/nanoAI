package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySummary

@Composable
internal fun LibraryHeader(summary: ModelLibrarySummary) {
  LibrarySummaryRow(summary = summary, modifier = Modifier.semantics { heading() })
}

@Composable
internal fun LibrarySummaryRow(summary: ModelLibrarySummary, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    tonalElevation = 3.dp,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      SummaryInlineStat(
        label = "Installed",
        value = summary.installed.toString(),
      )
      SummaryInlineStat(
        label = "Storage",
        value = formatSize(summary.installedBytes),
      )
    }
  }
}

@Composable
private fun SummaryInlineStat(label: String, value: String) {
  Text(
    text =
      buildAnnotatedString {
        append(label)
        append(' ')
        pushStyle(
          SpanStyle(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        )
        append(value)
        pop()
      },
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}
