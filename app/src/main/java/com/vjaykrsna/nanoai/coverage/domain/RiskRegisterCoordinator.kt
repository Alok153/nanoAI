package com.vjaykrsna.nanoai.coverage.domain

import com.vjaykrsna.nanoai.coverage.model.RiskRegisterItem
import com.vjaykrsna.nanoai.coverage.model.TestSuiteCatalogEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.WeekFields

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
      .flatMap { entry ->
        entry.riskTags.mapNotNull { tag ->
          normalizeRiskKey(tag).takeIf { it.isNotEmpty() }?.let { key -> key to entry }
        }
      }
      .groupBy({ it.first }, { it.second })
      .mapValues { (_, entries) -> entries.distinctBy { it.suiteId } }

  fun mitigationsFor(riskId: String): List<TestSuiteCatalogEntry> =
    normalizeRiskKey(riskId)
      .takeIf { it.isNotEmpty() }
      ?.let { key -> mitigationsByRiskId[key] }
      .orEmpty()

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
    val deadline = risk.targetBuildDeadline() ?: risk.targetBuild?.let(::parseReleaseStyleTarget)
    return deadline?.let { !it.isAfter(now) } ?: true
  }

  private fun parseReleaseStyleTarget(target: String): Instant? {
    val match = RELEASE_STYLE_REGEX.matchEntire(target.trim()) ?: return null
    return buildReleaseInstant(match)
  }

  private fun buildReleaseInstant(match: MatchResult): Instant? {
    val year = match.groupValues[YEAR_GROUP_INDEX].toIntOrNull()
    val releaseNumber = match.groupValues[RELEASE_GROUP_INDEX].toIntOrNull()
    return when {
      year == null || releaseNumber == null -> null
      releaseNumber !in RELEASE_MIN_WEEK..RELEASE_MAX_WEEK -> null
      else -> releaseStyleStartInstant(year, releaseNumber)
    }
  }

  private fun releaseStyleStartInstant(year: Int, releaseNumber: Int): Instant? {
    val weekFields = WeekFields.ISO
    return runCatching {
        LocalDate.of(year, ISO_WEEK_BASE_MONTH, ISO_WEEK_BASE_DAY)
          .with(weekFields.weekOfYear(), releaseNumber.toLong())
          .with(weekFields.dayOfWeek(), ISO_WEEK_START_DAY.toLong())
          .atStartOfDay()
          .toInstant(ZoneOffset.UTC)
      }
      .getOrNull()
  }

  private fun normalizeRiskKey(value: String): String =
    value
      .trim()
      .lowercase()
      .replace('_', '-')
      .replace(Regex("[^a-z0-9-]+"), "-")
      .replace(Regex("-+"), "-")
      .trim('-')

  private companion object {
    private val RELEASE_STYLE_REGEX = Regex("^r(\\d{4})\\.(\\d{1,2})$", RegexOption.IGNORE_CASE)
    private const val YEAR_GROUP_INDEX = 1
    private const val RELEASE_GROUP_INDEX = 2
    private const val ISO_WEEK_BASE_MONTH = 1
    private const val ISO_WEEK_BASE_DAY = 4
    private const val ISO_WEEK_START_DAY = 1
    private const val RELEASE_MIN_WEEK = 1
    private const val RELEASE_MAX_WEEK = 53
  }
}
