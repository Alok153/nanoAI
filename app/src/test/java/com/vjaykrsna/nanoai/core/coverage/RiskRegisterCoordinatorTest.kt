package com.vjaykrsna.nanoai.core.coverage

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.coverage.domain.RiskRegisterCoordinator
import com.vjaykrsna.nanoai.core.coverage.model.RiskRegisterItem
import com.vjaykrsna.nanoai.core.coverage.model.TestLayer
import com.vjaykrsna.nanoai.core.coverage.model.TestSuiteCatalogEntry
import java.time.Instant
import org.junit.jupiter.api.Test

class RiskRegisterCoordinatorTest {

  @Test
  fun `maps suites to risks using risk tags`() {
    val risks =
      listOf(
        RiskRegisterItem(
          riskId = "risk-high-ui",
          layer = TestLayer.UI,
          description = "TalkBack labels missing for composer",
          severity = RiskRegisterItem.Severity.HIGH,
          targetBuild = "build-110",
          status = RiskRegisterItem.Status.IN_PROGRESS,
          mitigation = "Add Compose semantics assertions",
        )
      )

    val catalog =
      listOf(
        TestSuiteCatalogEntry(
          suiteId = "suite-ui-accessibility",
          owner = "quality-engineering",
          layer = TestLayer.UI,
          journey = "Chat composer semantics",
          coverageContribution = 2.5,
          riskTags = setOf("accessibility", "risk-high-ui"),
        )
      )

    val coordinator = RiskRegisterCoordinator(risks, catalog)

    assertThat(coordinator.mitigationsFor("risk-high-ui").map { it.suiteId })
      .containsExactly("suite-ui-accessibility")
  }

  @Test
  fun `critical risks missing mitigation are flagged`() {
    val risks =
      listOf(
        RiskRegisterItem(
          riskId = "risk-critical-data",
          layer = TestLayer.DATA,
          description = "Offline writes are not validated",
          severity = RiskRegisterItem.Severity.CRITICAL,
          targetBuild = "build-2025-10-20",
          status = RiskRegisterItem.Status.OPEN,
          mitigation = "Add Room DAO coverage",
        ),
        RiskRegisterItem(
          riskId = "risk-critical-ui",
          layer = TestLayer.UI,
          description = "Compose chart not focusable",
          severity = RiskRegisterItem.Severity.CRITICAL,
          targetBuild = "build-2025-10-20",
          status = RiskRegisterItem.Status.RESOLVED,
          mitigation = "Suite suite-ui-accessibility",
        ),
      )

    val catalog =
      listOf(
        TestSuiteCatalogEntry(
          suiteId = "suite-ui-accessibility",
          owner = "quality-engineering",
          layer = TestLayer.UI,
          journey = "Coverage dashboard accessibility",
          coverageContribution = 3.1,
          riskTags = setOf("risk-critical-ui"),
        )
      )

    val coordinator = RiskRegisterCoordinator(risks, catalog)

    val unmitigatedCritical = coordinator.unmitigatedCriticalRisks()

    assertThat(unmitigatedCritical.map { it.riskId }).containsExactly("risk-critical-data")
  }

  @Test
  fun `resolved risks are excluded from attention summary`() {
    val risks =
      listOf(
        RiskRegisterItem(
          riskId = "risk-medium-history",
          layer = TestLayer.DATA,
          description = "History fetch does not cover pagination",
          severity = RiskRegisterItem.Severity.MEDIUM,
          targetBuild = "build-118",
          status = RiskRegisterItem.Status.RESOLVED,
          mitigation = "Suite history-pagination",
        )
      )
    val catalog = emptyList<TestSuiteCatalogEntry>()

    val coordinator = RiskRegisterCoordinator(risks, catalog)

    assertThat(coordinator.requiresAttention(Instant.parse("2025-10-12T00:00:00Z"))).isFalse()
  }

  @Test
  fun `requiresAttention ignores mitigated high severity risks`() {
    val risks =
      listOf(
        RiskRegisterItem(
          riskId = "RR-HIGH-027",
          layer = TestLayer.UI,
          description = "TalkBack labels missing for composer",
          severity = RiskRegisterItem.Severity.HIGH,
          targetBuild = "build-2025-10-10",
          status = RiskRegisterItem.Status.OPEN,
          mitigation = null,
        )
      )

    val catalog =
      listOf(
        TestSuiteCatalogEntry(
          suiteId = "suite-ui-accessibility",
          owner = "quality-engineering",
          layer = TestLayer.UI,
          journey = "Chat composer semantics",
          coverageContribution = 2.5,
          riskTags = setOf("rr-high-027", " accessibility "),
        )
      )

    val coordinator = RiskRegisterCoordinator(risks, catalog)

    val now = Instant.parse("2025-10-15T00:00:00Z")

    assertThat(coordinator.requiresAttention(now)).isFalse()
  }

  @Test
  fun `requiresAttention skips mitigated risks with release style target builds`() {
    val risks =
      listOf(
        RiskRegisterItem(
          riskId = "RR-HIGH-027",
          layer = TestLayer.UI,
          description = "Risk register coordinator escalates mitigated items",
          severity = RiskRegisterItem.Severity.HIGH,
          targetBuild = "r2025.43",
          status = RiskRegisterItem.Status.IN_PROGRESS,
          mitigation = "Suite coverage-dashboard",
        )
      )

    val catalog =
      listOf(
        TestSuiteCatalogEntry(
          suiteId = "suite-risk-register",
          owner = "quality-engineering",
          layer = TestLayer.UI,
          journey = "Risk register coverage",
          coverageContribution = 1.2,
          riskTags = setOf("RR_HIGH_027"),
        )
      )

    val coordinator = RiskRegisterCoordinator(risks, catalog)

    val now = Instant.parse("2025-10-18T00:00:00Z")

    assertThat(coordinator.mitigationsFor("RR-HIGH-027")).isNotEmpty()
    assertThat(coordinator.requiresAttention(now)).isFalse()
  }

  @Test
  fun `mitigationsFor matches tags regardless of case and whitespace`() {
    val risks =
      listOf(
        RiskRegisterItem(
          riskId = "RR-HIGH-041",
          layer = TestLayer.DATA,
          description = "Catalog not cached offline",
          severity = RiskRegisterItem.Severity.HIGH,
          targetBuild = null,
          status = RiskRegisterItem.Status.OPEN,
          mitigation = null,
        )
      )

    val catalog =
      listOf(
        TestSuiteCatalogEntry(
          suiteId = "suite-catalog-offline",
          owner = "quality-engineering",
          layer = TestLayer.DATA,
          journey = "Catalog offline fallback",
          coverageContribution = 1.7,
          riskTags = setOf(" rr-high-041  "),
        )
      )

    val coordinator = RiskRegisterCoordinator(risks, catalog)

    assertThat(coordinator.mitigationsFor("rr-high-041").map { it.suiteId })
      .containsExactly("suite-catalog-offline")
  }

  @Test
  fun `requiresAttention escalates overdue high risks without mitigation`() {
    val risks =
      listOf(
        RiskRegisterItem(
          riskId = "risk-high-offline",
          layer = TestLayer.DATA,
          description = "Offline sync not validated",
          severity = RiskRegisterItem.Severity.HIGH,
          targetBuild = "build-2025-10-01",
          status = RiskRegisterItem.Status.IN_PROGRESS,
          mitigation = null,
        )
      )

    val coordinator = RiskRegisterCoordinator(risks, emptyList())

    val now = Instant.parse("2025-10-12T00:00:00Z")

    assertThat(coordinator.requiresAttention(now)).isTrue()
  }
}
