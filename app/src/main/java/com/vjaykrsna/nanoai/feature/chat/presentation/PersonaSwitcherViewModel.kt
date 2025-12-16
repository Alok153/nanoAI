package com.vjaykrsna.nanoai.feature.chat.presentation

import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.chat.domain.ChatFeatureCoordinator
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val SWITCH_PERSONA_ERROR = "Failed to switch persona"

@HiltViewModel
class PersonaSwitcherViewModel
@Inject
constructor(
  private val chatFeatureCoordinator: ChatFeatureCoordinator,
  @MainImmediateDispatcher dispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<PersonaSwitcherUiState, PersonaSwitcherEvent>(
    initialState = PersonaSwitcherUiState(),
    dispatcher = dispatcher,
  ) {

  init {
    observePersonas()
  }

  fun setActiveThread(threadId: UUID?, personaId: UUID?) {
    updateState {
      copy(
        activeThreadId = threadId,
        selectedPersonaId = personaId ?: selectedPersonaId,
      )
    }
  }

  fun switchPersona(personaId: UUID, action: PersonaSwitchAction) {
    val currentThreadId = state.value.activeThreadId
    viewModelScope.launch(dispatcher) {
      updateState { copy(isSwitching = true, selectedPersonaId = personaId, errorMessage = null) }

      val result =
        if (currentThreadId == null) {
          chatFeatureCoordinator.createThread(personaId)
        } else {
          chatFeatureCoordinator.switchPersona(currentThreadId, personaId, action)
        }

      result
        .onSuccess { targetThreadId ->
          updateState {
            copy(
              isSwitching = false,
              activeThreadId = targetThreadId,
              selectedPersonaId = personaId,
            )
          }
          emitEvent(
            PersonaSwitcherEvent.SwitchCompleted(
              targetThreadId = targetThreadId,
              personaId = personaId,
              action = action,
            )
          )
        }
        .onFailure { recoverable ->
          val envelope = recoverable.toErrorEnvelope(SWITCH_PERSONA_ERROR)
          updateState { copy(isSwitching = false, errorMessage = envelope.userMessage) }
          emitEvent(PersonaSwitcherEvent.ErrorRaised(envelope.userMessage))
        }
    }
  }

  private fun observePersonas() {
    viewModelScope.launch(dispatcher) {
      chatFeatureCoordinator.observePersonas().collectLatest { personas ->
        updateState {
          val persistedSelection =
            selectedPersonaId ?: personas.firstOrNull()?.personaId
          copy(personas = personas.toPersistentList(), selectedPersonaId = persistedSelection)
        }
      }
    }
  }
}

data class PersonaSwitcherUiState(
  val personas: PersistentList<PersonaProfile> = persistentListOf(),
  val activeThreadId: UUID? = null,
  val selectedPersonaId: UUID? = null,
  val isSwitching: Boolean = false,
  val errorMessage: String? = null,
) : NanoAIViewState

sealed interface PersonaSwitcherEvent : NanoAIViewEvent {
  data class SwitchCompleted(
    val targetThreadId: UUID,
    val personaId: UUID,
    val action: PersonaSwitchAction,
  ) : PersonaSwitcherEvent

  data class ErrorRaised(val message: String) : PersonaSwitcherEvent
}
