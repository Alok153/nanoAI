package com.vjaykrsna.nanoai.feature.library.presentation.model

import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent

sealed interface HuggingFaceLibraryUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: LibraryError, val envelope: NanoAIErrorEnvelope) :
    HuggingFaceLibraryUiEvent

  data class DownloadRequested(val model: HuggingFaceModelSummary) : HuggingFaceLibraryUiEvent
}
