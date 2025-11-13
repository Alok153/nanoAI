package com.vjaykrsna.nanoai.feature.library.presentation

import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryDownloadItem
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySummary
import com.vjaykrsna.nanoai.feature.library.presentation.util.filterBy
import java.util.Locale

private const val PRIORITY_DOWNLOADING = 0
private const val PRIORITY_PAUSED = 1
private const val PRIORITY_QUEUED = 2
private const val PRIORITY_FAILED = 3
private const val PRIORITY_COMPLETED = 4
private const val PRIORITY_CANCELLED = 5

internal data class ModelLibraryDerivedContent(
  val localSections: ModelLibrarySections,
  val curatedSections: ModelLibrarySections,
  val summary: ModelLibrarySummary,
  val providerOptions: List<ProviderType>,
  val capabilityOptions: List<String>,
)

internal fun deriveModelLibraryContent(
  allModels: List<ModelPackage>,
  installedModels: List<ModelPackage>,
  downloadTasks: List<DownloadTask>,
  filterState: LibraryFilterState,
): ModelLibraryDerivedContent {
  val filteredModels = allModels.filterBy(filterState)

  val prioritizedDownloads =
    downloadTasks
      .filter { it.status.isActiveDownload() }
      .sortedWith(
        compareBy<DownloadTask> { downloadPriority(it.status) }
          .thenByDescending { it.progress }
          .thenBy { it.modelId }
      )

  val downloadItems =
    prioritizedDownloads.map { task ->
      val associatedModel = allModels.firstOrNull { it.modelId == task.modelId }
      LibraryDownloadItem(task = task, model = associatedModel)
    }

  val activeIds = prioritizedDownloads.map { it.modelId }.toSet()

  val localModels =
    filteredModels.filter { model ->
      model.installState == InstallState.INSTALLED || model.installState == InstallState.ERROR
    }

  val curatedAvailable =
    filteredModels
      .filter { it.installState == InstallState.NOT_INSTALLED }
      .filterNot { it.modelId in activeIds }
  val curatedInstalled = filteredModels.filter { it.installState == InstallState.INSTALLED }
  val curatedAttention = filteredModels.filter { it.installState == InstallState.ERROR }

  val localSections =
    ModelLibrarySections(
      downloads = downloadItems,
      attention = localModels.filter { it.installState == InstallState.ERROR },
      installed = localModels.filter { it.installState == InstallState.INSTALLED },
      available = emptyList(),
    )

  val curatedSections =
    ModelLibrarySections(
      downloads = downloadItems,
      attention = curatedAttention,
      installed = curatedInstalled,
      available = curatedAvailable,
    )

  val summary = buildSummary(allModels, installedModels)
  val providerOptions = buildProviderOptions(allModels)
  val capabilityOptions = buildCapabilityOptions(allModels)

  return ModelLibraryDerivedContent(
    localSections = localSections,
    curatedSections = curatedSections,
    summary = summary,
    providerOptions = providerOptions,
    capabilityOptions = capabilityOptions,
  )
}

private fun buildSummary(
  allModels: List<ModelPackage>,
  installedModels: List<ModelPackage>,
): ModelLibrarySummary {
  val attentionCount = allModels.count { it.installState == InstallState.ERROR }
  val availableCount = allModels.count { it.installState == InstallState.NOT_INSTALLED }
  return ModelLibrarySummary(
    total = allModels.size,
    installed = installedModels.size,
    attention = attentionCount,
    available = availableCount,
    installedBytes = installedModels.sumOf(ModelPackage::sizeBytes),
  )
}

private fun buildProviderOptions(models: List<ModelPackage>): List<ProviderType> =
  models.map(ModelPackage::providerType).distinct().sortedBy { it.name }

private fun buildCapabilityOptions(models: List<ModelPackage>): List<String> =
  models
    .flatMap { it.capabilities }
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .map { it.lowercase(Locale.US) }
    .distinct()
    .sorted()

private fun downloadPriority(status: DownloadStatus): Int =
  when (status) {
    DownloadStatus.DOWNLOADING -> PRIORITY_DOWNLOADING
    DownloadStatus.PAUSED -> PRIORITY_PAUSED
    DownloadStatus.QUEUED -> PRIORITY_QUEUED
    DownloadStatus.FAILED -> PRIORITY_FAILED
    DownloadStatus.COMPLETED -> PRIORITY_COMPLETED
    DownloadStatus.CANCELLED -> PRIORITY_CANCELLED
  }

private fun DownloadStatus.isActiveDownload(): Boolean =
  this == DownloadStatus.DOWNLOADING ||
    this == DownloadStatus.PAUSED ||
    this == DownloadStatus.QUEUED ||
    this == DownloadStatus.FAILED
