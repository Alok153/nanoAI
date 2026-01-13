package com.vjaykrsna.nanoai.core.data.db

import androidx.room.TypeConverter
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object TemporalTypeConverters {
  @TypeConverter fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

  @TypeConverter
  fun toInstant(epochMillis: Long?): Instant? =
    epochMillis?.let { Instant.fromEpochMilliseconds(it) }
}

object CollectionTypeConverters {
  @TypeConverter fun fromStringSet(value: Set<String>?): String? = value?.joinToString(",")

  @TypeConverter
  fun toStringSet(value: String?): Set<String>? =
    value?.split(",")?.filter { it.isNotBlank() }?.toSet()

  @TypeConverter
  fun fromStringList(values: List<String>?): String? =
    values?.let { json.encodeToString(stringListSerializer, it) }

  @TypeConverter
  fun toStringList(value: String?): List<String> =
    value?.takeIf { it.isNotBlank() }?.let { json.decodeFromString(stringListSerializer, it) }
      ?: emptyList()

  @TypeConverter
  fun fromStringBooleanMap(values: Map<String, Boolean>?): String? =
    values?.let { json.encodeToString(stringBooleanMapSerializer, it) }

  @TypeConverter
  fun toStringBooleanMap(value: String?): Map<String, Boolean> =
    value?.takeIf { it.isNotBlank() }?.let { json.decodeFromString(stringBooleanMapSerializer, it) }
      ?: emptyMap()
}

object UiPreferenceTypeConverters {
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
}

object DeliveryTypeConverters {
  @TypeConverter fun fromDeliveryType(deliveryType: DeliveryType?): String? = deliveryType?.name

  @TypeConverter
  fun toDeliveryType(name: String?): DeliveryType? =
    name?.let { enumValueOfOrNull<DeliveryType>(it) }
}

private val json: Json = Json {
  ignoreUnknownKeys = true
  encodeDefaults = true
  explicitNulls = false
}
private val stringListSerializer = ListSerializer(String.serializer())
private val stringBooleanMapSerializer = MapSerializer(String.serializer(), Boolean.serializer())

private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? =
  runCatching { enumValueOf<T>(name) }.getOrNull()
