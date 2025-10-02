package com.vjaykrsna.nanoai.core.model

/**
 * User-configurable inference routing preference.
 *
 * LOCAL_FIRST prioritises on-device models when available, while CLOUD_FIRST
 * prefers sending prompts to the cloud gateway unless offline or no cloud
 * providers are configured.
 */
enum class InferenceMode {
    LOCAL_FIRST,
    CLOUD_FIRST,
}
