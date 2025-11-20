package com.vjaykrsna.nanoai.feature.library.presentation

import app.cash.turbine.TurbineTestContext
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceLibraryUiEvent
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.state.HuggingFaceLibraryUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHostTestHarness
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HuggingFaceLibraryViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  private lateinit var catalogUseCase: HuggingFaceCatalogUseCase
  private lateinit var compatibilityChecker: HuggingFaceModelCompatibilityChecker

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    catalogUseCase = mockk()
    compatibilityChecker = mockk()

    every { compatibilityChecker.checkCompatibility(any()) } returns ProviderType.MEDIA_PIPE
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialFetchPopulatesState() =
    runTest(dispatcher) {
      val summary = sampleModel()
      coEvery { catalogUseCase.listModels(any()) } returns NanoAIResult.success(listOf(summary))

      val harness = createHarness()
      advanceTimeBy(DEBOUNCE_MS)
      advanceUntilIdle()

      val state = harness.awaitState { it.models.isNotEmpty() }
      assertThat(state.models).hasSize(1)
      assertThat(state.pipelineOptions).containsExactly(summary.pipelineTag)
      assertThat(state.libraryOptions).containsExactly(summary.libraryName)
      assertThat(state.downloadableModelIds).containsExactly(summary.modelId)
    }

  @Test
  fun updateSearchQueryUpdatesFilters() =
    runTest(dispatcher) {
      coEvery { catalogUseCase.listModels(any()) } returns NanoAIResult.success(emptyList())
      val harness = createHarness()

      harness.viewModel.updateSearchQuery("vision")
      advanceTimeBy(DEBOUNCE_MS)
      advanceUntilIdle()

      val state = harness.awaitState { it.filters.searchQuery == "vision" }
      assertThat(state.filters.searchQuery).isEqualTo("vision")
    }

  @Test
  fun requestDownloadEmitsEvent() =
    runTest(dispatcher) {
      val summary = sampleModel()
      coEvery { catalogUseCase.listModels(any()) } returns NanoAIResult.success(emptyList())
      val harness = createHarness()

      harness.testEvents {
        harness.viewModel.requestDownload(summary)
        advanceUntilIdle()
        val event = awaitItem()
        assertThat(event).isEqualTo(HuggingFaceLibraryUiEvent.DownloadRequested(summary))
      }
    }

  @Test
  fun fetchFailureEmitsErrorEvent() =
    runTest(dispatcher) {
      coEvery { catalogUseCase.listModels(any()) } returns
        NanoAIResult.recoverable(message = "offline", cause = null)
      val harness = createHarness()

      harness.testEvents {
        advanceTimeBy(DEBOUNCE_MS)
        advanceUntilIdle()
        val event = awaitItem() as HuggingFaceLibraryUiEvent.ErrorRaised
        assertThat(event.error).isInstanceOf(LibraryError.HuggingFaceLoadFailed::class.java)
        assertThat(event.envelope.userMessage).contains("Unable to load Hugging Face catalog")
      }
    }

  private fun createHarness(): HuggingFaceHarness {
    val viewModel =
      HuggingFaceLibraryViewModel(
        huggingFaceCatalogUseCase = catalogUseCase,
        compatibilityChecker = compatibilityChecker,
        mainDispatcher = dispatcher,
      )
    val harness =
      ViewModelStateHostTestHarness<HuggingFaceLibraryUiState, HuggingFaceLibraryUiEvent>(
        viewModel,
        defaultTimeout = 5.seconds,
      )
    return HuggingFaceHarness(viewModel, harness)
  }

  private data class HuggingFaceHarness(
    val viewModel: HuggingFaceLibraryViewModel,
    val stateHarness:
      ViewModelStateHostTestHarness<HuggingFaceLibraryUiState, HuggingFaceLibraryUiEvent>,
  ) {
    suspend fun awaitState(
      predicate: (HuggingFaceLibraryUiState) -> Boolean
    ): HuggingFaceLibraryUiState = stateHarness.awaitState(predicate)

    suspend fun testEvents(
      block: suspend TurbineTestContext<HuggingFaceLibraryUiEvent>.() -> Unit
    ) {
      stateHarness.testEvents(block = block)
    }
  }

  private fun sampleModel(): HuggingFaceModelSummary =
    HuggingFaceModelSummary(
      modelId = "author/model",
      displayName = "Model",
      author = "author",
      pipelineTag = "text-generation",
      libraryName = "transformers",
      tags = listOf("text-generation", "multimodal"),
      likes = 10,
      downloads = 100,
      trendingScore = 1L,
      createdAt = null,
      lastModified = null,
      isPrivate = false,
    )

  private companion object {
    private const val DEBOUNCE_MS = 350L
  }
}
