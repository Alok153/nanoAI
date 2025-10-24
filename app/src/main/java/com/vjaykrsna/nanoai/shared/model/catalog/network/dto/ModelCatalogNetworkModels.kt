package com.vjaykrsna.nanoai.shared.model.catalog.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Network DTO for model manifest retrieval. */
@Serializable
data class ModelManifestDto(
  val modelId: String,
  val version: String,
  val checksumSha256: String,
  val sizeBytes: Long,
  val downloadUrl: String,
  val signature: String? = null,
  val publicKeyUrl: String? = null,
  val expiresAt: String? = null,
)

/** Verification status for manifest download result. */
@Serializable
enum class ManifestVerificationStatusDto {
  @SerialName("SUCCESS") SUCCESS,
  @SerialName("CORRUPTED") CORRUPTED,
}

/** Verification status response from the API. */
@Serializable
enum class ManifestVerificationResponseStatusDto {
  @SerialName("ACCEPTED") ACCEPTED,
  @SerialName("RETRY") RETRY,
}

/** Request payload submitted after verifying a downloaded model package. */
@Serializable
data class ManifestVerificationRequestDto(
  val version: String,
  val checksumSha256: String,
  val deviceId: String,
  val verifiedAt: String,
  val status: ManifestVerificationStatusDto,
  val failureReason: String? = null,
)

/** Response payload for manifest verification submission. */
@Serializable
data class ManifestVerificationResponseDto(
  val status: ManifestVerificationResponseStatusDto,
  val nextRetryAfterSeconds: Int? = null,
)

/** Error envelope returned by backend endpoints when a request fails. */
@Serializable
data class ErrorEnvelopeDto(
  val code: String,
  val message: String,
  val retryAfterSeconds: Int? = null,
  val telemetryId: String? = null,
  val details: Map<String, String>? = null,
)
