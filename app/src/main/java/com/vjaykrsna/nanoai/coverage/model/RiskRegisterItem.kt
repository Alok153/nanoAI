package com.vjaykrsna.nanoai.coverage.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Describes a coverage gap and mitigation plan. Linked to suites via tags or explicit references.
 */
data class RiskRegisterItem(
  val riskId: String,
  val layer: TestLayer,
  val description: String,
  val severity: Severity,
  val targetBuild: String?,
  val status: Status,
  val mitigation: String?,
) {
  init {
    require(riskId.isNotBlank()) { "riskId must not be blank" }
    require(description.isNotBlank()) { "description must not be blank" }
    if (severity == Severity.CRITICAL) {
      val build = targetBuild?.takeIf { it.isNotBlank() }
      require(build != null) { "Critical risks must declare a target build for mitigation" }
      requireNotNull(parseTargetBuildInstant(build)) {
        "Critical risks must use target build format build-YYYY-MM-DD"
      }
    }
  }

  fun isActionable(now: Instant): Boolean {
    if (!status.isActive() || !severity.isHighPriority()) {
      return false
    }
    val deadline = targetBuildDeadline()
    return deadline?.let { !it.isAfter(now) } ?: true
  }

  internal fun targetBuildDeadline(): Instant? =
    targetBuild?.takeIf { it.isNotBlank() }?.let(::parseTargetBuildInstant)

  private fun Status.isActive(): Boolean = this != Status.RESOLVED && this != Status.DEFERRED

  private fun Severity.isHighPriority(): Boolean =
    this == Severity.HIGH || this == Severity.CRITICAL

  enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
  }

  enum class Status {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    DEFERRED,
  }

  companion object {
    private const val TARGET_BUILD_PREFIX = "build-"

    private fun parseTargetBuildInstant(target: String): Instant? {
      val trimmed = target.trim()
      val datePart =
        if (trimmed.startsWith(TARGET_BUILD_PREFIX)) {
          trimmed.removePrefix(TARGET_BUILD_PREFIX)
        } else {
          trimmed
        }
      return runCatching { LocalDate.parse(datePart).atStartOfDay().toInstant(ZoneOffset.UTC) }
        .getOrNull()
    }
  }
}
