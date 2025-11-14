package com.vjaykrsna.nanoai.feature.uiux.ui.commandpalette

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction

@Composable
internal fun CommandPaletteItem(action: CommandAction, selected: Boolean, onSelect: () -> Unit) {
  val colors = paletteItemColors(selected, action.enabled)
  val semanticsModifier = paletteItemSemantics(action.enabled)

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("command_palette_item")
        .then(semanticsModifier)
        .selectable(
          selected = selected,
          enabled = action.enabled,
          role = Role.Button,
          onClick = onSelect,
        ),
    shape = RoundedCornerShape(16.dp),
    tonalElevation = if (selected) 3.dp else 0.dp,
    color = colors.container,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CommandPaletteLabels(action = action, colors = colors)
      CommandPaletteShortcut(action = action)
    }
  }
}

@Composable
private fun RowScope.CommandPaletteLabels(action: CommandAction, colors: PaletteItemColors) {
  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = action.title,
      style = MaterialTheme.typography.titleSmall,
      color = colors.title,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    action.subtitle?.let { subtitle ->
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun CommandPaletteShortcut(action: CommandAction) {
  action.shortcut?.let { shortcut ->
    Text(
      text = shortcut,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private data class PaletteItemColors(val container: Color, val title: Color)

@Composable
private fun paletteItemColors(selected: Boolean, enabled: Boolean): PaletteItemColors {
  val containerColor =
    if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
  val titleColor =
    if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
  return PaletteItemColors(container = containerColor, title = titleColor)
}

private fun paletteItemSemantics(enabled: Boolean): Modifier =
  if (enabled) {
    Modifier
  } else {
    Modifier.semantics {
      contentDescription = "Unavailable offline"
      disabled()
    }
  }
