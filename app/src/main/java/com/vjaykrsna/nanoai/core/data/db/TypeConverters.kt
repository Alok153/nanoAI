package com.vjaykrsna.nanoai.core.data.db

import androidx.room.TypeConverter
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters for custom types used in database entities. Handles conversion between Kotlin
 * types and SQLite-compatible types.
 */
class TypeConverters {
  /**
   * Convert Instant to Long (epoch milliseconds) for storage. Note: UUIDs are stored as String
   * directly by Room, no conversion needed.
   */
  @TypeConverter fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

  @TypeConverter
  fun toInstant(epochMillis: Long?): Instant? =
    epochMillis?.let { Instant.fromEpochMilliseconds(it) }

  /** Convert Set<String> to comma-separated string for storage. Used for model capabilities. */
  @TypeConverter fun fromStringSet(value: Set<String>?): String? = value?.joinToString(",")

  @TypeConverter
  fun toStringSet(value: String?): Set<String>? =
    value?.split(",")?.filter { it.isNotBlank() }?.toSet()

  @TypeConverter
  fun fromThemePreference(themePreference: ThemePreference?): String? = themePreference?.name

  @TypeConverter
  fun toThemePreference(name: String?): ThemePreference? =
    name?.let { ThemePreference.fromName(it) }

  @TypeConverter fun fromVisualDensity(visualDensity: VisualDensity?): String? = visualDensity?.name

  @TypeConverter
  fun toVisualDensity(name: String?): VisualDensity? =
    name?.let { candidate ->
      VisualDensity.values().firstOrNull { it.name.equals(candidate, ignoreCase = true) }
    } ?: VisualDensity.DEFAULT

  @TypeConverter fun fromScreenType(screenType: ScreenType?): String? = screenType?.name

  @TypeConverter
  fun toScreenType(name: String?): ScreenType? =
    name?.let { candidate ->
      ScreenType.values().firstOrNull { it.name.equals(candidate, ignoreCase = true) }
    } ?: ScreenType.UNKNOWN

  @TypeConverter
  fun fromStringList(values: List<String>?): String? =
    values?.let { nonNullValues -> json.encodeToString(stringListSerializer, nonNullValues) }

  @TypeConverter
  fun toStringList(value: String?): List<String> =
    value
      ?.takeIf { it.isNotBlank() }
      ?.let { payload -> json.decodeFromString(stringListSerializer, payload) } ?: emptyList()

  @TypeConverter
  fun fromStringBooleanMap(values: Map<String, Boolean>?): String? =
    values?.let { nonNullValues -> json.encodeToString(stringBooleanMapSerializer, nonNullValues) }

  @TypeConverter
  fun toStringBooleanMap(value: String?): Map<String, Boolean> =
    value
      ?.takeIf { it.isNotBlank() }
      ?.let { payload -> json.decodeFromString(stringBooleanMapSerializer, payload) } ?: emptyMap()

  private companion object {
    val json: Json = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
      explicitNulls = false
    }
    val stringListSerializer = ListSerializer(String.serializer())
    val stringBooleanMapSerializer = MapSerializer(String.serializer(), Boolean.serializer())
  }
}
