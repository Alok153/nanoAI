package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.data.library.catalog.DownloadManifest
import com.vjaykrsna.nanoai.core.data.library.catalog.VerificationOutcome

/**
 * Interface for model manifest operations including fetching, validation, and verification
 * reporting.
 */
interface ModelManifestUseCaseInterface {
  @OneShot("Refresh model download manifest")
  suspend fun refreshManifest(modelId: String, version: String): NanoAIResult<DownloadManifest>

  @OneShot("Report manifest verification result")
  suspend fun reportVerification(
    modelId: String,
    version: String,
    checksumSha256: String,
    status: VerificationOutcome,
    failureReason: String? = null,
  ): NanoAIResult<Unit>
}
