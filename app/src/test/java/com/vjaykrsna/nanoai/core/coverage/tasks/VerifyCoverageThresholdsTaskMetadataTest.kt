package com.vjaykrsna.nanoai.core.coverage.tasks

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.coverage.model.TestLayer
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import org.junit.After
import org.junit.Before
import org.junit.Test

class VerifyCoverageThresholdsTaskMetadataTest {
  private lateinit var metadataFile: java.nio.file.Path

  @Before
  fun setUp() {
    metadataFile = createTempFile(suffix = ".json")
  }

  @After
  fun tearDown() {
    metadataFile.deleteIfExists()
  }

  @Test
  fun `parses threshold overrides from metadata`() {
    metadataFile.writeText(
      """
      {
        "metrics": [
          {"layer": "UI", "minimumPercent": 72},
          {"layer": "VIEW_MODEL", "minimumPercent": 80}
        ]
      }
      """
        .trimIndent()
    )

    val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataFile)

    assertThat(overrides.getValue(TestLayer.UI)).isEqualTo(72.0)
    assertThat(overrides.getValue(TestLayer.VIEW_MODEL)).isEqualTo(80.0)
    assertThat(overrides.getValue(TestLayer.DATA)).isEqualTo(70.0)
  }

  @Test
  fun `falls back to defaults when metadata missing`() {
    // Write malformed content to trigger fallback
    metadataFile.writeText("{}")

    val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataFile)

    assertThat(overrides.getValue(TestLayer.UI)).isEqualTo(65.0)
    assertThat(overrides.getValue(TestLayer.VIEW_MODEL)).isEqualTo(75.0)
    assertThat(overrides.getValue(TestLayer.DATA)).isEqualTo(70.0)
  }
}
