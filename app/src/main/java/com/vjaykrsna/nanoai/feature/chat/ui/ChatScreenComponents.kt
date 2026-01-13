package com.vjaykrsna.nanoai.feature.chat.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.library.Model
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.feature.chat.model.LocalInferenceMissingReason
import com.vjaykrsna.nanoai.feature.chat.model.LocalInferenceUiState
import com.vjaykrsna.nanoai.feature.chat.model.LocalInferenceUiStatus
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatError
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatUiEvent
import com.vjaykrsna.nanoai.feature.chat.presentation.PersonaSwitcherEvent
import com.vjaykrsna.nanoai.feature.chat.presentation.PersonaSwitcherUiState
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatUiState
import com.vjaykrsna.nanoai.feature.chat.ui.components.ModelPicker
import com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState
import com.vjaykrsna.nanoai.feature.uiux.presentation.NanoError
import com.vjaykrsna.nanoai.feature.uiux.ui.components.ConnectivityBanner
import com.vjaykrsna.nanoai.feature.uiux.ui.components.composer.NanoComposerBar
import com.vjaykrsna.nanoai.feature.uiux.ui.components.feedback.NanoErrorHandler
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoRadii
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val MESSAGE_BUBBLE_WIDTH_FRACTION = 0.85f

@Composable
internal fun ChatScreenEffects(
  events: Flow<ChatUiEvent>,
  uiStateFlow: Flow<ChatUiState>,
  snackbarHostState: SnackbarHostState,
  latestOnUpdateChatState: State<((ChatState?) -> Unit)?>,
  onError: (NanoError?) -> Unit,
  toNanoError: (ChatError, NanoAIErrorEnvelope) -> NanoError,
) {
  val latestOnError by rememberUpdatedState(onError)
  val latestToNanoError by rememberUpdatedState(toNanoError)

  LaunchedEffect(Unit) {
    launch {
      events.collectLatest { event ->
        when (event) {
          is ChatUiEvent.ErrorRaised -> {
            latestOnError(latestToNanoError(event.error, event.envelope))
            snackbarHostState.showSnackbar(event.envelope.userMessage)
          }
          is ChatUiEvent.ModelSelected ->
            snackbarHostState.showSnackbar("Switched to ${event.modelName}")
        }
      }
    }

    latestOnUpdateChatState.value?.let { update ->
      launch {
        uiStateFlow.collectLatest { state ->
          update(
            ChatState(
              availablePersonas = state.personas,
              currentPersonaId = state.activeThread?.personaId,
            )
          )
        }
      }
    }
  }
}

@Composable
internal fun PersonaSwitcherEffects(
  events: Flow<PersonaSwitcherEvent>,
  snackbarHostState: SnackbarHostState,
  onThreadSelect: (UUID) -> Unit,
  onCloseSheet: () -> Unit,
  personaLabelProvider: (UUID) -> String?,
) {
  val latestOnThreadSelect by rememberUpdatedState(onThreadSelect)
  val latestOnCloseSheet by rememberUpdatedState(onCloseSheet)
  val latestPersonaLabelProvider by rememberUpdatedState(personaLabelProvider)

  LaunchedEffect(Unit) {
    events.collectLatest { event ->
      when (event) {
        is PersonaSwitcherEvent.SwitchCompleted -> {
          latestOnThreadSelect(event.targetThreadId)
          latestOnCloseSheet()
          val personaName = latestPersonaLabelProvider(event.personaId) ?: "persona"
          snackbarHostState.showSnackbar("Switched to $personaName")
        }
        is PersonaSwitcherEvent.ErrorRaised -> snackbarHostState.showSnackbar(event.message)
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ChatScreenScaffold(
  uiState: ChatUiState,
  personaUiState: PersonaSwitcherUiState,
  snackbarHostState: SnackbarHostState,
  activeError: NanoError?,
  actions: ChatScreenActions,
  sheetState: SheetState,
  onOpenPersonaSwitcher: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription = "Chat screen with message history and input"
      }
  ) {
    ChatMessageContent(
      uiState = uiState,
      personaUiState = personaUiState,
      snackbarHostState = snackbarHostState,
      activeError = activeError,
      actions = actions,
      onOpenPersonaSwitcher = onOpenPersonaSwitcher,
    )

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(horizontal = 16.dp, vertical = 24.dp)
          .semantics { contentDescription = "Chat notifications and messages" },
    )

    if (uiState.isModelPickerVisible) {
      ModalBottomSheet(onDismissRequest = actions.onDismissModelPicker, sheetState = sheetState) {
        ModelPicker(
          models = uiState.installedModels,
          selectedModelId = uiState.activeThread?.activeModelId,
          onModelSelect = actions.onModelSelect,
          onManageModelsClick = actions.onManageModels,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
internal fun ChatMessageContent(
  uiState: ChatUiState,
  personaUiState: PersonaSwitcherUiState,
  snackbarHostState: SnackbarHostState,
  activeError: NanoError?,
  actions: ChatScreenActions,
  onOpenPersonaSwitcher: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize().padding(horizontal = NanoSpacing.lg, vertical = NanoSpacing.md),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
  ) {
    PersonaSwitcherSummary(
      personaUiState = personaUiState,
      activePersonaName = uiState.activePersonaSummary?.displayName,
      onOpenPersonaSwitcher = onOpenPersonaSwitcher,
      modifier = Modifier.fillMaxWidth(),
    )

    uiState.connectivityBanner?.let { banner ->
      ConnectivityBanner(
        state = banner,
        onCtaClick = actions.onManageModels,
        onDismiss = actions.onDismissConnectivityBanner,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    LocalInferenceIndicator(uiState = uiState.localInferenceUi, modifier = Modifier.fillMaxWidth())

    MessagesList(
      messages = uiState.messages,
      isLoading = uiState.isSendingMessage,
      modifier = Modifier.weight(1f).fillMaxWidth(),
    )

    NanoErrorHandler(
      error = activeError,
      snackbarHostState = snackbarHostState,
      modifier = Modifier.fillMaxWidth(),
      onDismiss = actions.onDismissError,
    )

    uiState.attachments.image?.let { attachment ->
      Image(
        bitmap = attachment.bitmap.asImageBitmap(),
        contentDescription = "Selected image",
        modifier = Modifier.size(128.dp),
      )
    }

    ChatComposerBar(uiState = uiState, actions = actions)
  }
}

@Composable
internal fun PersonaSwitcherSummary(
  personaUiState: PersonaSwitcherUiState,
  activePersonaName: String?,
  onOpenPersonaSwitcher: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier.semantics {
        contentDescription =
          activePersonaName?.let { "Active persona $it" } ?: "No persona selected"
      },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(NanoSpacing.md),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = "Persona", style = MaterialTheme.typography.labelSmall)
        Text(
          text = activePersonaName ?: "No persona selected",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(top = NanoSpacing.xs),
        )
        Text(
          text = "${personaUiState.personas.size} available",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = NanoSpacing.xs),
        )
      }

      Button(onClick = onOpenPersonaSwitcher) { Text(text = "Switch persona") }
    }
  }
}

@Composable
internal fun ChatComposerBar(uiState: ChatUiState, actions: ChatScreenActions) {
  NanoComposerBar(
    value = uiState.composerText,
    onValueChange = actions.onComposerTextChange,
    modifier = Modifier.fillMaxWidth(),
    placeholder = "Type a message…",
    enabled = !uiState.isSendingMessage && uiState.activeThread != null,
    onSend = actions.onSendMessage,
    sendEnabled =
      uiState.composerText.isNotBlank() &&
        uiState.activeThread != null &&
        !uiState.isSendingMessage,
    isSending = uiState.isSendingMessage,
    onImageSelect = { actions.onImageSelect() },
    onAudioRecord = {}, // Audio recording requires AudioSessionCoordinator integration
  )
}

@Composable
internal fun MessagesList(
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
internal fun MessageBubble(message: Message, modifier: Modifier = Modifier) {
  val isUser = message.role == MessageRole.USER
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

@Composable
internal fun LocalInferenceIndicator(
  uiState: LocalInferenceUiState,
  modifier: Modifier = Modifier,
) {
  when (val status = uiState.status) {
    LocalInferenceUiStatus.Idle -> Unit
    is LocalInferenceUiStatus.OfflineReady -> {
      val modelName = uiState.modelName ?: stringResource(R.string.nano_shell_select_model)
      val text = stringResource(R.string.chat_offline_ready, modelName)
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = NanoSpacing.xs).semantics { contentDescription = text },
      )
    }
    is LocalInferenceUiStatus.OfflineMissing -> {
      val message =
        when (status.reason) {
          LocalInferenceMissingReason.NO_LOCAL_MODEL ->
            stringResource(R.string.chat_offline_missing_no_model)
          LocalInferenceMissingReason.MODEL_NOT_READY ->
            stringResource(R.string.chat_offline_missing_not_ready)
        }
      Text(
        text = message,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
        modifier =
          modifier.padding(bottom = NanoSpacing.xs).semantics { contentDescription = message },
      )
    }
  }
}

internal data class ChatScreenActions(
  val onComposerTextChange: (String) -> Unit,
  val onSendMessage: () -> Unit,
  val onImageSelect: () -> Unit,
  val onDismissError: () -> Unit,
  val onDismissModelPicker: () -> Unit,
  val onModelSelect: (Model) -> Unit,
  val onManageModels: () -> Unit,
  val onDismissConnectivityBanner: () -> Unit,
)
