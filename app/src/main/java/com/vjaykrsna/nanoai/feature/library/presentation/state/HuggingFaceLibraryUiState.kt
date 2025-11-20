package com.vjaykrsna.nanoai.feature.library.presentation.state

import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceFilterState
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState

/**
 * Immutable representation of the Hugging Face library browser.
 *
 * Exposes catalog models, filters, and derived options from a single source so the model library
 * host can consume a consolidated snapshot.
 */
data class HuggingFaceLibraryUiState(
  val models: List<HuggingFaceModelSummary> = emptyList(),
  val filters: HuggingFaceFilterState = HuggingFaceFilterState(),
  val pipelineOptions: List<String> = emptyList(),
  val libraryOptions: List<String> = emptyList(),
  val downloadableModelIds: Set<String> = emptySet(),
  val isLoading: Boolean = false,
  val lastErrorMessage: String? = null,
) : NanoAIViewState
