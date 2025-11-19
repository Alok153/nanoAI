package com.vjaykrsna.nanoai.core.domain.settings.huggingface

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for coordinating Hugging Face authentication flows.
 *
 * Exposes state streams for UI consumption along with operations for credential lifecycle
 * management and device authorization flows.
 */
interface HuggingFaceAuthCoordinator {
  /** Reactive authentication state. */
  val state: StateFlow<HuggingFaceAuthState>

  /** Optional state describing active device authorization flow. */
  val deviceAuthState: StateFlow<HuggingFaceDeviceAuthState?>

  /** Persist a personal access token and refresh account information. */
  @OneShot("Save Hugging Face access token")
  suspend fun savePersonalAccessToken(token: String): NanoAIResult<HuggingFaceAuthState>

  /** Begin the OAuth device authorization flow. */
  @OneShot("Begin Hugging Face device authorization")
  suspend fun beginDeviceAuthorization(
    clientId: String,
    scope: String,
  ): NanoAIResult<HuggingFaceDeviceAuthState>

  /** Cancel any in-progress device authorization flow. */
  suspend fun cancelDeviceAuthorization()

  /** Clear stored credentials and reset the auth state. */
  suspend fun clearCredentials()

  /** Force a refresh of the stored credential (if any). */
  suspend fun refreshAccount(): HuggingFaceAuthState
}
