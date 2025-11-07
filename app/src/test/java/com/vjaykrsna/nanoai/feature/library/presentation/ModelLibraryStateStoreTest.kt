package com.vjaykrsna.nanoai.feature.library.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModelLibraryStateStoreTest {
  private val dispatcher = StandardTestDispatcher()
  private lateinit var scope: TestScope

  private val allModelsFlow = MutableStateFlow<List<ModelPackage>>(emptyList())
  private val installedModelsFlow = MutableStateFlow<List<ModelPackage>>(emptyList())
  private val downloadTasksFlow =
    MutableStateFlow<List<com.vjaykrsna.nanoai.core.domain.model.DownloadTask>>(emptyList())
  private val loadingFlow = MutableStateFlow(false)

  private val modelCatalogUseCase =
    mockk<com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase>()
  private val downloadManager = mockk<DownloadManager>()
  private val huggingFaceViewModel = mockk<HuggingFaceLibraryViewModel>()

  private lateinit var stateStore: ModelLibraryStateStore

  @BeforeEach
  fun setup() {
    scope = TestScope(dispatcher)

    every { modelCatalogUseCase.observeAllModels() } returns allModelsFlow
    every { modelCatalogUseCase.observeInstalledModels() } returns installedModelsFlow

    every { downloadManager.observeDownloadTasks() } returns downloadTasksFlow
    every { downloadManager.isLoading } returns loadingFlow

    every { huggingFaceViewModel.models } returns MutableStateFlow(emptyList())
    every { huggingFaceViewModel.filters } returns MutableStateFlow(HuggingFaceFilterState())
    every { huggingFaceViewModel.pipelineOptions } returns MutableStateFlow(emptyList())
    every { huggingFaceViewModel.libraryOptions } returns MutableStateFlow(emptyList())
    every { huggingFaceViewModel.downloadableModelIds } returns MutableStateFlow(emptySet())
    every { huggingFaceViewModel.isLoading } returns MutableStateFlow(false)

    justRun { huggingFaceViewModel.updateSearchQuery(any()) }
    justRun { huggingFaceViewModel.setPipeline(any()) }
    justRun { huggingFaceViewModel.setSort(any()) }
    justRun { huggingFaceViewModel.setLibrary(any()) }
    justRun { huggingFaceViewModel.clearFilters() }

    stateStore =
      ModelLibraryStateStore(modelCatalogUseCase, downloadManager, huggingFaceViewModel, scope)
  }

  @Test
  fun beginRefreshPreventsConcurrentRefreshes() {
    assertThat(stateStore.beginRefresh()).isTrue()
    assertThat(stateStore.beginRefresh()).isFalse()
    stateStore.completeRefresh()
    assertThat(stateStore.beginRefresh()).isTrue()
  }

  @Test
  fun updateSearchQueryUpdatesLocalFilter() =
    runTest(dispatcher) {
      stateStore.filtersController.updateSearchQuery("vision")
      advanceUntilIdle()

      assertThat(stateStore.filters.value.localSearchQuery).isEqualTo("vision")
    }

  @Test
  fun updateSearchQueryDelegatesToHuggingFaceWhenTabActive() =
    runTest(dispatcher) {
      stateStore.filtersController.selectTab(ModelLibraryTab.HUGGING_FACE)
      stateStore.filtersController.updateSearchQuery("audio")
      advanceUntilIdle()

      assertThat(stateStore.filters.value.huggingFaceSearchQuery).isEqualTo("audio")
    }

  @Test
  fun selectLocalLibraryFiltersModels() =
    runTest(dispatcher) {
      val mediaPipeModel =
        DomainTestBuilders.buildModelPackage(
          modelId = "local",
          providerType = ProviderType.MEDIA_PIPE,
        )
      val cloudModel =
        DomainTestBuilders.buildModelPackage(
          modelId = "cloud",
          providerType = ProviderType.CLOUD_API,
        )
      allModelsFlow.value = listOf(mediaPipeModel, cloudModel)

      stateStore.filtersController.selectLocalLibrary(ProviderType.MEDIA_PIPE)
      advanceUntilIdle()

      val curated = stateStore.curatedSections.value
      val allModels = curated.attention + curated.installed + curated.available
      assertThat(allModels.map { it.providerType }).containsExactly(ProviderType.MEDIA_PIPE)
    }

  @Test
  fun clearFiltersResetsLocalSettings() =
    runTest(dispatcher) {
      stateStore.filtersController.updateSearchQuery("test")
      stateStore.filtersController.setPipeline("text")
      stateStore.filtersController.setLocalSort(ModelSort.NAME)
      stateStore.filtersController.selectLocalLibrary(ProviderType.CLOUD_API)

      stateStore.filtersController.clearFilters()
      advanceUntilIdle()

      val filters = stateStore.filters.value
      assertThat(filters.localSearchQuery).isEmpty()
      assertThat(filters.pipelineTag).isNull()
      assertThat(filters.localLibrary).isNull()
      assertThat(filters.localSort).isEqualTo(ModelSort.RECOMMENDED)
    }

  @Test
  fun curatedSectionsIncludesDownloads() =
    runTest(dispatcher) {
      val model = DomainTestBuilders.buildModelPackage(modelId = "model")
      allModelsFlow.value = listOf(model)

      val task = DomainTestBuilders.buildDownloadTask(modelId = "model")
      downloadTasksFlow.value = listOf(task)

      advanceUntilIdle()

      val sections = stateStore.curatedSections.value
      assertThat(sections.downloads).hasSize(1)
      assertThat(sections.downloads.first().task).isEqualTo(task)
    }

  @Test
  fun summaryReflectsInstalledModels() =
    runTest(dispatcher) {
      val installed =
        DomainTestBuilders.buildModelPackage(
          modelId = "installed",
          installState = com.vjaykrsna.nanoai.core.domain.model.library.InstallState.INSTALLED,
          sizeBytes = 1024,
        )
      val available =
        DomainTestBuilders.buildModelPackage(
          modelId = "available",
          installState = com.vjaykrsna.nanoai.core.domain.model.library.InstallState.NOT_INSTALLED,
        )
      allModelsFlow.value = listOf(installed, available)
      installedModelsFlow.value = listOf(installed)

      advanceUntilIdle()

      val summary = stateStore.summary.value
      assertThat(summary.total).isEqualTo(2)
      assertThat(summary.installed).isEqualTo(1)
      assertThat(summary.installedBytes).isEqualTo(1024)
    }

  @Test
  fun hasActiveFiltersMatchesState() =
    runTest(dispatcher) {
      assertThat(stateStore.hasActiveFilters.value).isFalse()
      stateStore.filtersController.updateSearchQuery("demo")
      advanceUntilIdle()
      assertThat(stateStore.hasActiveFilters.value).isTrue()
    }

  @Test
  fun localSectionsDefaultsToEmptySections() {
    assertThat(stateStore.localSections.value).isEqualTo(ModelLibrarySections())
  }
}
