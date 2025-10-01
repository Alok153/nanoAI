package com.vjaykrsna.nanoai.core.data.repository

import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository for API provider configuration management.
 *
 * Manages cloud API providers for fallback when local models are unavailable.
 */
interface ApiProviderConfigRepository {
    /** Get all API providers. */
    suspend fun getAllProviders(): List<APIProviderConfig>

    /** Get a specific provider by ID. */
    suspend fun getProvider(providerId: String): APIProviderConfig?

    /** Add a new provider configuration. */
    suspend fun addProvider(config: APIProviderConfig)

    /** Update an existing provider configuration. */
    suspend fun updateProvider(config: APIProviderConfig)

    /** Delete a provider configuration. */
    suspend fun deleteProvider(providerId: String)

    /** Get all enabled providers. */
    suspend fun getEnabledProviders(): List<APIProviderConfig>

    /** Observe all providers (reactive updates). */
    fun observeAllProviders(): Flow<List<APIProviderConfig>>
}
