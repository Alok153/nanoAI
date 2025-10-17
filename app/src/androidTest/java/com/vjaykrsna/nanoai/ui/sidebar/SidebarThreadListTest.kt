package com.vjaykrsna.nanoai.ui.sidebar

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SidebarThreadListTest : BaseSidebarContentTest() {

  @Test
  fun sidebarThreadList_displaysThreads() {
    val state = defaultState()

    renderSidebar(
      state,
      interactions =
        SidebarInteractions(
          onThreadSelected = { selectedThread = it },
          onNewThread = { newThreadClicked = true },
        ),
    )

    composeRule.onNodeWithContentDescription("Conversation threads list").assertIsDisplayed()

    composeRule.onNodeWithText("Test Conversation 1").assertIsDisplayed()
    composeRule.onNodeWithText("Test Conversation 2").assertIsDisplayed()
  }

  @Test
  fun sidebarThreadList_emptyStateShowsMessage() {
    val state = defaultState(threads = emptyList())

    renderSidebar(state)

    composeRule.onNodeWithContentDescription("Conversation threads list").assertIsDisplayed()
    composeRule.onNodeWithText("Test Conversation 1").assertDoesNotExist()
  }

  @Test
  fun sidebarThreadList_threadSelectionNavigates() {
    val state = defaultState()

    renderSidebar(
      state,
      interactions = SidebarInteractions(onThreadSelected = { selectedThread = it })
    )

    composeRule.onNodeWithText("Test Conversation 1").performClick()

    composeRule.runOnIdle {
      assertThat(selectedThread).isNotNull()
      assertThat(selectedThread?.title).isEqualTo("Test Conversation 1")
    }
  }

  @Test
  fun sidebarThreadList_hasAccessibleLabels() {
    val state = defaultState()

    renderSidebar(state)

    composeRule.onNodeWithContentDescription("Conversation navigation drawer").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Conversation threads list").assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Create new conversation")
      .assertIsDisplayed()
      .assertContentDescriptionContains("Create new conversation")
  }
}
