package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.model.InferenceMode

/**
 * Encapsulates the user-selected inference routing preference.
 */
data class InferencePreference(
    val mode: InferenceMode = InferenceMode.LOCAL_FIRST,
)
