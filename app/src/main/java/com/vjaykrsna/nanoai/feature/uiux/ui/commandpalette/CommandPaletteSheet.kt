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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteDismissReason

@Composable
fun CommandPaletteSheet(
  state: CommandPaletteState,
  onDismissRequest: (PaletteDismissReason) -> Unit,
  onCommandSelect: (CommandAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  var query by remember(state.results, state.query) { mutableStateOf(state.query) }
  var selectedIndex by
    remember(state.results, state.selectedIndex) {
      mutableStateOf(
        state.selectedIndex.takeIf { state.results.isNotEmpty() && it in state.results.indices }
          ?: 0
      )
    }

  val filteredResults = remember(query, state.results) { filterCommands(state.results, query) }

  LaunchedEffect(filteredResults) {
    val currentIndex = selectedIndex
    val firstEnabled = filteredResults.firstEnabledIndex()
    selectedIndex =
      when {
        filteredResults.isEmpty() -> -1
        currentIndex in filteredResults.indices && filteredResults[currentIndex].enabled ->
          currentIndex
        firstEnabled != -1 -> firstEnabled
        currentIndex in filteredResults.indices -> currentIndex
        else -> -1
      }
  }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  val interactionSource = remember { MutableInteractionSource() }
  val keyHandler: (KeyEvent) -> Boolean =
    remember(filteredResults, selectedIndex) {
      { event ->
        if (event.type != KeyEventType.KeyDown) return@remember false
        when (event.key) {
          Key.DirectionDown -> {
            val nextIndex =
              when {
                filteredResults.isEmpty() -> null
                selectedIndex == -1 -> filteredResults.firstEnabledIndex()
                else ->
                  filteredResults.nextEnabledIndex(selectedIndex)
                    ?: filteredResults.firstEnabledIndex()
              }
            if (nextIndex != null && nextIndex != -1) {
              selectedIndex = nextIndex
            }
            true
          }
          Key.DirectionUp -> {
            val previousIndex =
              when {
                filteredResults.isEmpty() -> null
                selectedIndex == -1 -> filteredResults.lastEnabledIndex()
                else ->
                  filteredResults.previousEnabledIndex(selectedIndex)
                    ?: filteredResults.lastEnabledIndex()
              }
            if (previousIndex != null && previousIndex != -1) {
              selectedIndex = previousIndex
            }
            true
          }
          Key.Enter -> {
            filteredResults.getOrNull(selectedIndex)?.takeIf(CommandAction::enabled)?.let {
              onCommandSelect(it)
            }
            true
          }
          Key.Escape -> {
            onDismissRequest(PaletteDismissReason.BACK_PRESSED)
            true
          }
          Key.K -> {
            if (event.isCtrlPressed) {
              onDismissRequest(PaletteDismissReason.BACK_PRESSED)
              true
            } else {
              false
            }
          }
          else -> false
        }
      }
    }

  Box(modifier = modifier.fillMaxSize().testTag("command_palette")) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
          .clickable(interactionSource = interactionSource, indication = null) {
            onDismissRequest(PaletteDismissReason.HIDDEN)
          },
    )

    Surface(
      modifier =
        Modifier.align(Alignment.TopCenter)
          .padding(horizontal = 24.dp, vertical = 64.dp)
          .fillMaxWidth()
          .onPreviewKeyEvent(keyHandler),
      tonalElevation = 6.dp,
      shape = RoundedCornerShape(28.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier =
              Modifier.weight(1f).focusRequester(focusRequester).testTag("command_palette_search"),
            placeholder = { Text("Search commands…") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
              if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) {
                  Icon(Icons.Outlined.Close, contentDescription = "Clear query")
                }
              }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions =
              KeyboardActions(
                onSearch = {
                  filteredResults
                    .firstOrNull()
                    ?.takeIf(CommandAction::enabled)
                    ?.let(onCommandSelect)
                }
              ),
          )
        }

        HorizontalDivider()

        if (filteredResults.isEmpty()) {
          Text(
            text = "No commands match \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 32.dp).align(Alignment.CenterHorizontally),
          )
        } else {
          CommandPaletteResultList(
            results = filteredResults,
            selectedIndex = selectedIndex,
            onSelect = { index -> selectedIndex = index },
            onCommandSelect = onCommandSelect,
            keyHandler = keyHandler,
          )
        }
      }
    }
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
private fun CommandPaletteItem(
  action: CommandAction,
  selected: Boolean,
  onSelect: () -> Unit,
) {
  val indicatorColor =
    if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
  val titleColor =
    if (action.enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant
  val disabledModifier =
    if (!action.enabled) {
      Modifier.semantics {
        contentDescription = "Unavailable offline"
        disabled()
      }
    } else {
      Modifier
    }

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("command_palette_item")
        .then(disabledModifier)
        .selectable(
          selected = selected,
          enabled = action.enabled,
          role = Role.Button,
          onClick = onSelect,
        ),
    shape = RoundedCornerShape(16.dp),
    tonalElevation = if (selected) 3.dp else 0.dp,
    color = indicatorColor,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = action.title,
          style = MaterialTheme.typography.titleSmall,
          color = titleColor,
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
      action.shortcut?.let { shortcut ->
        Text(
          text = shortcut,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private const val PRIORITY_PREFIX_MATCH = 0
private const val PRIORITY_TITLE_CONTAINS = 1
private const val PRIORITY_SUBTITLE_CONTAINS = 2

private fun List<CommandAction>.firstEnabledIndex(): Int =
  indexOfFirst(CommandAction::enabled).takeIf { it >= 0 } ?: -1

private fun List<CommandAction>.lastEnabledIndex(): Int =
  indexOfLast(CommandAction::enabled).takeIf { it >= 0 } ?: -1

private fun List<CommandAction>.nextEnabledIndex(current: Int): Int? {
  if (isEmpty()) return null
  for (index in (current + 1).coerceAtLeast(0) until size) {
    if (this[index].enabled) return index
  }
  return null
}

private fun List<CommandAction>.previousEnabledIndex(current: Int): Int? {
  if (isEmpty()) return null
  for (index in current - 1 downTo 0) {
    if (this[index].enabled) return index
  }
  return null
}

private fun filterCommands(actions: List<CommandAction>, query: String): List<CommandAction> {
  if (query.isBlank()) return actions
  val needle = query.lowercase()
  return actions
    .mapNotNull { action ->
      val title = action.title.lowercase()
      val subtitle = action.subtitle?.lowercase()
      val priority =
        when {
          title.startsWith(needle) -> PRIORITY_PREFIX_MATCH
          title.contains(needle) -> PRIORITY_TITLE_CONTAINS
          subtitle?.contains(needle) == true -> PRIORITY_SUBTITLE_CONTAINS
          else -> null
        }
      priority?.let { it to action }
    }
    .sortedWith(compareBy({ it.first }, { it.second.title.length }))
    .map { it.second }
}
