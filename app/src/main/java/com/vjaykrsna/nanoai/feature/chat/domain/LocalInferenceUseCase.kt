package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import javax.inject.Inject
import kotlin.collections.buildList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Snapshot of a local model candidate that can run on-device. */
data class LocalModelCandidate(
  val modelId: String,
  val displayName: String,
  val providerType: ProviderType,
  val installState: InstallState,
  val sizeBytes: Long,
)

/** Indicates whether a ready local model is available. */
sealed interface LocalModelReadiness {
  data object Unknown : LocalModelReadiness

  data class Ready(val candidate: LocalModelCandidate, val autoSelected: Boolean) :
    LocalModelReadiness

  enum class MissingReason {
    NO_LOCAL_MODELS,
    NOT_READY,
  }

  data class Missing(val reason: MissingReason) : LocalModelReadiness
}

/** Repository contract for querying installed local models and runtime readiness. */
interface LocalInferenceRepository {
  fun observeInstalledLocalModels(): Flow<List<ModelPackage>>

  suspend fun getInstalledLocalModels(): List<ModelPackage>

  suspend fun isModelReady(modelId: String): Boolean
}

/** Use case that selects the best-fit on-device model for offline chat. */
class LocalInferenceUseCase @Inject constructor(private val repository: LocalInferenceRepository) {

  suspend fun prepareForOffline(
    currentModelId: String?,
    personaModelPreference: String?,
  ): LocalModelReadiness {
    val installed = repository.getInstalledLocalModels().localCandidates()
    if (installed.isEmpty()) {
      return LocalModelReadiness.Missing(LocalModelReadiness.MissingReason.NO_LOCAL_MODELS)
    }

    val candidateOrder = buildCandidateOrder(currentModelId, personaModelPreference, installed)
    val readyCandidate =
      candidateOrder.firstNotNullOfOrNull { candidateId ->
        val resolved =
          installed.firstOrNull { it.modelId == candidateId } ?: return@firstNotNullOfOrNull null
        if (repository.isModelReady(resolved.modelId)) resolved to candidateId else null
      }

    return if (readyCandidate != null) {
      LocalModelReadiness.Ready(
        candidate = readyCandidate.first.toCandidate(),
        autoSelected = readyCandidate.second != currentModelId,
      )
    } else {
      LocalModelReadiness.Missing(LocalModelReadiness.MissingReason.NOT_READY)
    }
  }

  fun observeLocalModelCandidates(): Flow<List<LocalModelCandidate>> =
    repository.observeInstalledLocalModels().map { packages -> packages.localCandidates().toUi() }

  private fun buildCandidateOrder(
    currentModelId: String?,
    personaPreference: String?,
    installed: List<ModelPackage>,
  ): List<String> {
    val installedIds = installed.map { it.modelId }
    return buildList {
        currentModelId?.let { add(it) }
        personaPreference?.let { add(it) }
        installedIds.forEach { add(it) }
      }
      .distinct()
  }
}

private fun List<ModelPackage>.localCandidates(): List<ModelPackage> = filter {
  it.providerType != ProviderType.CLOUD_API && it.installState == InstallState.INSTALLED
}

private fun List<ModelPackage>.toUi(): List<LocalModelCandidate> = map { it.toCandidate() }

private fun ModelPackage.toCandidate(): LocalModelCandidate =
  LocalModelCandidate(
    modelId = modelId,
    displayName = displayName,
    providerType = providerType,
    installState = installState,
    sizeBytes = sizeBytes,
  )
