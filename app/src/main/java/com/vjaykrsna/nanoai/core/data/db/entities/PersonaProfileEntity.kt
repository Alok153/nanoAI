package com.vjaykrsna.nanoai.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Room entity representing a persona profile configuration.
 *
 * Personas define different AI assistant personalities with custom system prompts, model
 * preferences, and generation parameters (temperature, topP).
 *
 * @property personaId Unique identifier (UUID string)
 * @property name Display name of the persona
 * @property description Brief description of persona's purpose/style
 * @property systemPrompt Full system instruction text
 * @property defaultModelPreference Preferred model ID (null = use app default)
 * @property temperature Sampling temperature (0.0-2.0, default 1.0)
 * @property topP Nucleus sampling parameter (0.0-1.0, default 1.0)
 * @property defaultVoice Reserved for future audio TTS voice selection
 * @property defaultImageStyle Reserved for future image generation style
 * @property createdAt Timestamp when persona was created
 * @property updatedAt Timestamp when persona was last modified
 */
@Entity(tableName = "persona_profiles")
data class PersonaProfileEntity(
  @PrimaryKey @ColumnInfo(name = "persona_id") val personaId: String,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "description") val description: String,
  @ColumnInfo(name = "system_prompt") val systemPrompt: String,
  @ColumnInfo(name = "default_model_preference") val defaultModelPreference: String? = null,
  @ColumnInfo(name = "temperature") val temperature: Float = 1.0f,
  @ColumnInfo(name = "top_p") val topP: Float = 1.0f,
  @ColumnInfo(name = "default_voice") val defaultVoice: String? = null,
  @ColumnInfo(name = "default_image_style") val defaultImageStyle: String? = null,
  @ColumnInfo(name = "created_at") val createdAt: Instant,
  @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
