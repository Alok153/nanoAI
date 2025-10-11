package com.vjaykrsna.nanoai.coverage.model

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
}
