package com.vjaykrsna.nanoai.feature.library.data.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import kotlinx.datetime.Instant

/** Room entity describing model packages with integrity metadata and catalog details. */
@Entity(
  tableName = "model_packages",
  indices =
    [
      Index(value = ["provider_type"]),
      Index(value = ["delivery_type"]),
      Index(value = ["install_state"]),
    ],
)
data class ModelPackageEntity(
  @PrimaryKey @ColumnInfo(name = "model_id") val modelId: String,
  @ColumnInfo(name = "display_name") val displayName: String,
  @ColumnInfo(name = "version") val version: String,
  @ColumnInfo(name = "provider_type") val providerType: ProviderType,
  @ColumnInfo(name = "delivery_type") val deliveryType: DeliveryType,
  @ColumnInfo(name = "min_app_version") val minAppVersion: Int,
  @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
  @ColumnInfo(name = "capabilities") val capabilities: Set<String>,
  @ColumnInfo(name = "install_state") val installState: InstallState,
  @ColumnInfo(name = "download_task_id") val downloadTaskId: String?,
  @ColumnInfo(name = "manifest_url") val manifestUrl: String,
  @ColumnInfo(name = "checksum_sha256") val checksumSha256: String?,
  @ColumnInfo(name = "signature") val signature: String?,
  @ColumnInfo(name = "created_at") val createdAt: Instant,
  @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
