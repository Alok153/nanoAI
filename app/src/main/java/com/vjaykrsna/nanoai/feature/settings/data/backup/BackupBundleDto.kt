package com.vjaykrsna.nanoai.feature.settings.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BackupBundleDto(
  val personas: List<BackupPersonaDto> = emptyList(),
  val apiProviders: List<BackupApiProviderDto> = emptyList(),
  val settings: BackupSettingsDto? = null,
)

@Serializable
internal data class BackupPersonaDto(
  @SerialName("personaId") val id: String? = null,
  val name: String,
  val description: String? = null,
  val systemPrompt: String,
  val defaultModelPreference: String? = null,
  val temperature: Float? = null,
  val topP: Float? = null,
  val defaultVoice: String? = null,
  val defaultImageStyle: String? = null,
  val createdAt: String? = null,
  val updatedAt: String? = null,
)

@Serializable
internal data class BackupApiProviderDto(
  @SerialName("providerId") val id: String,
  val name: String,
  @SerialName("endpoint") val baseUrl: String,
  val apiKey: String? = null,
  val apiType: String? = null,
  val enabled: Boolean? = null,
)

@Serializable
internal data class BackupSettingsDto(
  val telemetryOptIn: Boolean? = null,
  val exportWarningsDismissed: Boolean? = null,
)
