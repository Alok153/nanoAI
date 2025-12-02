package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadManifest
import com.vjaykrsna.nanoai.core.domain.model.library.VerificationOutcome
import javax.inject.Inject
import javax.inject.Singleton

/** Use case facade for manifest operations ensuring callers stay within the domain layer. */
@Singleton
class ModelManifestUseCase @Inject constructor(private val repository: ModelManifestRepository) :
  ModelManifestUseCaseInterface {
  @OneShot("Refresh model download manifest")
  override suspend fun refreshManifest(
    modelId: String,
    version: String,
  ): NanoAIResult<DownloadManifest> = repository.refreshManifest(modelId, version)

  @OneShot("Report manifest verification result")
  override suspend fun reportVerification(
    modelId: String,
    version: String,
    checksumSha256: String,
    status: VerificationOutcome,
    failureReason: String?,
  ): NanoAIResult<Unit> =
    repository.reportVerification(
      modelId = modelId,
      version = version,
      checksumSha256 = checksumSha256,
      status = status,
      failureReason = failureReason,
    )
}
