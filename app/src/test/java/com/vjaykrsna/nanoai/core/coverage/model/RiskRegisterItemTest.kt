package com.vjaykrsna.nanoai.core.coverage.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RiskRegisterItemTest {

  @Test
  fun `isActionable returns true for overdue high severity risk`() {
    val risk =
      RiskRegisterItem(
        riskId = "risk-high-ui",
        layer = TestLayer.UI,
        description = "Compose semantics missing",
        severity = RiskRegisterItem.Severity.HIGH,
        targetBuild = "build-2025-10-10",
        status = RiskRegisterItem.Status.OPEN,
        mitigation = null,
      )

    val actionable = risk.isActionable(Instant.parse("2025-10-12T00:00:00Z"))

    assertThat(actionable).isTrue()
  }

  @Test
  fun `isActionable ignores resolved risks`() {
    val risk =
      RiskRegisterItem(
        riskId = "risk-medium-data",
        layer = TestLayer.DATA,
        description = "Room DAO missing index",
        severity = RiskRegisterItem.Severity.HIGH,
        targetBuild = "build-2025-10-15",
        status = RiskRegisterItem.Status.RESOLVED,
        mitigation = "Added DAO test",
      )

    val actionable = risk.isActionable(Instant.parse("2025-10-20T00:00:00Z"))

    assertThat(actionable).isFalse()
  }

  @Test
  fun `critical risk without target build throws`() {
    assertThrows<IllegalArgumentException> {
      RiskRegisterItem(
        riskId = "risk-critical",
        layer = TestLayer.DATA,
        description = "Offline writes failing",
        severity = RiskRegisterItem.Severity.CRITICAL,
        targetBuild = null,
        status = RiskRegisterItem.Status.OPEN,
        mitigation = null,
      )
    }
  }

  @Test
  fun `medium severity risks remain non actionable even when overdue`() {
    val risk =
      RiskRegisterItem(
        riskId = "risk-medium",
        layer = TestLayer.DATA,
        description = "Deferred cleanup",
        severity = RiskRegisterItem.Severity.MEDIUM,
        targetBuild = "build-2025-10-01",
        status = RiskRegisterItem.Status.OPEN,
        mitigation = "  refine docs  ",
      )

    val actionable = risk.isActionable(Instant.parse("2025-10-12T00:00:00Z"))

    assertThat(actionable).isFalse()
  }

  @Test
  fun `risk register items expose actionable deadline helper`() {
    val methods = RiskRegisterItem::class.java.methods.map { it.name }

    assertThat(methods).contains("targetBuildDeadline")
  }

  @Test
  fun `mitigation formatting helper trims and capitalizes description`() {
    val risk =
      RiskRegisterItem(
        riskId = "risk-format",
        layer = TestLayer.UI,
        description = "Compose semantics",
        severity = RiskRegisterItem.Severity.HIGH,
        targetBuild = "build-2025-10-15",
        status = RiskRegisterItem.Status.OPEN,
        mitigation = "  add talkback copy  ",
      )

    val method =
      RiskRegisterItem::class.java.methods.firstOrNull { it.name == "getFormattedMitigation" }
        ?: error("Expected formattedMitigation helper to exist for RiskRegisterItem")
    val formatted = method.invoke(risk) as? String

    assertThat(formatted).isEqualTo("Add talkback copy")
  }
}
