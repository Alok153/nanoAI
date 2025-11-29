package com.vjaykrsna.nanoai.shared.ui

import androidx.compose.runtime.Immutable

/**
 * Centralized test tags and content descriptions for UI components.
 *
 * Using shared constants instead of literal strings provides:
 * - Stable references that survive refactoring
 * - Single source of truth for both production code and tests
 * - Easy searchability for tag usages
 * - Type safety through IDE auto-completion
 *
 * Usage in Composables:
 * ```kotlin
 * Modifier.testTag(TestTags.Shell.COMMAND_PALETTE)
 * ```
 *
 * Usage in Tests:
 * ```kotlin
 * composeTestRule.onNodeWithTag(TestTags.Shell.COMMAND_PALETTE)
 * ```
 */
@Immutable
object TestTags {

  /** Tags for the main shell/scaffold components. */
  object Shell {
    const val COMMAND_PALETTE = "command_palette"
    const val COMMAND_PALETTE_SEARCH = "command_palette_search"
    const val COMMAND_PALETTE_LIST = "command_palette_list"
    const val COMMAND_PALETTE_ITEM = "command_palette_item"
    const val SIDEBAR_DRAWER = "sidebar_drawer"
    const val SIDEBAR_DEEPLINK_SLOT = "sidebar_deeplink_slot"
    const val PROGRESS_CENTER_PANEL = "progress_center_panel"
    const val PROGRESS_LIST = "progress_list"
    const val CONNECTIVITY_BANNER = "connectivity_banner"

    /** Creates a dynamic tag for sidebar pinned tool items. */
    fun sidebarPinnedTool(toolId: String) = "sidebar_pinned_$toolId"

    /** Creates a dynamic tag for progress list items. */
    fun progressListItem(index: Int) = "progress_list_item_$index"

    /** Creates a dynamic tag for progress retry buttons. */
    fun progressRetryButton(jobId: String) = "progress_retry_button_$jobId"

    const val PROGRESS_CLEAR_BUTTON = "progress_clear_button"
  }

  /** Tags for home hub screen components. */
  object Home {
    const val HUB = "home_hub"
    const val MODE_GRID = "home_mode_grid"
    const val MODE_CARD = "mode_card"
    const val TOOLS_TOGGLE = "home_tools_toggle"
    const val TOOLS_PANEL_EXPANDED = "home_tools_panel_expanded"
    const val TOOLS_PANEL_COLLAPSED = "home_tools_panel_collapsed"
    const val QUICK_ACTIONS_ROW = "quick_actions_row"
    const val RECENT_ACTION_CONFIRMATION = "home_recent_action_confirmation"
    const val RECENT_ACTIVITY_LIST = "recent_activity_list"
    const val RECENT_ACTIVITY_ITEM = "recent_activity_item"

    /** Creates a dynamic tag for quick action buttons. */
    fun quickAction(actionId: String) = "home_quick_action_$actionId"
  }

  /** Tags for chat feature components. */
  object Chat {
    const val MODEL_SELECTOR_PANEL = "chat_model_selector_panel"
    const val MESSAGE_LIST = "chat_message_list"
    const val MESSAGE_INPUT = "chat_message_input"
    const val SEND_BUTTON = "chat_send_button"

    /** Creates a dynamic tag for chat message items. */
    fun messageItem(index: Int) = "chat_message_$index"
  }

  /** Tags for settings feature components. */
  object Settings {
    const val SHORTCUTS_PANEL = "settings_shortcuts_panel"
    const val SCREEN = "settings_screen"
  }

  /** Tags for coverage dashboard components. */
  object Coverage {
    const val DASHBOARD_ERROR_BANNER = "coverage-dashboard-error-banner"
    const val DASHBOARD_LOADING = "coverage-dashboard-loading"
    const val DASHBOARD_CONTENT = "coverage-dashboard-content"
  }

  /** Tags for app-level components. */
  object App {
    const val HYDRATION_STATE = "app_hydration_state"
  }
}

/**
 * Centralized content descriptions for accessibility.
 *
 * These descriptions should be used with semantics blocks:
 * ```kotlin
 * Modifier.semantics { contentDescription = ContentDescriptions.Shell.TOGGLE_TOOLS }
 * ```
 */
@Immutable
object ContentDescriptions {

  /** Content descriptions for shell components. */
  object Shell {
    const val TOGGLE_TOOLS = "Toggle tools panel"
    const val CLOSE_PALETTE = "Close command palette"
    const val OPEN_SIDEBAR = "Open navigation sidebar"
    const val CLOSE_SIDEBAR = "Close navigation sidebar"
    const val RETRY_JOB = "Retry failed job"
    const val CLEAR_COMPLETED = "Clear completed job"
  }

  /** Content descriptions for home components. */
  object Home {
    const val MODE_GRID = "Available modes"
    const val RECENT_ACTIVITY = "Recent activity"
    const val QUICK_ACTIONS = "Quick actions"
  }

  /** Content descriptions for chat components. */
  object Chat {
    const val SEND_MESSAGE = "Send message"
    const val MESSAGE_INPUT = "Message input field"
  }
}
