package com.vjaykrsna.nanoai.core.data.library.catalog

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import javax.inject.Inject
import javax.inject.Singleton

/** Abstraction over manifest API + Room cache. */
interface ModelManifestRepository {
  /** Fetches the latest manifest for a model/version and caches it locally. */
  @OneShot("Refresh model download manifest")
  suspend fun refreshManifest(modelId: String, version: String): NanoAIResult<DownloadManifest>

  /** Reports verification result back to the catalog service. */
  @OneShot("Report manifest verification result")
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
  private val modelManifestUseCase: com.vjaykrsna.nanoai.core.domain.library.ModelManifestUseCase
) : ModelManifestRepository {
  @OneShot("Refresh model download manifest")
  override suspend fun refreshManifest(
    modelId: String,
    version: String,
  ): NanoAIResult<DownloadManifest> = modelManifestUseCase.refreshManifest(modelId, version)

  @OneShot("Report manifest verification result")
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
