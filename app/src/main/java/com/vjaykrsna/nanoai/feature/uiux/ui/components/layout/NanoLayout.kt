package com.vjaykrsna.nanoai.feature.uiux.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoLayoutDefaults
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

@Composable
fun NanoScreen(
  modifier: Modifier = Modifier,
  header: (@Composable ColumnScope.() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val scrollState = rememberScrollState()
  Column(
    modifier =
      modifier.fillMaxSize().verticalScroll(scrollState).padding(NanoLayoutDefaults.ScreenPadding),
    verticalArrangement = Arrangement.spacedBy(NanoLayoutDefaults.ScreenVerticalSpacing),
  ) {
    header?.invoke(this)
    content()
  }
}

@Composable
fun NanoSection(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  action: (@Composable (() -> Unit))? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.semantics { heading() },
        verticalArrangement = Arrangement.spacedBy(NanoSpacing.xs),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
        )
        subtitle?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      action?.invoke()
    }
    Column(verticalArrangement = Arrangement.spacedBy(NanoLayoutDefaults.ItemSpacing)) { content() }
  }
}
