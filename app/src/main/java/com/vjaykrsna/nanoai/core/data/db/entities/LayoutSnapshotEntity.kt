package com.vjaykrsna.nanoai.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot

/** Room entity capturing per-user saved layout configurations. */
@Entity(
  tableName = "layout_snapshots",
  foreignKeys =
    [
      ForeignKey(
        entity = UserProfileEntity::class,
        parentColumns = ["user_id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE,
      ),
    ],
  indices =
    [
      Index(value = ["user_id"]),
      Index(value = ["user_id", "position"], unique = true),
      Index(value = ["last_opened_screen"]),
    ],
)
data class LayoutSnapshotEntity(
  @PrimaryKey @ColumnInfo(name = "layout_id") val layoutId: String,
  @ColumnInfo(name = "user_id") val userId: String,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "last_opened_screen") val lastOpenedScreen: String,
  @ColumnInfo(name = "pinned_tools") val pinnedTools: List<String>,
  @ColumnInfo(name = "is_compact") val isCompact: Boolean,
  @ColumnInfo(name = "position") val position: Int,
)

fun LayoutSnapshotEntity.toDomain(): LayoutSnapshot =
  LayoutSnapshot(
    id = layoutId,
    name = name,
    lastOpenedScreen = lastOpenedScreen,
    pinnedTools = pinnedTools,
    isCompact = isCompact,
  )

fun LayoutSnapshot.toEntity(userId: String, position: Int): LayoutSnapshotEntity =
  LayoutSnapshotEntity(
    layoutId = id,
    userId = userId,
    name = name,
    lastOpenedScreen = lastOpenedScreen,
    pinnedTools = pinnedTools,
    isCompact = isCompact,
    position = position,
  )
