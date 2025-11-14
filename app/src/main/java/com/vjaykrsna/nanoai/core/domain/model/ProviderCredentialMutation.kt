package com.vjaykrsna.nanoai.core.domain.model

/** Describes how a provider credential should be mutated when persisting changes. */
sealed interface ProviderCredentialMutation {
  /** No credential changes are requested. Existing secrets should remain untouched. */
  data object None : ProviderCredentialMutation

  /** Removes any stored credential associated with the provider. */
  data object Remove : ProviderCredentialMutation

  /** Replaces the stored credential with a new value. */
  data class Replace(val value: String) : ProviderCredentialMutation

  companion object {
    /** Helper that maps raw user input into a mutation request. */
    fun fromInput(value: String?, shouldRemove: Boolean): ProviderCredentialMutation {
      if (shouldRemove) return Remove
      val cleaned = value?.trim()
      return if (cleaned.isNullOrEmpty()) None else Replace(cleaned)
    }
  }
}
