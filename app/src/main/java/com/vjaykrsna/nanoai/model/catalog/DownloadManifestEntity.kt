package com.vjaykrsna.nanoai.model.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.datetime.Instant

/** Room entity representing a cached download manifest for a given model version. */
@Entity(
  tableName = "download_manifests",
  primaryKeys = ["model_id", "version"],
  foreignKeys =
    [
      ForeignKey(
        entity = ModelPackageEntity::class,
        parentColumns = ["model_id"],
        childColumns = ["model_id"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE,
      ),
    ],
  indices =
    [
      Index(value = ["model_id"]), Index(value = ["expires_at"]), Index(value = ["checksum_sha256"])
    ],
)
data class DownloadManifestEntity(
  @ColumnInfo(name = "model_id") val modelId: String,
  @ColumnInfo(name = "version") val version: String,
  @ColumnInfo(name = "checksum_sha256") val checksumSha256: String,
  @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
  @ColumnInfo(name = "download_url") val downloadUrl: String,
  @ColumnInfo(name = "signature") val signature: String?,
  @ColumnInfo(name = "public_key_url") val publicKeyUrl: String?,
  @ColumnInfo(name = "expires_at") val expiresAt: Instant?,
  @ColumnInfo(name = "fetched_at") val fetchedAt: Instant,
  @ColumnInfo(name = "release_notes") val releaseNotes: String?,
)