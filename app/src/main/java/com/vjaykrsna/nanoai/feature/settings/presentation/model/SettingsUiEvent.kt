package com.vjaykrsna.nanoai.feature.settings.presentation.model

import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent

sealed interface SettingsUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val envelope: NanoAIErrorEnvelope) : SettingsUiEvent

  data class ExportCompleted(val destinationPath: String) : SettingsUiEvent

  data class ImportCompleted(val summary: ImportSummary) : SettingsUiEvent
}
