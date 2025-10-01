package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.data.db.entities.ApiProviderConfigEntity
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import kotlinx.datetime.Instant

/**
 * Domain model for API provider configuration.
 * 
 * Clean architecture: Separate from database entities.
 * Used by repositories, use cases, ViewModels, and UI.
 */
data class ApiProviderConfig(
    val providerId: String,
    val providerName: String,
    val baseUrl: String,
    val apiKey: String,
    val apiType: APIType,
    val isEnabled: Boolean = true,
    val quotaResetAt: Instant? = null,
    val lastStatus: ProviderStatus = ProviderStatus.UNKNOWN
)

/**
 * Extension function to convert entity to domain model.
 */
fun ApiProviderConfigEntity.toDomain(): ApiProviderConfig = ApiProviderConfig(
    providerId = providerId,
    providerName = providerName,
    baseUrl = baseUrl,
    apiKey = apiKey,
    apiType = apiType,
    isEnabled = isEnabled,
    quotaResetAt = quotaResetAt,
    lastStatus = lastStatus
)

/**
 * Extension function to convert domain model to entity.
 */
fun ApiProviderConfig.toEntity(): ApiProviderConfigEntity = ApiProviderConfigEntity(
    providerId = providerId,
    providerName = providerName,
    baseUrl = baseUrl,
    apiKey = apiKey,
    apiType = apiType,
    isEnabled = isEnabled,
    quotaResetAt = quotaResetAt,
    lastStatus = lastStatus
)
