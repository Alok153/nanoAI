package com.vjaykrsna.nanoai.core.data.preferences

import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Helper responsible for encoding and decoding complex preference payloads using Kotlin
 * Serialization.
 */
@Singleton
class UiPreferencesConverters(
  private val json: Json,
) {
  private val stringListSerializer = ListSerializer(String.serializer())
  private val stringBooleanMapSerializer = MapSerializer(String.serializer(), Boolean.serializer())

  fun encodeStringList(values: List<String>): String =
    json.encodeToString(stringListSerializer, values)

  fun decodeStringList(payload: String?): List<String> =
    payload?.takeIf { it.isNotBlank() }?.let { json.decodeFromString(stringListSerializer, it) }
      ?: emptyList()

  fun encodeBooleanMap(values: Map<String, Boolean>): String =
    json.encodeToString(stringBooleanMapSerializer, values)

  fun decodeBooleanMap(payload: String?): Map<String, Boolean> =
    payload
      ?.takeIf { it.isNotBlank() }
      ?.let { json.decodeFromString(stringBooleanMapSerializer, it) } ?: emptyMap()
}
