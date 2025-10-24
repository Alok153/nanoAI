package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.model.catalog.DownloadManifest
import com.vjaykrsna.nanoai.model.catalog.VerificationOutcome

/**
 * Interface for model manifest operations including fetching, validation, and verification
 * reporting.
 */
interface ModelManifestUseCaseInterface {
  suspend fun refreshManifest(modelId: String, version: String): NanoAIResult<DownloadManifest>

  suspend fun reportVerification(
    modelId: String,
    version: String,
    checksumSha256: String,
    status: VerificationOutcome,
    failureReason: String? = null,
  ): NanoAIResult<Unit>
}
