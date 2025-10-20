package com.vjaykrsna.nanoai.feature.library.presentation.model

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

sealed class LibraryUiEvent {
  data object RequestLocalModelImport : LibraryUiEvent()
}
