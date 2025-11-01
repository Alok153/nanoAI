package com.vjaykrsna.nanoai.core.coverage.model

import com.google.common.truth.Truth.assertThat
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredFunctions
import org.junit.jupiter.api.Test

class TestLayerTest {

  @Test
  fun `machineName is camelCase`() {
    assertThat(TestLayer.VIEW_MODEL.machineName).isEqualTo("ViewModel")
    assertThat(TestLayer.UI.machineName).isEqualTo("UI")
    assertThat(TestLayer.DATA.machineName).isEqualTo("Data")
  }

  @Test
  fun `analyticsKey is kebab-case`() {
    assertThat(TestLayer.VIEW_MODEL.analyticsKey).isEqualTo("view-model")
    assertThat(TestLayer.UI.analyticsKey).isEqualTo("ui")
    assertThat(TestLayer.DATA.analyticsKey).isEqualTo("data")
  }

  @Test
  fun `analytics keys normalize to enum values`() {
    val companion = TestLayer::class.companionObject
    val instance = TestLayer::class.companionObjectInstance
    check(companion != null && instance != null) { "TestLayer companion object missing" }

    val method =
      companion.declaredFunctions.firstOrNull { it.name == "fromAnalyticsKey" }
        ?: error("Expected TestLayer.fromAnalyticsKey helper to exist")

    val normalized = method.call(instance, "View-Model") as? TestLayer

    assertThat(normalized).isEqualTo(TestLayer.VIEW_MODEL)
  }
}
