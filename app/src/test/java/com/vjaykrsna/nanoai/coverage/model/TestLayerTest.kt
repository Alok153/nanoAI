package com.vjaykrsna.nanoai.coverage.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TestLayerTest {

  @Test
  fun `machineName is camelCase`() {
    assertThat(TestLayer.VIEW_MODEL.machineName).isEqualTo("viewModel")
    assertThat(TestLayer.UI.machineName).isEqualTo("ui")
    assertThat(TestLayer.DATA.machineName).isEqualTo("data")
  }

  @Test
  fun `analyticsKey is kebab-case`() {
    assertThat(TestLayer.VIEW_MODEL.analyticsKey).isEqualTo("view-model")
    assertThat(TestLayer.UI.analyticsKey).isEqualTo("ui")
    assertThat(TestLayer.DATA.analyticsKey).isEqualTo("data")
  }
}
