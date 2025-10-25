package com.vjaykrsna.nanoai.feature.library.data.catalog

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import javax.inject.Inject
import javax.inject.Singleton

/** Abstraction over manifest API + Room cache. */
interface ModelManifestRepository {
  /** Fetches the latest manifest for a model/version and caches it locally. */
  suspend fun refreshManifest(modelId: String, version: String): NanoAIResult<DownloadManifest>

  /** Reports verification result back to the catalog service. */
  suspend fun reportVerification(
    modelId: String,
    version: String,
    checksumSha256: String,
    status: VerificationOutcome,
    failureReason: String? = null,
  ): NanoAIResult<Unit>
}

/** Result status for reporting manifest verification. */
enum class VerificationOutcome {
  SUCCESS,
  CORRUPTED,
}

@Singleton
class ModelManifestRepositoryImpl
@Inject
constructor(
  private val modelManifestUseCase: com.vjaykrsna.nanoai.feature.library.domain.ModelManifestUseCase
) : ModelManifestRepository {
  override suspend fun refreshManifest(
    modelId: String,
    version: String,
  ): NanoAIResult<DownloadManifest> = modelManifestUseCase.refreshManifest(modelId, version)

  override suspend fun reportVerification(
    modelId: String,
    version: String,
    checksumSha256: String,
    status: VerificationOutcome,
    failureReason: String?,
  ): NanoAIResult<Unit> =
    modelManifestUseCase.reportVerification(
      modelId = modelId,
      version = version,
      checksumSha256 = checksumSha256,
      status = status,
      failureReason = failureReason,
    )
}
