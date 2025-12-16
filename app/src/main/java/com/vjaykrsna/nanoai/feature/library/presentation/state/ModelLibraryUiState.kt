package com.vjaykrsna.nanoai.feature.library.presentation.state

import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySummary
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState

/**
 * Immutable representation of the model library UI.
 *
 * Consolidates local/curated sections, filters, download progress, and Hugging Face catalog data so
 * composables can render from a single source of truth.
 */
data class ModelLibraryUiState(
  val filters: LibraryFilterState = LibraryFilterState(),
  val summary: ModelLibrarySummary = ModelLibrarySummary(),
  val localSections: ModelLibrarySections = ModelLibrarySections(),
  val curatedSections: ModelLibrarySections = ModelLibrarySections(),
  val huggingFaceModels: List<HuggingFaceModelSummary> = emptyList(),
  val huggingFaceFilterState: HuggingFaceFilterState = HuggingFaceFilterState(),
  val huggingFaceDownloadableModelIds: Set<String> = emptySet(),
  val pipelineOptions: List<String> = emptyList(),
  val huggingFaceLibraryOptions: List<String> = emptyList(),
  val providerOptions: List<ProviderType> = emptyList(),
  val capabilityOptions: List<String> = emptyList(),
  val isLoading: Boolean = false,
  val isRefreshing: Boolean = false,
  val isHuggingFaceLoading: Boolean = false,
  val connectivityStatus: ConnectivityStatus = ConnectivityStatus.ONLINE,
  val lastErrorMessage: String? = null,
) : NanoAIViewState
