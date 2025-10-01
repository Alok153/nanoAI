package com.vjaykrsna.nanoai.core.data.repository

import com.vjaykrsna.nanoai.core.domain.model.ApiProviderConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository for API provider configuration management.
 *
 * Manages cloud API providers for fallback when local models are unavailable.
 */
interface ApiProviderConfigRepository {

    /** Get all API providers. */
    suspend fun getAllProviders(): List<ApiProviderConfig>

    /** Get a specific provider by ID. */
    suspend fun getProvider(providerId: String): ApiProviderConfig?

    /** Add a new provider configuration. */
    suspend fun addProvider(config: ApiProviderConfig)

    /** Update an existing provider configuration. */
    suspend fun updateProvider(config: ApiProviderConfig)

    /** Delete a provider configuration. */
    suspend fun deleteProvider(providerId: String)

    /** Get all enabled providers. */
    suspend fun getEnabledProviders(): List<ApiProviderConfig>

    /** Observe all providers (reactive updates). */
    fun observeAllProviders(): Flow<List<ApiProviderConfig>>
}
