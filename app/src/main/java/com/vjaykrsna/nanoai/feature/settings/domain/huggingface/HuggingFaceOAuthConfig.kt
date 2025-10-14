package com.vjaykrsna.nanoai.feature.settings.domain.huggingface

/** Configuration for initiating Hugging Face device-auth flows. */
data class HuggingFaceOAuthConfig(
  val clientId: String,
  val scope: String,
) {
  val isClientConfigured: Boolean
    get() = clientId.isNotBlank()
}
