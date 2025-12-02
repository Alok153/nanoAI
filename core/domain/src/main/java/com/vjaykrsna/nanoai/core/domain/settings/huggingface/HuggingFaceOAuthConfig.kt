package com.vjaykrsna.nanoai.core.domain.settings.huggingface

/** Configuration for initiating Hugging Face device-auth flows. */
data class HuggingFaceOAuthConfig(val clientId: String, val scope: String) {
  val isClientConfigured: Boolean
    get() = clientId.isNotBlank()
}
