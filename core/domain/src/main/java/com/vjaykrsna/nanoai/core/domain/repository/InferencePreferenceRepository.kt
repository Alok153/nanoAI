package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.model.InferencePreference
import com.vjaykrsna.nanoai.core.model.InferenceMode
import kotlinx.coroutines.flow.Flow

/** Repository exposing the persisted inference routing preference. */
interface InferencePreferenceRepository {
  /** Observe changes to the user's inference preference. */
  fun observeInferencePreference(): Flow<InferencePreference>

  /** Persist a new inference routing mode. */
  suspend fun setInferenceMode(mode: InferenceMode)
}
