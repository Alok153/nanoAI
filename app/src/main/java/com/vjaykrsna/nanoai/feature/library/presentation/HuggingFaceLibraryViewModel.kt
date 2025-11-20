package com.vjaykrsna.nanoai.feature.library.presentation

import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.withFallbackMessage
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceLibraryUiEvent
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.toQuery
import com.vjaykrsna.nanoai.feature.library.presentation.state.HuggingFaceLibraryUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val HUGGING_FACE_SEARCH_DEBOUNCE_MS = 350L
private const val HUGGING_FACE_LOAD_ERROR = "Unable to load Hugging Face catalog"

class HuggingFaceLibraryViewModel
@Inject
constructor(
  private val huggingFaceCatalogUseCase: HuggingFaceCatalogUseCase,
  private val compatibilityChecker: HuggingFaceModelCompatibilityChecker,
  @MainImmediateDispatcher private val mainDispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<HuggingFaceLibraryUiState, HuggingFaceLibraryUiEvent>(
    initialState = HuggingFaceLibraryUiState(),
    dispatcher = mainDispatcher,
  ) {

  private val filtersFlow = state.map { it.filters }.distinctUntilChanged()

  private var initialized = false
  private var lastFilters: HuggingFaceFilterState? = null

  init {
    observeFilterChanges()
  }

  private fun observeFilterChanges() {
    viewModelScope.launch(mainDispatcher) {
      filtersFlow.debounce(HUGGING_FACE_SEARCH_DEBOUNCE_MS).collect { filters ->
        fetchModels(filters)
      }
    }
  }

  private fun fetchModels(filters: HuggingFaceFilterState, force: Boolean = false) {
    if (!force && !shouldFetch(filters)) return

    initialized = true
    lastFilters = filters

    viewModelScope.launch(mainDispatcher) {
      updateState { copy(isLoading = true, lastErrorMessage = null) }
      val query = filters.toQuery()
      val result = huggingFaceCatalogUseCase.listModels(query)
      when (result) {
        is NanoAIResult.Success -> {
          val models = result.value.withNormalizedTags()
          updateState {
            copy(
              models = models,
              pipelineOptions = models.pipelineOptions(),
              libraryOptions = models.libraryOptions(),
              downloadableModelIds = models.downloadableIds(compatibilityChecker),
              filters = filters,
              lastErrorMessage = null,
            )
          }
        }
        is NanoAIResult.RecoverableError -> {
          emitHuggingFaceError(result.message, result.toErrorEnvelope(HUGGING_FACE_LOAD_ERROR))
        }
        is NanoAIResult.FatalError -> {
          emitHuggingFaceError(result.message, result.toErrorEnvelope(HUGGING_FACE_LOAD_ERROR))
        }
      }
      updateState { copy(isLoading = false) }
    }
  }

  private fun shouldFetch(filters: HuggingFaceFilterState): Boolean {
    if (!initialized) return true
    return lastFilters != filters
  }

  fun updateSearchQuery(query: String) {
    updateState { copy(filters = filters.copy(searchQuery = query)) }
  }

  fun setPipeline(pipelineTag: String?) {
    updateState { copy(filters = filters.copy(pipelineTag = pipelineTag)) }
  }

  fun setSort(sort: HuggingFaceSortOption) {
    updateState { copy(filters = filters.copy(sort = sort)) }
  }

  fun setLibrary(library: String?) {
    updateState { copy(filters = filters.copy(library = library)) }
  }

  fun clearFilters() {
    updateState { copy(filters = HuggingFaceFilterState()) }
  }

  fun requestDownload(model: HuggingFaceModelSummary) {
    viewModelScope.launch(mainDispatcher) {
      emitEvent(HuggingFaceLibraryUiEvent.DownloadRequested(model))
    }
  }

  private suspend fun emitHuggingFaceError(message: String, rawEnvelope: NanoAIErrorEnvelope) {
    val error = LibraryError.HuggingFaceLoadFailed(message)
    val envelope = rawEnvelope.withFallbackMessage(HUGGING_FACE_LOAD_ERROR)
    updateState { copy(lastErrorMessage = envelope.userMessage) }
    emitEvent(HuggingFaceLibraryUiEvent.ErrorRaised(error, envelope))
  }
}

private fun List<HuggingFaceModelSummary>.withNormalizedTags(): List<HuggingFaceModelSummary> =
  map { summary ->
    summary.copy(tags = summary.tags.normalizeTags())
  }

private fun Collection<String>.normalizeTags(): List<String> {
  if (isEmpty()) return emptyList()
  val normalized = map { it.trim() }.filter { it.isNotEmpty() }
  val hasMultimodal = normalized.any { it.equals("multimodal", ignoreCase = true) }
  return normalized.filterNot { hasMultimodal && it.equals("text-generation", ignoreCase = true) }
}

private fun List<HuggingFaceModelSummary>.pipelineOptions(): List<String> =
  mapNotNull { it.pipelineTag?.takeIf(String::isNotBlank) }
    .distinctBy { it.lowercase(Locale.US) }
    .sortedBy { it.lowercase(Locale.US) }

private fun List<HuggingFaceModelSummary>.libraryOptions(): List<String> =
  mapNotNull { it.libraryName?.takeIf(String::isNotBlank) }
    .distinctBy { it.lowercase(Locale.US) }
    .sortedBy { it.lowercase(Locale.US) }

private fun List<HuggingFaceModelSummary>.downloadableIds(
  compatibilityChecker: HuggingFaceModelCompatibilityChecker
): Set<String> =
  filter { compatibilityChecker.checkCompatibility(it) != null }.map { it.modelId }.toSet()
