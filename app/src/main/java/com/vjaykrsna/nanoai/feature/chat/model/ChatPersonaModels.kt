package com.vjaykrsna.nanoai.feature.chat.model

import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.feature.chat.domain.LocalModelReadiness
import java.util.UUID

/** Lightweight summary of the active persona for rendering and inference decisions. */
data class ChatPersonaSummary(
  val personaId: UUID,
  val displayName: String,
  val description: String,
  val preferredModelId: String?,
)

/** Presentation-model describing offline readiness for the current persona. */
data class LocalInferenceUiState(
  val personaName: String? = null,
  val modelName: String? = null,
  val status: LocalInferenceUiStatus = LocalInferenceUiStatus.Idle,
)

sealed interface LocalInferenceUiStatus {
  data object Idle : LocalInferenceUiStatus

  data class OfflineReady(val autoSelected: Boolean) : LocalInferenceUiStatus

  data class OfflineMissing(val reason: LocalInferenceMissingReason) : LocalInferenceUiStatus
}

enum class LocalInferenceMissingReason {
  NO_LOCAL_MODEL,
  MODEL_NOT_READY,
}

fun PersonaProfile.toPersonaSummary(): ChatPersonaSummary =
  ChatPersonaSummary(
    personaId = personaId,
    displayName = name,
    description = description,
    preferredModelId = defaultModelPreference,
  )

fun LocalModelReadiness.toUiState(persona: ChatPersonaSummary?): LocalInferenceUiState =
  when (this) {
    is LocalModelReadiness.Ready ->
      LocalInferenceUiState(
        personaName = persona?.displayName,
        modelName = candidate.displayName,
        status = LocalInferenceUiStatus.OfflineReady(autoSelected),
      )
    is LocalModelReadiness.Missing ->
      LocalInferenceUiState(
        personaName = persona?.displayName,
        status =
          LocalInferenceUiStatus.OfflineMissing(
            when (reason) {
              LocalModelReadiness.MissingReason.NO_LOCAL_MODELS ->
                LocalInferenceMissingReason.NO_LOCAL_MODEL
              LocalModelReadiness.MissingReason.NOT_READY ->
                LocalInferenceMissingReason.MODEL_NOT_READY
            }
          ),
      )
    LocalModelReadiness.Unknown -> LocalInferenceUiState(personaName = persona?.displayName)
  }
