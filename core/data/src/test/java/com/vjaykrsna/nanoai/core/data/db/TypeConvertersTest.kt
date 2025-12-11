package com.vjaykrsna.nanoai.core.data.db

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.maintenance.model.MaintenanceCategory
import com.vjaykrsna.nanoai.core.maintenance.model.MaintenanceStatus
import com.vjaykrsna.nanoai.core.maintenance.model.PriorityLevel
import com.vjaykrsna.nanoai.core.maintenance.model.SeverityLevel
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TypeConvertersTest {

  @Nested
  inner class TemporalTypeConvertersTest {
    @Test
    fun `fromInstant converts instant to epoch millis`() {
      val instant = Instant.parse("2024-01-15T10:30:00Z")
      val result = TemporalTypeConverters.fromInstant(instant)
      assertThat(result).isEqualTo(instant.toEpochMilliseconds())
    }

    @Test
    fun `fromInstant handles null`() {
      val result = TemporalTypeConverters.fromInstant(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toInstant converts epoch millis to instant`() {
      val epochMillis = 1705315800000L
      val result = TemporalTypeConverters.toInstant(epochMillis)
      assertThat(result).isEqualTo(Instant.fromEpochMilliseconds(epochMillis))
    }

    @Test
    fun `toInstant handles null`() {
      val result = TemporalTypeConverters.toInstant(null)
      assertThat(result).isNull()
    }

    @Test
    fun `round trip preserves instant value`() {
      val original = Instant.parse("2024-06-15T14:30:00Z")
      val millis = TemporalTypeConverters.fromInstant(original)
      val roundTrip = TemporalTypeConverters.toInstant(millis)
      assertThat(roundTrip).isEqualTo(original)
    }
  }

  @Nested
  inner class CollectionTypeConvertersTest {
    @Test
    fun `fromStringSet converts set to comma-separated string`() {
      val set = setOf("a", "b", "c")
      val result = CollectionTypeConverters.fromStringSet(set)
      assertThat(result).isEqualTo("a,b,c")
    }

    @Test
    fun `fromStringSet handles null`() {
      val result = CollectionTypeConverters.fromStringSet(null)
      assertThat(result).isNull()
    }

    @Test
    fun `fromStringSet handles empty set`() {
      val result = CollectionTypeConverters.fromStringSet(emptySet())
      assertThat(result).isEmpty()
    }

    @Test
    fun `toStringSet converts comma-separated string to set`() {
      val result = CollectionTypeConverters.toStringSet("x,y,z")
      assertThat(result).containsExactly("x", "y", "z")
    }

    @Test
    fun `toStringSet handles null`() {
      val result = CollectionTypeConverters.toStringSet(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toStringSet filters blank entries`() {
      val result = CollectionTypeConverters.toStringSet("a,,b,c")
      // Empty strings from consecutive commas are filtered, but " " is not blank after split
      assertThat(result).containsExactly("a", "b", "c")
    }

    @Test
    fun `fromStringList converts list to JSON`() {
      val list = listOf("item1", "item2", "item3")
      val result = CollectionTypeConverters.fromStringList(list)
      assertThat(result).contains("item1")
      assertThat(result).contains("item2")
      assertThat(result).contains("item3")
    }

    @Test
    fun `fromStringList handles null`() {
      val result = CollectionTypeConverters.fromStringList(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toStringList converts JSON to list`() {
      val json = """["a","b","c"]"""
      val result = CollectionTypeConverters.toStringList(json)
      assertThat(result).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun `toStringList handles null`() {
      val result = CollectionTypeConverters.toStringList(null)
      assertThat(result).isEmpty()
    }

    @Test
    fun `toStringList handles blank string`() {
      val result = CollectionTypeConverters.toStringList("")
      assertThat(result).isEmpty()
    }

    @Test
    fun `fromStringBooleanMap converts map to JSON`() {
      val map = mapOf("key1" to true, "key2" to false)
      val result = CollectionTypeConverters.fromStringBooleanMap(map)
      assertThat(result).contains("key1")
      assertThat(result).contains("key2")
    }

    @Test
    fun `fromStringBooleanMap handles null`() {
      val result = CollectionTypeConverters.fromStringBooleanMap(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toStringBooleanMap converts JSON to map`() {
      val json = """{"enabled":true,"disabled":false}"""
      val result = CollectionTypeConverters.toStringBooleanMap(json)
      assertThat(result["enabled"]).isTrue()
      assertThat(result["disabled"]).isFalse()
    }

    @Test
    fun `toStringBooleanMap handles null`() {
      val result = CollectionTypeConverters.toStringBooleanMap(null)
      assertThat(result).isEmpty()
    }

    @Test
    fun `toStringBooleanMap handles blank string`() {
      val result = CollectionTypeConverters.toStringBooleanMap("")
      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class UiPreferenceTypeConvertersTest {
    @Test
    fun `fromThemePreference converts to name`() {
      val result = UiPreferenceTypeConverters.fromThemePreference(ThemePreference.DARK)
      assertThat(result).isEqualTo("DARK")
    }

    @Test
    fun `fromThemePreference handles null`() {
      val result = UiPreferenceTypeConverters.fromThemePreference(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toThemePreference converts name to enum`() {
      val result = UiPreferenceTypeConverters.toThemePreference("LIGHT")
      assertThat(result).isEqualTo(ThemePreference.LIGHT)
    }

    @Test
    fun `toThemePreference handles null`() {
      val result = UiPreferenceTypeConverters.toThemePreference(null)
      assertThat(result).isNull()
    }

    @Test
    fun `fromVisualDensity converts to name`() {
      val result = UiPreferenceTypeConverters.fromVisualDensity(VisualDensity.COMPACT)
      assertThat(result).isEqualTo("COMPACT")
    }

    @Test
    fun `fromVisualDensity handles null`() {
      val result = UiPreferenceTypeConverters.fromVisualDensity(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toVisualDensity converts name to enum`() {
      val result = UiPreferenceTypeConverters.toVisualDensity("COMPACT")
      assertThat(result).isEqualTo(VisualDensity.COMPACT)
    }

    @Test
    fun `toVisualDensity handles null with default`() {
      val result = UiPreferenceTypeConverters.toVisualDensity(null)
      assertThat(result).isEqualTo(VisualDensity.DEFAULT)
    }

    @Test
    fun `toVisualDensity is case insensitive`() {
      val result = UiPreferenceTypeConverters.toVisualDensity("compact")
      assertThat(result).isEqualTo(VisualDensity.COMPACT)
    }

    @Test
    fun `fromScreenType converts to name`() {
      val result = UiPreferenceTypeConverters.fromScreenType(ScreenType.HOME)
      assertThat(result).isEqualTo("HOME")
    }

    @Test
    fun `fromScreenType handles null`() {
      val result = UiPreferenceTypeConverters.fromScreenType(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toScreenType converts name to enum`() {
      val result = UiPreferenceTypeConverters.toScreenType("SETTINGS")
      assertThat(result).isEqualTo(ScreenType.SETTINGS)
    }

    @Test
    fun `toScreenType handles null with default`() {
      val result = UiPreferenceTypeConverters.toScreenType(null)
      assertThat(result).isEqualTo(ScreenType.UNKNOWN)
    }

    @Test
    fun `toScreenType is case insensitive`() {
      val result = UiPreferenceTypeConverters.toScreenType("home")
      assertThat(result).isEqualTo(ScreenType.HOME)
    }
  }

  @Nested
  inner class MaintenanceTypeConvertersTest {
    @Test
    fun `fromMaintenanceCategory converts to name`() {
      val result =
        MaintenanceTypeConverters.fromMaintenanceCategory(MaintenanceCategory.STATIC_ANALYSIS)
      assertThat(result).isEqualTo("STATIC_ANALYSIS")
    }

    @Test
    fun `fromMaintenanceCategory handles null`() {
      val result = MaintenanceTypeConverters.fromMaintenanceCategory(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toMaintenanceCategory converts name to enum`() {
      val result = MaintenanceTypeConverters.toMaintenanceCategory("SECURITY")
      assertThat(result).isEqualTo(MaintenanceCategory.SECURITY)
    }

    @Test
    fun `toMaintenanceCategory handles null`() {
      val result = MaintenanceTypeConverters.toMaintenanceCategory(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toMaintenanceCategory handles invalid name`() {
      val result = MaintenanceTypeConverters.toMaintenanceCategory("INVALID")
      assertThat(result).isNull()
    }

    @Test
    fun `fromPriorityLevel converts to name`() {
      val result = MaintenanceTypeConverters.fromPriorityLevel(PriorityLevel.HIGH)
      assertThat(result).isEqualTo("HIGH")
    }

    @Test
    fun `toPriorityLevel converts name to enum`() {
      val result = MaintenanceTypeConverters.toPriorityLevel("MEDIUM")
      assertThat(result).isEqualTo(PriorityLevel.MEDIUM)
    }

    @Test
    fun `fromMaintenanceStatus converts to name`() {
      val result = MaintenanceTypeConverters.fromMaintenanceStatus(MaintenanceStatus.IN_PROGRESS)
      assertThat(result).isEqualTo("IN_PROGRESS")
    }

    @Test
    fun `toMaintenanceStatus converts name to enum`() {
      val result = MaintenanceTypeConverters.toMaintenanceStatus("IN_PROGRESS")
      assertThat(result).isEqualTo(MaintenanceStatus.IN_PROGRESS)
    }

    @Test
    fun `fromSeverityLevel converts to name`() {
      val result = MaintenanceTypeConverters.fromSeverityLevel(SeverityLevel.ERROR)
      assertThat(result).isEqualTo("ERROR")
    }

    @Test
    fun `toSeverityLevel converts name to enum`() {
      val result = MaintenanceTypeConverters.toSeverityLevel("WARNING")
      assertThat(result).isEqualTo(SeverityLevel.WARNING)
    }
  }

  @Nested
  inner class DeliveryTypeConvertersTest {
    @Test
    fun `fromDeliveryType converts to name`() {
      val result = DeliveryTypeConverters.fromDeliveryType(DeliveryType.LOCAL_ARCHIVE)
      assertThat(result).isEqualTo("LOCAL_ARCHIVE")
    }

    @Test
    fun `fromDeliveryType handles null`() {
      val result = DeliveryTypeConverters.fromDeliveryType(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toDeliveryType converts name to enum`() {
      val result = DeliveryTypeConverters.toDeliveryType("PLAY_ASSET")
      assertThat(result).isEqualTo(DeliveryType.PLAY_ASSET)
    }

    @Test
    fun `toDeliveryType handles null`() {
      val result = DeliveryTypeConverters.toDeliveryType(null)
      assertThat(result).isNull()
    }

    @Test
    fun `toDeliveryType handles invalid name`() {
      val result = DeliveryTypeConverters.toDeliveryType("INVALID_TYPE")
      assertThat(result).isNull()
    }
  }
}
