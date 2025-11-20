package com.vjaykrsna.nanoai.feature.library.presentation.model

import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent

sealed class LibraryError {
  data class DownloadFailed(val modelId: String, val message: String) : LibraryError()

  data class PauseFailed(val taskId: String, val message: String) : LibraryError()

  data class ResumeFailed(val taskId: String, val message: String) : LibraryError()

  data class CancelFailed(val taskId: String, val message: String) : LibraryError()

  data class RetryFailed(val taskId: String, val message: String) : LibraryError()

  data class DeleteFailed(val modelId: String, val message: String) : LibraryError()

  data class UnexpectedError(val message: String) : LibraryError()

  data class HuggingFaceLoadFailed(val message: String) : LibraryError()
}

sealed interface ModelLibraryUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: LibraryError, val envelope: NanoAIErrorEnvelope) :
    ModelLibraryUiEvent

  data class Message(val message: String) : ModelLibraryUiEvent

  data object RequestLocalModelImport : ModelLibraryUiEvent
}
