package com.vjaykrsna.nanoai.core.maintenance.model

import kotlinx.datetime.Instant

/** Domain models representing repository maintenance tracking entries and code quality metrics. */
enum class MaintenanceCategory {
  STATIC_ANALYSIS,
  SECURITY,
  TESTING,
  RUNTIME,
  DOCS,
}

enum class PriorityLevel {
  CRITICAL,
  HIGH,
  MEDIUM,
  LOW,
}

enum class MaintenanceStatus {
  IDENTIFIED,
  IN_PROGRESS,
  IN_REVIEW,
  VERIFIED,
  BLOCKED,
}

enum class SeverityLevel {
  WARNING,
  ERROR,
}

/** Data class describing a maintenance task tracked in Room. */
data class RepoMaintenanceTask(
  val id: String,
  val title: String,
  val description: String,
  val category: MaintenanceCategory,
  val priority: PriorityLevel,
  val status: MaintenanceStatus,
  val owner: String?,
  val blockingRules: List<String>,
  val linkedArtifacts: List<String>,
  val createdAt: Instant,
  val updatedAt: Instant,
)

/** Data class describing a Detekt/ktlint code quality metric record. */
data class CodeQualityMetric(
  val id: String,
  val taskId: String?,
  val ruleId: String,
  val filePath: String,
  val severity: SeverityLevel,
  val occurrences: Int,
  val threshold: Int,
  val firstDetectedAt: Instant,
  val resolvedAt: Instant?,
  val notes: String?,
)
