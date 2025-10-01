package com.vjaykrsna.nanoai.feature.library.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import kotlinx.datetime.Instant

/**
 * Room entity representing an AI model package.
 * 
 * Models can be local (downloaded) or cloud-based. Tracks installation state,
 * capabilities, and associated download tasks.
 *
 * @property modelId Unique model identifier (e.g., "gemma-2b-it")
 * @property displayName Human-readable model name
 * @property version Model version string (e.g., "1.0", "2024-01")
 * @property providerType Runtime provider for model execution
 * @property sizeBytes Total size in bytes for download planning
 * @property capabilities Set of capability strings (TEXT_GEN, IMAGE_GEN, AUDIO_IN, AUDIO_OUT)
 * @property installState Current installation status
 * @property downloadTaskId Associated download task UUID (if downloading)
 * @property checksum SHA256 checksum for verification (nullable if not downloaded)
 * @property updatedAt Timestamp when model info was last updated
 */
@Entity(tableName = "model_packages")
data class ModelPackageEntity(
    @PrimaryKey
    @ColumnInfo(name = "model_id")
    val modelId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "version")
    val version: String,

    @ColumnInfo(name = "provider_type")
    val providerType: ProviderType,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "capabilities")
    val capabilities: Set<String>,

    @ColumnInfo(name = "install_state")
    val installState: InstallState,

    @ColumnInfo(name = "download_task_id")
    val downloadTaskId: String? = null,

    @ColumnInfo(name = "checksum")
    val checksum: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
