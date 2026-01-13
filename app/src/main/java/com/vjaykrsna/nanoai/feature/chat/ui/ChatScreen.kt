package com.vjaykrsna.nanoai.feature.chat.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatError
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatViewModel
import com.vjaykrsna.nanoai.feature.chat.presentation.PersonaSwitcher
import com.vjaykrsna.nanoai.feature.chat.presentation.PersonaSwitcherViewModel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState
import com.vjaykrsna.nanoai.feature.uiux.presentation.NanoError
import java.util.UUID

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
  personaSwitcherViewModel: PersonaSwitcherViewModel = hiltViewModel(),
  onUpdateChatState: ((ChatState?) -> Unit)? = null,
  onNavigate: (ModeId) -> Unit = {},
) {
  val uiState by viewModel.state.collectAsStateWithLifecycle()
  val sheetState = rememberModalBottomSheetState()
  val snackbarHostState = remember { SnackbarHostState() }
  var activeError by remember { mutableStateOf<NanoError?>(null) }
  val latestOnUpdateChatState = rememberUpdatedState(onUpdateChatState)

  val personaUiState by personaSwitcherViewModel.state.collectAsStateWithLifecycle()
  val personaSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isPersonaSheetVisible by rememberSaveable { mutableStateOf(false) }

  val launchImagePicker = rememberChatImagePicker { bitmap -> viewModel.onImageSelected(bitmap) }

  ChatScreenEffects(
    events = viewModel.events,
    uiStateFlow = viewModel.state,
    snackbarHostState = snackbarHostState,
    latestOnUpdateChatState = latestOnUpdateChatState,
    onError = { activeError = it },
    toNanoError = { error, envelope -> error.toNanoError(envelope) },
  )

  PersonaSwitcherLogic(
    events = personaSwitcherViewModel.events,
    personaUiState = personaUiState,
    uiState = uiState,
    snackbarHostState = snackbarHostState,
    onThreadSelect = viewModel::selectThread,
    onCloseSheet = { isPersonaSheetVisible = false },
    onSetActiveThread = personaSwitcherViewModel::setActiveThread,
  )

  val actions =
    rememberChatScreenActions(
      viewModel,
      launchImagePicker,
      onNavigate,
      onDismissError = { activeError = null },
    )

  ChatScreenScaffold(
    uiState = uiState,
    personaUiState = personaUiState,
    snackbarHostState = snackbarHostState,
    activeError = activeError,
    actions = actions,
    sheetState = sheetState,
    onOpenPersonaSwitcher = { isPersonaSheetVisible = true },
    modifier = modifier,
  )

  PersonaSwitcherSheet(
    isVisible = isPersonaSheetVisible,
    onDismiss = { isPersonaSheetVisible = false },
    personaUiState = personaUiState,
    personaSheetState = personaSheetState,
    onSwitchPersona = personaSwitcherViewModel::switchPersona,
  )
}

@Composable
private fun PersonaSwitcherLogic(
  events:
    kotlinx.coroutines.flow.Flow<
      com.vjaykrsna.nanoai.feature.chat.presentation.PersonaSwitcherEvent
    >,
  personaUiState: com.vjaykrsna.nanoai.feature.chat.presentation.PersonaSwitcherUiState,
  uiState: com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatUiState,
  snackbarHostState: SnackbarHostState,
  onThreadSelect: (UUID) -> Unit,
  onCloseSheet: () -> Unit,
  onSetActiveThread: (UUID?, UUID?) -> Unit,
) {
  val latestOnSetActiveThread by rememberUpdatedState(onSetActiveThread)

  PersonaSwitcherEffects(
    events = events,
    snackbarHostState = snackbarHostState,
    onThreadSelect = onThreadSelect,
    onCloseSheet = onCloseSheet,
    personaLabelProvider = { personaId ->
      personaUiState.personas.firstOrNull { it.personaId == personaId }?.name
    },
  )

  LaunchedEffect(uiState.activeThreadId, uiState.activeThread?.personaId) {
    latestOnSetActiveThread(uiState.activeThreadId, uiState.activeThread?.personaId)
  }
}

@Composable
private fun rememberChatScreenActions(
  viewModel: ChatViewModel,
  launchImagePicker: () -> Unit,
  onNavigate: (ModeId) -> Unit,
  onDismissError: () -> Unit,
): ChatScreenActions {
  return remember(viewModel, launchImagePicker, onNavigate) {
    ChatScreenActions(
      onComposerTextChange = viewModel::onComposerTextChanged,
      onSendMessage = {
        viewModel.onSendMessage()
        onDismissError()
      },
      onImageSelect = launchImagePicker,
      onDismissError = {
        onDismissError()
        viewModel.clearPendingError()
      },
      onDismissModelPicker = viewModel::dismissModelPicker,
      onModelSelect = viewModel::selectModel,
      onManageModels = {
        viewModel.dismissModelPicker()
        onNavigate(ModeId.LIBRARY)
      },
      onDismissConnectivityBanner = viewModel::dismissConnectivityBanner,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaSwitcherSheet(
  isVisible: Boolean,
  onDismiss: () -> Unit,
  personaUiState: com.vjaykrsna.nanoai.feature.chat.presentation.PersonaSwitcherUiState,
  personaSheetState: androidx.compose.material3.SheetState,
  onSwitchPersona: (UUID, PersonaSwitchAction) -> Unit,
) {
  if (isVisible) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = personaSheetState) {
      PersonaSwitcher(
        uiState = personaUiState,
        onDismiss = onDismiss,
        onContinueThread = { personaId ->
          onSwitchPersona(personaId, PersonaSwitchAction.CONTINUE_THREAD)
        },
        onStartNewThread = { personaId ->
          onSwitchPersona(personaId, PersonaSwitchAction.START_NEW_THREAD)
        },
      )
    }
  }
}

private fun ChatError.toNanoError(envelope: NanoAIErrorEnvelope): NanoError {
  val description = envelope.userMessage
  return when (this) {
    is ChatError.InferenceFailed ->
      NanoError.Inline(title = "Couldn't complete inference", description = description)
    is ChatError.PersonaSwitchFailed ->
      NanoError.Inline(title = "Persona switch failed", description = description)
    is ChatError.PersonaSelectionFailed ->
      NanoError.Inline(title = "No persona selected", description = description)
    is ChatError.ThreadCreationFailed ->
      NanoError.Inline(title = "Couldn't start conversation", description = description)
    is ChatError.ThreadArchiveFailed ->
      NanoError.Inline(title = "Couldn't archive conversation", description = description)
    is ChatError.ThreadDeletionFailed ->
      NanoError.Inline(title = "Couldn't delete conversation", description = description)
    is ChatError.UnexpectedError ->
      NanoError.Inline(title = "Something went wrong", description = description)
  }
}
