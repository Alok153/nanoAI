package com.vjaykrsna.nanoai.coverage.domain

import com.vjaykrsna.nanoai.coverage.model.RiskRegisterItem
import com.vjaykrsna.nanoai.coverage.model.TestSuiteCatalogEntry
import java.time.Instant

/**
 * Utility for correlating risk register entries with automated test suites and highlighting
 * lingering critical issues.
 */
class RiskRegisterCoordinator(
  private val risks: List<RiskRegisterItem>,
  private val catalog: List<TestSuiteCatalogEntry>,
) {
  private val mitigationsByRiskId: Map<String, List<TestSuiteCatalogEntry>> =
    catalog
      .flatMap { entry -> entry.riskTags.map { tag -> tag to entry } }
      .groupBy({ it.first }, { it.second })

  fun mitigationsFor(riskId: String): List<TestSuiteCatalogEntry> =
    mitigationsByRiskId[riskId].orEmpty()

  fun unmitigatedCriticalRisks(): List<RiskRegisterItem> =
    risks.filter { risk ->
      risk.severity == RiskRegisterItem.Severity.CRITICAL &&
        risk.status != RiskRegisterItem.Status.RESOLVED &&
        mitigationsFor(risk.riskId).isEmpty()
    }

  fun requiresAttention(now: Instant): Boolean =
    risks.any { risk ->
      risk.isActionable(now) && mitigationsFor(risk.riskId).isEmpty() && shouldEscalate(risk, now)
    }

  private fun shouldEscalate(risk: RiskRegisterItem, now: Instant): Boolean {
    val deadline = risk.targetBuildDeadline()
    return deadline?.let { !it.isAfter(now) } ?: true
  }
}
