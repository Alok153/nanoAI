package com.vjaykrsna.nanoai.feature.uiux.ui.commandpalette

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteDismissReason

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
  val keyHandler =
    rememberCommandPaletteKeyHandler(
      filteredResults = filteredResults,
      selectedIndex = selectedIndex,
      onSelectedIndexChange = { selectedIndex = it },
      onDismissRequest = onDismissRequest,
      onCommandSelect = onCommandSelect,
    )

  CommandPaletteLayout(
    interactionSource = interactionSource,
    focusRequester = focusRequester,
    query = query,
    onQueryChange = { query = it },
    results = filteredResults,
    selectedIndex = selectedIndex,
    onSelect = { selectedIndex = it },
    onDismissRequest = onDismissRequest,
    onCommandSelect = onCommandSelect,
    modifier = modifier,
    keyHandler = keyHandler,
  )
}

@Composable
private fun rememberCommandPaletteKeyHandler(
  filteredResults: List<CommandAction>,
  selectedIndex: Int,
  onSelectedIndexChange: (Int) -> Unit,
  onDismissRequest: (PaletteDismissReason) -> Unit,
  onCommandSelect: (CommandAction) -> Unit,
): (KeyEvent) -> Boolean =
  remember(filteredResults, selectedIndex) {
    { event ->
      if (event.type != KeyEventType.KeyDown) return@remember false
      when {
        handleNavigationKey(event.key, filteredResults, selectedIndex, onSelectedIndexChange) ->
          true
        event.key == Key.Enter -> handleEnterKey(filteredResults, selectedIndex, onCommandSelect)
        event.key == Key.Escape -> {
          onDismissRequest(PaletteDismissReason.BACK_PRESSED)
          true
        }
        event.key == Key.K && event.isCtrlPressed -> {
          onDismissRequest(PaletteDismissReason.BACK_PRESSED)
          true
        }
        else -> false
      }
    }
  }

private fun handleNavigationKey(
  key: Key,
  results: List<CommandAction>,
  selectedIndex: Int,
  onSelectedIndexChange: (Int) -> Unit,
): Boolean {
  val targetIndex =
    when (key) {
      Key.DirectionDown -> results.resolveNextIndex(selectedIndex)
      Key.DirectionUp -> results.resolvePreviousIndex(selectedIndex)
      else -> null
    } ?: -1

  return if (targetIndex >= 0) {
    onSelectedIndexChange(targetIndex)
    true
  } else {
    false
  }
}

private fun handleEnterKey(
  results: List<CommandAction>,
  selectedIndex: Int,
  onCommandSelect: (CommandAction) -> Unit,
): Boolean {
  val action = results.getOrNull(selectedIndex)?.takeIf(CommandAction::enabled) ?: return false
  onCommandSelect(action)
  return true
}
