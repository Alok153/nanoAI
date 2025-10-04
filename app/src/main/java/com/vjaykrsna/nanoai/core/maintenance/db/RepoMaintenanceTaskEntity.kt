package com.vjaykrsna.nanoai.core.maintenance.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.core.maintenance.model.MaintenanceCategory
import com.vjaykrsna.nanoai.core.maintenance.model.MaintenanceStatus
import com.vjaykrsna.nanoai.core.maintenance.model.PriorityLevel
import kotlinx.datetime.Instant

/** Room entity backing RepoMaintenanceTask records used to track stabilization work. */
@Entity(
  tableName = "repo_maintenance_tasks",
  indices = [Index(value = ["status"]), Index(value = ["priority"])],
)
data class RepoMaintenanceTaskEntity(
  @PrimaryKey @ColumnInfo(name = "task_id") val id: String,
  @ColumnInfo(name = "title") val title: String,
  @ColumnInfo(name = "description") val description: String,
  @ColumnInfo(name = "category") val category: MaintenanceCategory,
  @ColumnInfo(name = "priority") val priority: PriorityLevel,
  @ColumnInfo(name = "status") val status: MaintenanceStatus,
  @ColumnInfo(name = "owner") val owner: String?,
  @ColumnInfo(name = "blocking_rules") val blockingRules: List<String>,
  @ColumnInfo(name = "linked_artifacts") val linkedArtifacts: List<String>,
  @ColumnInfo(name = "created_at") val createdAt: Instant,
  @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
