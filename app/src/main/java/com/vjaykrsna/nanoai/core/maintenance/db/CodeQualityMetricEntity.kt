package com.vjaykrsna.nanoai.core.maintenance.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.core.maintenance.model.SeverityLevel
import kotlinx.datetime.Instant

/** Room entity capturing Detekt/ktlint metric snapshots linked to maintenance tasks. */
@Entity(
  tableName = "code_quality_metrics",
  foreignKeys =
    [
      ForeignKey(
        entity = RepoMaintenanceTaskEntity::class,
        parentColumns = ["task_id"],
        childColumns = ["task_id"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.SET_NULL,
      )
    ],
  indices = [Index(value = ["task_id"]), Index(value = ["rule_id"]), Index(value = ["file_path"])],
)
data class CodeQualityMetricEntity(
  @PrimaryKey @ColumnInfo(name = "metric_id") val id: String,
  @ColumnInfo(name = "task_id") val taskId: String?,
  @ColumnInfo(name = "rule_id") val ruleId: String,
  @ColumnInfo(name = "file_path") val filePath: String,
  @ColumnInfo(name = "severity") val severity: SeverityLevel,
  @ColumnInfo(name = "occurrences") val occurrences: Int,
  @ColumnInfo(name = "threshold") val threshold: Int,
  @ColumnInfo(name = "first_detected_at") val firstDetectedAt: Instant,
  @ColumnInfo(name = "resolved_at") val resolvedAt: Instant?,
  @ColumnInfo(name = "notes") val notes: String?,
)
