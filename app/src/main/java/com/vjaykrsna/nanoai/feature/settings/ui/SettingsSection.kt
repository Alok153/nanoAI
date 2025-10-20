package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vjaykrsna.nanoai.feature.uiux.ui.components.layout.NanoSection

@Composable
internal fun SettingsSection(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  NanoSection(title = title, modifier = modifier.fillMaxWidth()) { content() }
}
