package com.vjaykrsna.nanoai.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatError
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatViewModel
import com.vjaykrsna.nanoai.feature.chat.ui.components.ModelPicker
import com.vjaykrsna.nanoai.feature.uiux.state.NanoError
import com.vjaykrsna.nanoai.feature.uiux.ui.components.composer.NanoComposerBar
import com.vjaykrsna.nanoai.feature.uiux.ui.components.feedback.NanoErrorHandler
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoRadii
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val MESSAGE_BUBBLE_WIDTH_FRACTION = 0.85f

/**
 * Main chat interface screen that displays conversation history and provides message input.
 *
 * Features:
 * - Real-time message display with user/assistant differentiation
 * - Persistent composer text across configuration changes
 * - Scroll position preservation
 * - Loading states and error handling
 * - Accessibility support with proper content descriptions
 *
 * @param modifier Modifier to apply to the screen
 * @param viewModel ChatViewModel for managing chat state and operations
 * @param onUpdateChatState Callback to notify parent about chat state changes (optional)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  modifier: Modifier = Modifier,
  viewModel: ChatViewModel = hiltViewModel(),
  onUpdateChatState: ((com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState?) -> Unit)? = null,
  onNavigate: (com.vjaykrsna.nanoai.feature.uiux.state.ModeId) -> Unit,
) {
  val messages by viewModel.messages.collectAsState()
  val currentThread by viewModel.currentThread.collectAsState()
  val availablePersonas by viewModel.availablePersonas.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val showModelPicker by viewModel.showModelPicker.collectAsState()
  val models by viewModel.models.collectAsState()
  val sheetState = rememberModalBottomSheetState()

  val snackbarHostState = remember { SnackbarHostState() }
  var composerText by rememberSaveable { mutableStateOf("") }
  var activeError by remember { mutableStateOf<NanoError?>(null) }

  LaunchedEffect(availablePersonas, currentThread) {
    onUpdateChatState?.invoke(
      com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState(
        availablePersonas = availablePersonas,
        currentPersonaId = currentThread?.personaId,
      )
    )
  }

  LaunchedEffect(Unit) {
    viewModel.errorEvents.collectLatest { error -> activeError = error.toNanoError() }
  }

  LaunchedEffect(snackbarHostState) {
    viewModel.events.collectLatest { event ->
      if (event is com.vjaykrsna.nanoai.feature.chat.presentation.ChatViewEvent.ModelSelected) {
        snackbarHostState.showSnackbar("Switched to ${event.modelName}")
      }
    }
  }

  Box(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription = "Chat screen with message history and input"
      }
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize().padding(horizontal = NanoSpacing.lg, vertical = NanoSpacing.md),
      verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
    ) {
      MessagesList(
        messages = messages,
        isLoading = isLoading,
        modifier = Modifier.weight(1f).fillMaxWidth(),
      )

      NanoErrorHandler(
        error = activeError,
        snackbarHostState = snackbarHostState,
        modifier = Modifier.fillMaxWidth(),
        onDismiss = { activeError = null },
      )

      NanoComposerBar(
        value = composerText,
        onValueChange = { composerText = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = "Type a message…",
        enabled = !isLoading && currentThread != null,
        onSend = {
          val trimmed = composerText.trim()
          if (trimmed.isNotEmpty()) {
            val personaId = currentThread?.personaId
            if (personaId != null) {
              viewModel.sendMessage(trimmed, personaId)
              composerText = ""
              activeError = null
            } else {
              activeError =
                NanoError.Inline(
                  title = "No persona selected",
                  description = "Choose a persona before sending messages.",
                )
            }
          }
        },
        sendEnabled = composerText.isNotBlank() && currentThread != null && !isLoading,
        isSending = isLoading,
      )
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(horizontal = 16.dp, vertical = 24.dp)
          .semantics { contentDescription = "Chat notifications and messages" },
    )

    if (showModelPicker) {
      ModalBottomSheet(
        onDismissRequest = { viewModel.dismissModelPicker() },
        sheetState = sheetState,
      ) {
        ModelPicker(
          models = models,
          selectedModelId = currentThread?.activeModelId,
          onModelSelect = { viewModel.selectModel(it) },
          onManageModelsClick = {
            viewModel.dismissModelPicker()
            onNavigate(com.vjaykrsna.nanoai.feature.uiux.state.ModeId.LIBRARY)
          },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun MessagesList(
  messages: List<Message>,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
) {
  val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  LazyColumn(
    state = listState,
    contentPadding = PaddingValues(NanoSpacing.md),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
    reverseLayout = false,
    modifier =
      modifier.semantics {
        liveRegion = LiveRegionMode.Polite
        contentDescription = "Message history list"
      },
  ) {
    items(items = messages, key = { it.messageId.toString() }, contentType = { it.role }) { message
      ->
      MessageBubble(message = message)
    }

    if (isLoading) {
      item {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(
            modifier =
              Modifier.size(24.dp).semantics { contentDescription = "AI is generating a response" }
          )
        }
      }
    }
  }
}

@Composable
private fun MessageBubble(message: Message, modifier: Modifier = Modifier) {
  val isUser = message.role == com.vjaykrsna.nanoai.core.model.Role.USER
  val backgroundColor =
    if (isUser) {
      MaterialTheme.colorScheme.primaryContainer
    } else {
      MaterialTheme.colorScheme.secondaryContainer
    }

  val alignment = if (isUser) Alignment.End else Alignment.Start

  Column(horizontalAlignment = alignment, modifier = modifier.fillMaxWidth()) {
    Card(
      colors = CardDefaults.cardColors(containerColor = backgroundColor),
      shape = RoundedCornerShape(NanoRadii.medium),
      modifier =
        Modifier.fillMaxWidth(MESSAGE_BUBBLE_WIDTH_FRACTION).semantics {
          val roleDescription = if (isUser) "Your" else "Assistant's"
          contentDescription = "$roleDescription message: ${message.text ?: ""}"
        },
    ) {
      Column(modifier = Modifier.padding(NanoSpacing.md)) {
        message.text?.let { text -> Text(text = text, style = MaterialTheme.typography.bodyLarge) }

        Spacer(modifier = Modifier.height(NanoSpacing.xs))

        val timestamp =
          message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).let {
            "${it.hour}:${it.minute.toString().padStart(2, '0')}"
          }

        Text(
          text = timestamp,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.semantics { contentDescription = "Message sent at $timestamp" },
        )
      }
    }
  }
}

private fun ChatError.toNanoError(): NanoError {
  return when (this) {
    is ChatError.InferenceFailed ->
      NanoError.Inline(title = "Couldn't complete inference", description = message)
    is ChatError.PersonaSwitchFailed ->
      NanoError.Inline(title = "Persona switch failed", description = message)
    is ChatError.ThreadCreationFailed ->
      NanoError.Inline(title = "Couldn't start conversation", description = message)
    is ChatError.ThreadArchiveFailed ->
      NanoError.Inline(title = "Couldn't archive conversation", description = message)
    is ChatError.ThreadDeletionFailed ->
      NanoError.Inline(title = "Couldn't delete conversation", description = message)
    is ChatError.UnexpectedError ->
      NanoError.Inline(title = "Something went wrong", description = message)
  }
}
