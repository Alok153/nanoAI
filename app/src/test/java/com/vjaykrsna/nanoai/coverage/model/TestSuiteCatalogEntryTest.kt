package com.vjaykrsna.nanoai.coverage.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestSuiteCatalogEntryTest {

  @Test
  fun `mitigatesRisk returns true when tag matches`() {
    val entry =
      TestSuiteCatalogEntry(
        suiteId = "suite-coverage",
        owner = "quality-engineering",
        layer = TestLayer.DATA,
        journey = "Offline cache",
        coverageContribution = 4.0,
        riskTags = setOf("risk-offline", "risk-critical-data"),
      )

    assertThat(entry.mitigatesRisk("risk-critical-data")).isTrue()
  }

  @Test
  fun `mitigatesRisk returns false when tag missing`() {
    val entry =
      TestSuiteCatalogEntry(
        suiteId = "suite-coverage",
        owner = "quality-engineering",
        layer = TestLayer.DATA,
        journey = "Offline cache",
        coverageContribution = 4.0,
        riskTags = setOf("risk-offline"),
      )

    assertThat(entry.mitigatesRisk("risk-critical-data")).isFalse()
  }

  @Test
  fun `owner must not be blank`() {
    assertThrows<IllegalArgumentException> {
      TestSuiteCatalogEntry(
        suiteId = "suite-coverage",
        owner = " ",
        layer = TestLayer.DATA,
        journey = "Offline cache",
        coverageContribution = 4.0,
        riskTags = emptySet(),
      )
    }
  }

  @Test
  fun `mitigatesRisk matches tags case insensitively`() {
    val entry =
      TestSuiteCatalogEntry(
        suiteId = "suite-coverage",
        owner = "quality-engineering",
        layer = TestLayer.DATA,
        journey = "Offline cache",
        coverageContribution = 4.0,
        riskTags = setOf("RISK-OFFLINE"),
      )

    assertThat(entry.mitigatesRisk("risk-offline")).isTrue()
  }

  @Test
  fun `negative coverage contribution throws`() {
    assertThrows<IllegalArgumentException> {
      TestSuiteCatalogEntry(
        suiteId = "suite-negative",
        owner = "quality-engineering",
        layer = TestLayer.DATA,
        journey = "Offline cache",
        coverageContribution = -1.0,
        riskTags = setOf("risk-offline"),
      )
    }
  }
}
