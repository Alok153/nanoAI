package com.vjaykrsna.nanoai.ui.sidebar

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import com.vjaykrsna.nanoai.ui.theme.NanoAITheme
import java.util.UUID
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Rule

abstract class BaseSidebarContentTest {

  @get:Rule(order = 0) val environmentRule = TestEnvironmentRule()
  @get:Rule(order = 1) val composeRule: ComposeContentTestRule = createComposeRule()

  protected lateinit var testThreads: List<ChatThread>
  protected var newThreadClicked: Boolean = false
  protected var selectedThread: ChatThread? = null
  protected var archivedThreadId: UUID? = null
  protected var deletedThreadId: UUID? = null
  protected var searchQuery: String = ""

  @Before
  fun setUpBase() {
    newThreadClicked = false
    selectedThread = null
    archivedThreadId = null
    deletedThreadId = null
    searchQuery = ""

    testThreads =
      listOf(
        ChatThread(
          threadId = UUID.randomUUID(),
          title = "Test Conversation 1",
          personaId = UUID.randomUUID(),
          activeModelId = "gemini-2.0-flash-lite",
          createdAt = Clock.System.now(),
          updatedAt = Clock.System.now(),
          isArchived = false,
        ),
        ChatThread(
          threadId = UUID.randomUUID(),
          title = "Test Conversation 2",
          personaId = UUID.randomUUID(),
          activeModelId = "gemini-2.0-flash-lite",
          createdAt = Clock.System.now(),
          updatedAt = Clock.System.now(),
          isArchived = false,
        ),
      )
  }

  protected fun renderSidebar(
    state: SidebarUiState,
    interactions: SidebarInteractions = SidebarInteractions(),
  ) {
    composeRule.setContent {
      NanoAITheme { SidebarContent(state = state, interactions = interactions) }
    }
    composeRule.waitForIdle()
  }

  protected fun defaultState(
    threads: List<ChatThread> = testThreads,
    searchQuery: String = "",
    showArchived: Boolean = false,
    inferenceMode: InferenceMode = InferenceMode.CLOUD_FIRST,
    pinnedTools: List<String> = emptyList(),
    activeRoute: String? = null,
  ): SidebarUiState =
    SidebarUiState(
      threads = threads,
      searchQuery = searchQuery,
      showArchived = showArchived,
      inferenceMode = inferenceMode,
      pinnedTools = pinnedTools,
      activeRoute = activeRoute,
    )
}
