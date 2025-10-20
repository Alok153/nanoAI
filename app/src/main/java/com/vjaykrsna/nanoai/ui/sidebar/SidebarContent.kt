package com.vjaykrsna.nanoai.ui.sidebar

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.feature.uiux.ui.SidebarDrawer
import java.util.UUID
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** UI state for the navigation drawer sidebar. */
data class SidebarUiState(
  val threads: List<ChatThread>,
  val searchQuery: String,
  val showArchived: Boolean,
  val inferenceMode: InferenceMode,
  val pinnedTools: List<String>,
  val activeRoute: String? = null,
)

/** Aggregated callbacks for sidebar interactions to keep composables lightweight. */
data class SidebarInteractions(
  val onSearchQueryChange: (String) -> Unit = {},
  val onToggleArchive: () -> Unit = {},
  val onInferenceModeChange: (InferenceMode) -> Unit = {},
  val onThreadSelected: (ChatThread) -> Unit = {},
  val onArchiveThread: (UUID) -> Unit = {},
  val onDeleteThread: (UUID) -> Unit = {},
  val onNewThread: () -> Unit = {},
  val onNavigateHome: () -> Unit = {},
  val onNavigateSettings: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarContent(
  state: SidebarUiState,
  interactions: SidebarInteractions,
  modifier: Modifier = Modifier,
) {
  // String resources
  val sidebarContentDescription = stringResource(R.string.sidebar_content_description)
  val pinnedToolsContentDesc = stringResource(R.string.sidebar_pinned_tools_content_description)

  Column(
    modifier =
      modifier.fillMaxHeight().testTag("sidebar_drawer_container").semantics {
        contentDescription = sidebarContentDescription
      }
  ) {
    SidebarDrawer(
      pinnedTools = state.pinnedTools,
      activeRoute = state.activeRoute,
      onNavigateSettings = interactions.onNavigateSettings,
      onNavigateHome = interactions.onNavigateHome,
      modifier = Modifier.fillMaxWidth().semantics { contentDescription = pinnedToolsContentDesc },
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
      Spacer(modifier = Modifier.height(12.dp))
      SidebarHeader(onNewThread = interactions.onNewThread)
      Spacer(modifier = Modifier.height(16.dp))
      SidebarSearchField(
        query = state.searchQuery,
        onQueryChange = interactions.onSearchQueryChange,
      )
      Spacer(modifier = Modifier.height(12.dp))
      SidebarArchiveToggle(
        showArchived = state.showArchived,
        onToggleArchive = interactions.onToggleArchive,
      )
      Spacer(modifier = Modifier.height(12.dp))
      InferencePreferenceToggleRow(
        inferenceMode = state.inferenceMode,
        onInferenceModeChange = interactions.onInferenceModeChange,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    SidebarThreadList(
      threads = state.threads,
      interactions = interactions,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun SidebarHeader(onNewThread: () -> Unit) {
  // String resources
  val conversationsTxt = stringResource(R.string.sidebar_header_conversations)
  val createNewDesc = stringResource(R.string.sidebar_header_create_new)

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = conversationsTxt,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.semantics { heading() },
    )
    IconButton(
      onClick = onNewThread,
      modifier = Modifier.semantics { contentDescription = createNewDesc },
    ) {
      Icon(Icons.Default.Add, contentDescription = null)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarSearchField(query: String, onQueryChange: (String) -> Unit) {
  // String resources
  val placeholderTxt = stringResource(R.string.sidebar_search_placeholder)
  val searchContentDesc = stringResource(R.string.sidebar_search_content_description)

  TextField(
    value = query,
    onValueChange = onQueryChange,
    placeholder = { Text(placeholderTxt) },
    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
    modifier = Modifier.fillMaxWidth().semantics { contentDescription = searchContentDesc },
  )
}

@Composable
private fun SidebarArchiveToggle(showArchived: Boolean, onToggleArchive: () -> Unit) {
  // String resources
  val activeText = stringResource(R.string.sidebar_archive_toggle_active)
  val archivedText = stringResource(R.string.sidebar_archive_toggle_archived)
  val showActiveText = stringResource(R.string.sidebar_archive_toggle_show_active)
  val showArchivedText = stringResource(R.string.sidebar_archive_toggle_show_archived)
  val toggleContentDescription = stringResource(R.string.sidebar_archive_toggle_content_description)

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = if (showArchived) archivedText else activeText,
      style = MaterialTheme.typography.titleMedium,
    )
    AssistChip(
      onClick = onToggleArchive,
      label = { Text(if (showArchived) showActiveText else showArchivedText) },
      leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
      modifier = Modifier.semantics { contentDescription = toggleContentDescription },
    )
  }
}

@Composable
private fun SidebarThreadList(
  threads: List<ChatThread>,
  interactions: SidebarInteractions,
  modifier: Modifier = Modifier,
) {
  // String resources
  val threadListContentDesc = stringResource(R.string.sidebar_thread_list_content_description)

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
    modifier = modifier.semantics { contentDescription = threadListContentDesc },
  ) {
    items(items = threads, key = { it.threadId.toString() }, contentType = { "thread_item" }) {
      thread ->
      ThreadItem(
        thread = thread,
        onClick = { interactions.onThreadSelected(thread) },
        onArchive = { interactions.onArchiveThread(thread.threadId) },
        onDelete = { interactions.onDeleteThread(thread.threadId) },
      )
    }
  }
}

@VisibleForTesting
@Composable
fun InferencePreferenceToggleRow(
  inferenceMode: InferenceMode,
  onInferenceModeChange: (InferenceMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  // String resources
  val inferenceContentDesc =
    stringResource(R.string.sidebar_inference_preference_content_description)
  val localStateDesc = stringResource(R.string.sidebar_inference_preference_local_state)
  val cloudStateDesc = stringResource(R.string.sidebar_inference_preference_cloud_state)
  val inferenceTitle = stringResource(R.string.sidebar_inference_preference_title)
  val localDesc = stringResource(R.string.sidebar_inference_preference_local_description)
  val cloudDesc = stringResource(R.string.sidebar_inference_preference_cloud_description)
  val toggleDesc = stringResource(R.string.sidebar_inference_preference_toggle_content_description)

  Row(
    modifier =
      modifier.semantics {
        contentDescription = inferenceContentDesc
        stateDescription =
          if (inferenceMode == InferenceMode.LOCAL_FIRST) {
            localStateDesc
          } else {
            cloudStateDesc
          }
      },
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = inferenceTitle, style = MaterialTheme.typography.titleMedium)
      Text(
        text =
          if (inferenceMode == InferenceMode.LOCAL_FIRST) {
            localDesc
          } else {
            cloudDesc
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = inferenceMode == InferenceMode.LOCAL_FIRST,
      onCheckedChange = { checked ->
        val mode = if (checked) InferenceMode.LOCAL_FIRST else InferenceMode.CLOUD_FIRST
        onInferenceModeChange(mode)
      },
      modifier =
        Modifier.semantics {
          role = androidx.compose.ui.semantics.Role.Switch
          contentDescription = toggleDesc
        },
    )
  }
}

@Composable
private fun ThreadItem(
  thread: ChatThread,
  onClick: () -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // String resources
  val threadUntitledText = stringResource(R.string.sidebar_thread_item_untitled)
  val threadContentDescFormat = stringResource(R.string.sidebar_thread_item_content_description)
  val archiveDesc = stringResource(R.string.sidebar_thread_item_archive_content_description)
  val deleteDesc = stringResource(R.string.sidebar_thread_item_delete_content_description)
  val archiveIconDesc = stringResource(R.string.sidebar_thread_item_archive_icon)
  val deleteIconDesc = stringResource(R.string.sidebar_thread_item_delete_icon)

  Card(
    modifier =
      modifier.fillMaxWidth().clickable(onClick = onClick).semantics {
        val localDateTime = thread.updatedAt.toLocalDateTime(TimeZone.currentSystemDefault())
        val dateStr = "${localDateTime.monthNumber}/${localDateTime.dayOfMonth}"
        contentDescription =
          threadContentDescFormat.format(thread.title ?: threadUntitledText, dateStr)
      },
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = thread.title ?: threadUntitledText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          val timestamp =
            thread.updatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).let {
              "${it.monthNumber}/${it.dayOfMonth}"
            }
          Text(
            text = timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = onArchive,
            modifier = Modifier.semantics { contentDescription = archiveDesc },
          ) {
            Icon(Icons.Default.Delete, contentDescription = archiveIconDesc)
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.semantics { contentDescription = deleteDesc },
          ) {
            Icon(Icons.Default.Delete, contentDescription = deleteIconDesc)
          }
        }
      }
    }
  }
}
