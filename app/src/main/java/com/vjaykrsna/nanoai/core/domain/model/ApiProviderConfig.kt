package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import kotlinx.datetime.Instant

/**
 * Domain model for API provider configuration.
 *
 * Clean architecture: Separate from database entities. Used by repositories, use cases, ViewModels,
 * and UI. Mapping to/from entities is handled by
 * [com.vjaykrsna.nanoai.core.data.db.mappers.ApiProviderConfigMapper].
 */
data class ApiProviderConfig(
  val providerId: String,
  val providerName: String,
  val baseUrl: String,
  val apiType: APIType,
  val isEnabled: Boolean = true,
  val quotaResetAt: Instant? = null,
  val lastStatus: ProviderStatus = ProviderStatus.UNKNOWN,
  val credentialId: String? = null,
) {
  val hasCredential: Boolean
    get() = !credentialId.isNullOrBlank()
}

typealias APIProviderConfig = ApiProviderConfig
