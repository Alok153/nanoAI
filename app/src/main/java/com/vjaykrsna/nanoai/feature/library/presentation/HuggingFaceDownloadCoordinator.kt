package com.vjaykrsna.nanoai.feature.library.presentation

import com.vjaykrsna.nanoai.core.common.fold
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import java.io.IOException
import kotlinx.coroutines.CancellationException

/** Coordinates Hugging Face model conversions and download setup. */
internal class HuggingFaceDownloadCoordinator(
  private val converter: HuggingFaceToModelPackageConverter,
  private val modelCatalogUseCase: ModelCatalogUseCase,
  private val downloadModelUseCase: DownloadModelUseCase,
  private val emitError: suspend (LibraryError) -> Unit,
) {

  suspend fun process(model: HuggingFaceModelSummary) {
    try {
      val modelPackage = convertOrEmit(model) ?: return
      if (!ensureModelIsNewOrEmit(modelPackage)) return
      if (!addModelToCatalogOrEmit(modelPackage)) return
      startDownloadOrEmit(modelPackage.modelId)
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (ioException: IOException) {
      emitError(
        LibraryError.DownloadFailed(
          modelId = model.modelId,
          message = "Network error while processing Hugging Face model: ${ioException.message}",
        )
      )
    } catch (illegalStateException: IllegalStateException) {
      emitError(
        LibraryError.DownloadFailed(
          modelId = model.modelId,
          message = "Failed to process Hugging Face model: ${illegalStateException.message}",
        )
      )
    } catch (illegalArgumentException: IllegalArgumentException) {
      emitError(
        LibraryError.DownloadFailed(
          modelId = model.modelId,
          message = "Invalid Hugging Face model metadata: ${illegalArgumentException.message}",
        )
      )
    }
  }

  private suspend fun convertOrEmit(model: HuggingFaceModelSummary): ModelPackage? {
    val packageOrNull = converter.convertIfCompatible(model)
    if (packageOrNull == null) {
      emitError(
        LibraryError.DownloadFailed(
          modelId = model.modelId,
          message = "Model is not compatible with local runtimes",
        )
      )
    }
    return packageOrNull
  }

  private suspend fun ensureModelIsNewOrEmit(modelPackage: ModelPackage): Boolean {
    val existingModel =
      modelCatalogUseCase
        .getModel(modelPackage.modelId)
        .fold(onSuccess = { it }, onFailure = { null })

    if (existingModel != null) {
      emitError(
        LibraryError.DownloadFailed(
          modelId = modelPackage.modelId,
          message = "Model already exists in catalog",
        )
      )
      return false
    }
    return true
  }

  private suspend fun addModelToCatalogOrEmit(modelPackage: ModelPackage): Boolean {
    var success = true
    modelCatalogUseCase.upsertModel(modelPackage).onFailure { error ->
      success = false
      emitError(
        LibraryError.DownloadFailed(
          modelId = modelPackage.modelId,
          message = "Failed to add model to catalog: ${error.message}",
        )
      )
    }
    return success
  }

  private suspend fun startDownloadOrEmit(modelId: String) {
    downloadModelUseCase.downloadModel(modelId).onFailure { error ->
      emitError(
        LibraryError.DownloadFailed(
          modelId = modelId,
          message = error.message ?: "Failed to start download",
        )
      )
    }
  }
}
