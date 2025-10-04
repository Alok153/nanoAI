package com.vjaykrsna.nanoai.core.maintenance.db

import com.vjaykrsna.nanoai.core.maintenance.model.RepoMaintenanceTask

/** Mapping extensions between Room entity and domain model. */
fun RepoMaintenanceTaskEntity.toDomain(): RepoMaintenanceTask =
  RepoMaintenanceTask(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    status = status,
    owner = owner,
    blockingRules = blockingRules,
    linkedArtifacts = linkedArtifacts,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun RepoMaintenanceTask.toEntity(): RepoMaintenanceTaskEntity =
  RepoMaintenanceTaskEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    status = status,
    owner = owner,
    blockingRules = blockingRules,
    linkedArtifacts = linkedArtifacts,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
