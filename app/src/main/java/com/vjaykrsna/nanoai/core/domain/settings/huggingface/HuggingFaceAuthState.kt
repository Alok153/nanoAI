package com.vjaykrsna.nanoai.core.domain.settings.huggingface

import kotlinx.datetime.Instant

/** High-level representation of Hugging Face authentication status. */
data class HuggingFaceAuthState(
  val isAuthenticated: Boolean = false,
  val username: String? = null,
  val displayName: String? = null,
  val avatarUrl: String? = null,
  val tokenSource: HuggingFaceTokenSource = HuggingFaceTokenSource.NONE,
  val lastVerifiedAt: Instant? = null,
  val isVerifying: Boolean = false,
  val lastError: String? = null,
) {
  val accountLabel: String?
    get() = displayName?.takeIf { it.isNotBlank() } ?: username

  companion object {
    fun unauthenticated(): HuggingFaceAuthState = HuggingFaceAuthState()
  }
}

/** Enumerates the origin of the persisted Hugging Face credential. */
enum class HuggingFaceTokenSource(val id: String) {
  NONE("none"),
  API_TOKEN("api_token"),
  OAUTH("oauth"),
  UNKNOWN("unknown");

  companion object {
    fun fromId(id: String?): HuggingFaceTokenSource =
      values().firstOrNull { it.id == id } ?: UNKNOWN
  }
}

data class HuggingFaceDeviceAuthState(
  val userCode: String,
  val verificationUri: String,
  val verificationUriComplete: String?,
  val expiresAt: Instant,
  var pollIntervalSeconds: Int,
  var isPolling: Boolean = false,
  var lastError: String? = null,
  var lastErrorAnnouncement: String? = null,
)
