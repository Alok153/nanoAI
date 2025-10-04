package com.vjaykrsna.nanoai.core.maintenance.db

import com.vjaykrsna.nanoai.core.maintenance.model.CodeQualityMetric

/** Mapping helpers between Room entities and domain models for code quality metrics. */
fun CodeQualityMetricEntity.toDomain(): CodeQualityMetric =
  CodeQualityMetric(
    id = id,
    taskId = taskId,
    ruleId = ruleId,
    filePath = filePath,
    severity = severity,
    occurrences = occurrences,
    threshold = threshold,
    firstDetectedAt = firstDetectedAt,
    resolvedAt = resolvedAt,
    notes = notes,
  )

fun CodeQualityMetric.toEntity(): CodeQualityMetricEntity =
  CodeQualityMetricEntity(
    id = id,
    taskId = taskId,
    ruleId = ruleId,
    filePath = filePath,
    severity = severity,
    occurrences = occurrences,
    threshold = threshold,
    firstDetectedAt = firstDetectedAt,
    resolvedAt = resolvedAt,
    notes = notes,
  )
