package com.vjaykrsna.nanoai.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatError
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatViewModel
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val MESSAGE_BUBBLE_WIDTH_FRACTION = 0.85f

@Composable
fun ChatScreen(
  modifier: Modifier = Modifier,
  viewModel: ChatViewModel = hiltViewModel(),
  onUpdateChatState: ((com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState?) -> Unit)? = null,
) {
  val messages by viewModel.messages.collectAsState()
  val currentThread by viewModel.currentThread.collectAsState()
  val availablePersonas by viewModel.availablePersonas.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(availablePersonas, currentThread) {
    onUpdateChatState?.invoke(
      com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState(
        availablePersonas = availablePersonas,
        currentPersonaId = currentThread?.personaId,
      )
    )
  }

  LaunchedEffect(Unit) {
    viewModel.errorEvents.collectLatest { error ->
      val message =
        when (error) {
          is ChatError.InferenceFailed -> "Inference failed: ${error.message}"
          is ChatError.PersonaSwitchFailed -> "Persona switch failed: ${error.message}"
          is ChatError.ThreadCreationFailed -> "Thread creation failed: ${error.message}"
          is ChatError.ThreadArchiveFailed -> "Archive failed: ${error.message}"
          is ChatError.ThreadDeletionFailed -> "Delete failed: ${error.message}"
          is ChatError.UnexpectedError -> "Error: ${error.message}"
        }
      snackbarHostState.showSnackbar(message)
    }
  }

  Box(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription = "Chat screen with message history and input"
      },
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MessagesList(
        messages = messages,
        isLoading = isLoading,
        modifier = Modifier.weight(1f).fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(8.dp))

      MessageInputArea(
        onSendMessage = { text ->
          currentThread?.personaId?.let { personaId -> viewModel.sendMessage(text, personaId) }
        },
        enabled = !isLoading && currentThread != null,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 24.dp),
    )
  }
}

@Composable
private fun MessagesList(
  messages: List<Message>,
  isLoading: Boolean,
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  LazyColumn(
    state = listState,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    reverseLayout = false,
    modifier =
      modifier.semantics {
        liveRegion = LiveRegionMode.Polite
        contentDescription = "Message history list"
      },
  ) {
    items(
      items = messages,
      key = { it.messageId.toString() },
      contentType = { it.role },
    ) { message ->
      MessageBubble(message = message)
    }

    if (isLoading) {
      item {
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp).semantics { contentDescription = "Loading response" },
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

  Column(
    horizontalAlignment = alignment,
    modifier = modifier.fillMaxWidth(),
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = backgroundColor),
      shape = RoundedCornerShape(12.dp),
      modifier =
        Modifier.fillMaxWidth(MESSAGE_BUBBLE_WIDTH_FRACTION).semantics {
          val roleDescription = if (isUser) "Your" else "Assistant's"
          contentDescription = "$roleDescription message: ${message.text ?: ""}"
        },
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
      ) {
        message.text?.let { text ->
          Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        val timestamp =
          message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).let {
            "${it.hour}:${it.minute.toString().padStart(2, '0')}"
          }

        Text(
          text = timestamp,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun MessageInputArea(
  onSendMessage: (String) -> Unit,
  enabled: Boolean,
  modifier: Modifier = Modifier
) {
  var messageText by rememberSaveable { mutableStateOf("") }

  Row(
    modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    TextField(
      value = messageText,
      onValueChange = { messageText = it },
      placeholder = { Text("Type a message...") },
      enabled = enabled,
      modifier = Modifier.weight(1f).semantics { contentDescription = "Message input field" },
    )

    IconButton(
      onClick = {
        if (messageText.isNotBlank()) {
          onSendMessage(messageText)
          messageText = ""
        }
      },
      enabled = enabled && messageText.isNotBlank(),
      modifier = Modifier.semantics { contentDescription = "Send message button" },
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Send,
        contentDescription = "Send",
      )
    }
  }
}
