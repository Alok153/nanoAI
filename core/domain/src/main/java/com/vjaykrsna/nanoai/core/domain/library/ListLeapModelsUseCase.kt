package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** A use case that lists all available Leap models. */
class ListLeapModelsUseCase
@Inject
constructor(private val modelCatalogRepository: ModelCatalogRepository) {
  /** Returns a list of all available Leap models. */
  @OneShot("Fetch Leap provider catalog snapshot")
  suspend operator fun invoke(): NanoAIResult<List<ModelPackage>> = guardLeapListing {
    val models = modelCatalogRepository.getAllModels()
    val leapModels = models.filter { it.providerType == ProviderType.LEAP }
    NanoAIResult.success(leapModels)
  }

  private inline fun <T> guardLeapListing(block: () -> NanoAIResult<T>): NanoAIResult<T> {
    return try {
      block()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (error: Throwable) {
      NanoAIResult.recoverable(
        message = "Failed to load Leap catalog entries",
        cause = error,
        context = mapOf("providerType" to ProviderType.LEAP.name),
      )
    }
  }
}
