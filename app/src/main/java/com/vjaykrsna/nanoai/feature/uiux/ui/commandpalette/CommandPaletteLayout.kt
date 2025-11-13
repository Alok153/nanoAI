package com.vjaykrsna.nanoai.feature.uiux.ui.commandpalette

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteDismissReason

@Composable
internal fun CommandPaletteLayout(
  interactionSource: MutableInteractionSource,
  focusRequester: FocusRequester,
  query: String,
  onQueryChange: (String) -> Unit,
  results: List<CommandAction>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  onDismissRequest: (PaletteDismissReason) -> Unit,
  onCommandSelect: (CommandAction) -> Unit,
  modifier: Modifier = Modifier,
  keyHandler: (KeyEvent) -> Boolean,
) {
  Box(modifier = modifier.fillMaxSize().testTag("command_palette")) {
    CommandPaletteScrim(interactionSource = interactionSource, onDismissRequest = onDismissRequest)

    Surface(
      modifier =
        Modifier.align(Alignment.TopCenter)
          .padding(horizontal = 24.dp, vertical = 64.dp)
          .fillMaxWidth()
          .onPreviewKeyEvent(keyHandler),
      tonalElevation = 6.dp,
      shape = RoundedCornerShape(28.dp),
    ) {
      CommandPaletteContent(
        query = query,
        onQueryChange = onQueryChange,
        focusRequester = focusRequester,
        results = results,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        onCommandSelect = onCommandSelect,
        keyHandler = keyHandler,
      )
    }
  }
}

@Composable
private fun CommandPaletteScrim(
  interactionSource: MutableInteractionSource,
  onDismissRequest: (PaletteDismissReason) -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
        .clickable(interactionSource = interactionSource, indication = null) {
          onDismissRequest(PaletteDismissReason.HIDDEN)
        }
  )
}

@Composable
private fun CommandPaletteContent(
  query: String,
  onQueryChange: (String) -> Unit,
  focusRequester: FocusRequester,
  results: List<CommandAction>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  onCommandSelect: (CommandAction) -> Unit,
  keyHandler: (KeyEvent) -> Boolean,
) {
  Column(
    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    CommandPaletteSearchBar(
      query = query,
      onQueryChange = onQueryChange,
      focusRequester = focusRequester,
      results = results,
      onCommandSelect = onCommandSelect,
    )

    HorizontalDivider()

    CommandPaletteResultsSection(
      query = query,
      results = results,
      selectedIndex = selectedIndex,
      onSelect = onSelect,
      onCommandSelect = onCommandSelect,
      keyHandler = keyHandler,
    )
  }
}

@Composable
private fun CommandPaletteSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  focusRequester: FocusRequester,
  results: List<CommandAction>,
  onCommandSelect: (CommandAction) -> Unit,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier =
        Modifier.weight(1f).focusRequester(focusRequester).testTag("command_palette_search"),
      placeholder = { Text("Search commands…") },
      singleLine = true,
      leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(onClick = { onQueryChange("") }) {
            Icon(Icons.Outlined.Close, contentDescription = "Clear query")
          }
        }
      },
      keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
      keyboardActions =
        KeyboardActions(
          onSearch = { results.firstOrNull()?.takeIf(CommandAction::enabled)?.let(onCommandSelect) }
        ),
    )
  }
}

@Composable
private fun CommandPaletteResultsSection(
  query: String,
  results: List<CommandAction>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  onCommandSelect: (CommandAction) -> Unit,
  keyHandler: (KeyEvent) -> Boolean,
) {
  if (results.isEmpty()) {
    Text(
      text = "No commands match \"$query\"",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
      textAlign = TextAlign.Center,
    )
  } else {
    CommandPaletteResultList(
      results = results,
      selectedIndex = selectedIndex,
      onSelect = onSelect,
      onCommandSelect = onCommandSelect,
      keyHandler = keyHandler,
    )
  }
}

@Composable
private fun CommandPaletteResultList(
  results: List<CommandAction>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  onCommandSelect: (CommandAction) -> Unit,
  keyHandler: (KeyEvent) -> Boolean,
) {
  LazyColumn(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(max = 360.dp)
        .selectableGroup()
        .testTag("command_palette_list")
        .onPreviewKeyEvent(keyHandler),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    itemsIndexed(results, key = { _, action -> action.id }) { index, action ->
      val selected = index == selectedIndex
      CommandPaletteItem(
        action = action,
        selected = selected,
        onSelect = {
          onSelect(index)
          if (action.enabled) {
            onCommandSelect(action)
          }
        },
      )
    }
  }
}

@Composable
private fun CommandPaletteItem(action: CommandAction, selected: Boolean, onSelect: () -> Unit) {
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
      PaletteLabels()
      PaletteShortcut()
    }
  }

  @Composable
  fun PaletteLabels() {
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
  fun PaletteShortcut() {
    action.shortcut?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
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
