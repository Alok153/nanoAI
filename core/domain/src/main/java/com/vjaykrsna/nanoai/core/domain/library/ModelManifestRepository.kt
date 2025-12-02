package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadManifest
import com.vjaykrsna.nanoai.core.domain.model.library.VerificationOutcome

/** Repository abstraction for fetching and verifying model manifests. */
interface ModelManifestRepository {
  /** Fetch the latest manifest for the given model/version, caching as needed. */
  @OneShot("Refresh model download manifest")
  suspend fun refreshManifest(modelId: String, version: String): NanoAIResult<DownloadManifest>

  /** Report verification status for a manifest back to the catalog service. */
  @OneShot("Report manifest verification result")
  suspend fun reportVerification(
    modelId: String,
    version: String,
    checksumSha256: String,
    status: VerificationOutcome,
    failureReason: String? = null,
  ): NanoAIResult<Unit>
}
