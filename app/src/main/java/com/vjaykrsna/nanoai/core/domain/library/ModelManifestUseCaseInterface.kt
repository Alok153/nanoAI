package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.library.catalog.DownloadManifest
import com.vjaykrsna.nanoai.core.data.library.catalog.VerificationOutcome

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
