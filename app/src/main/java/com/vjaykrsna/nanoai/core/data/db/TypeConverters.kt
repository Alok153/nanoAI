package com.vjaykrsna.nanoai.core.data.db

import androidx.room.TypeConverter
import kotlinx.datetime.Instant

/**
 * Room TypeConverters for custom types used in database entities.
 * Handles conversion between Kotlin types and SQLite-compatible types.
 */
class TypeConverters {

    /**
     * Convert Instant to Long (epoch milliseconds) for storage.
     * Note: UUIDs are stored as String directly by Room, no conversion needed.
     */
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }

    @TypeConverter
    fun toInstant(epochMillis: Long?): Instant? {
        return epochMillis?.let { Instant.fromEpochMilliseconds(it) }
    }

    /**
     * Convert Set<String> to comma-separated string for storage.
     * Used for model capabilities.
     */
    @TypeConverter
    fun fromStringSet(value: Set<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringSet(value: String?): Set<String>? {
        return value?.split(",")?.filter { it.isNotBlank() }?.toSet()
    }
}
