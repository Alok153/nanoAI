package com.vjaykrsna.nanoai.core.domain.model.uiux

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CommandPaletteStateTest {

  private fun createTestAction(id: String) =
    CommandAction(id = id, title = "Action $id", category = CommandCategory.MODES)

  @Test
  fun `hasQuery returns true for non-blank queries`() {
    val state = CommandPaletteState(query = "test")
    assertThat(state.hasQuery).isTrue()
  }

  @Test
  fun `hasQuery returns false for blank queries`() {
    val state = CommandPaletteState(query = "  ")
    assertThat(state.hasQuery).isFalse()
  }

  @Test
  fun `hasQuery returns false for empty queries`() {
    val state = CommandPaletteState(query = "")
    assertThat(state.hasQuery).isFalse()
  }

  @Test
  fun `selectedCommand returns null when results are empty`() {
    val state = CommandPaletteState(results = emptyList(), selectedIndex = 0)
    assertThat(state.selectedCommand).isNull()
  }

  @Test
  fun `selectedCommand returns null when selectedIndex is negative`() {
    val state = CommandPaletteState(results = listOf(createTestAction("1")), selectedIndex = -1)
    assertThat(state.selectedCommand).isNull()
  }

  @Test
  fun `selectedCommand returns correct action for valid index`() {
    val actions = listOf(createTestAction("1"), createTestAction("2"))
    val state = CommandPaletteState(results = actions, selectedIndex = 1)
    assertThat(state.selectedCommand).isEqualTo(actions[1])
  }

  @Test
  fun `selectedCommand returns null when index is out of bounds`() {
    val state = CommandPaletteState(results = listOf(createTestAction("1")), selectedIndex = 5)
    assertThat(state.selectedCommand).isNull()
  }

  @Test
  fun `moveSelection returns reset state when results are empty`() {
    val state = CommandPaletteState(results = emptyList(), selectedIndex = 2)
    val moved = state.moveSelection(1)
    assertThat(moved.selectedIndex).isEqualTo(-1)
  }

  @Test
  fun `moveSelection wraps forward correctly`() {
    val actions = listOf(createTestAction("1"), createTestAction("2"), createTestAction("3"))
    val state = CommandPaletteState(results = actions, selectedIndex = 2)
    val moved = state.moveSelection(1)
    assertThat(moved.selectedIndex).isEqualTo(0)
  }

  @Test
  fun `moveSelection wraps backward correctly`() {
    val actions = listOf(createTestAction("1"), createTestAction("2"), createTestAction("3"))
    val state = CommandPaletteState(results = actions, selectedIndex = 0)
    val moved = state.moveSelection(-1)
    assertThat(moved.selectedIndex).isEqualTo(2)
  }

  @Test
  fun `moveSelection moves forward within bounds`() {
    val actions = listOf(createTestAction("1"), createTestAction("2"), createTestAction("3"))
    val state = CommandPaletteState(results = actions, selectedIndex = 0)
    val moved = state.moveSelection(1)
    assertThat(moved.selectedIndex).isEqualTo(1)
  }

  @Test
  fun `moveSelection uses zero when selectedIndex is invalid`() {
    val actions = listOf(createTestAction("1"), createTestAction("2"))
    val state = CommandPaletteState(results = actions, selectedIndex = -1)
    val moved = state.moveSelection(1)
    assertThat(moved.selectedIndex).isEqualTo(1)
  }

  @Test
  fun `clearSelection resets selectedIndex to negative one`() {
    val state = CommandPaletteState(selectedIndex = 5)
    val cleared = state.clearSelection()
    assertThat(cleared.selectedIndex).isEqualTo(-1)
  }

  @Test
  fun `cleared resets query and results but preserves surfaceTarget`() {
    val state =
      CommandPaletteState(
        query = "test",
        results = listOf(createTestAction("1")),
        surfaceTarget = CommandCategory.SETTINGS,
      )
    val cleared = state.cleared()
    assertThat(cleared.query).isEmpty()
    assertThat(cleared.results).isEmpty()
    assertThat(cleared.surfaceTarget).isEqualTo(CommandCategory.SETTINGS)
  }

  @Test
  fun `withResults deduplicates actions by id`() {
    val duplicateActions = listOf(createTestAction("1"), createTestAction("1"))
    val state = CommandPaletteState()
    val updated = state.withResults("query", duplicateActions)
    assertThat(updated.results).hasSize(1)
  }

  @Test
  fun `withResults resets selectedIndex to zero when results are not empty`() {
    val state = CommandPaletteState(selectedIndex = -1)
    val updated = state.withResults("query", listOf(createTestAction("1")))
    assertThat(updated.selectedIndex).isEqualTo(0)
  }

  @Test
  fun `withResults sets selectedIndex to negative one when results are empty`() {
    val state = CommandPaletteState(selectedIndex = 2)
    val updated = state.withResults("query", emptyList())
    assertThat(updated.selectedIndex).isEqualTo(-1)
  }

  @Test
  fun `withResults preserves selectedIndex if still within bounds`() {
    val actions = listOf(createTestAction("1"), createTestAction("2"), createTestAction("3"))
    val state = CommandPaletteState(selectedIndex = 1)
    val updated = state.withResults("query", actions)
    assertThat(updated.selectedIndex).isEqualTo(1)
  }

  @Test
  fun `Empty companion object returns default state`() {
    val empty = CommandPaletteState.Empty
    assertThat(empty.query).isEmpty()
    assertThat(empty.results).isEmpty()
    assertThat(empty.selectedIndex).isEqualTo(-1)
  }
}
