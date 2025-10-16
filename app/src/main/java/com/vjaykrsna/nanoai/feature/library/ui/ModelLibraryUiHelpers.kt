package com.vjaykrsna.nanoai.feature.library.ui

import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.ModelSort
import java.util.Locale
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun downloadStatusLabel(task: DownloadTask): String {
  val progressPercent = (task.progress * ModelLibraryUiConstants.PERCENTAGE_MULTIPLIER).toInt()
  return when (task.status) {
    DownloadStatus.QUEUED -> "Queued"
    DownloadStatus.DOWNLOADING -> "Downloading ${progressPercent}%"
    DownloadStatus.PAUSED -> "Paused at ${progressPercent}%"
    DownloadStatus.COMPLETED -> "Completed"
    DownloadStatus.FAILED -> "Failed: ${task.errorMessage ?: "Unknown error"}"
    DownloadStatus.CANCELLED -> "Cancelled"
  }
}

internal fun ProviderType.displayName(): String =
  name.lowercase(Locale.US).replace('_', ' ').replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
  }

internal fun ModelSort.label(): String =
  when (this) {
    ModelSort.RECOMMENDED -> "Recommended"
    ModelSort.NAME -> "Name"
    ModelSort.SIZE_DESC -> "Size"
    ModelSort.UPDATED -> "Updated"
  }

internal fun formatSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val kib = bytes / ModelLibraryUiConstants.BYTES_PER_KIB
  val mib = bytes / ModelLibraryUiConstants.BYTES_PER_MIB
  val gib = bytes / ModelLibraryUiConstants.BYTES_PER_GIB
  return when {
    gib >= 1 -> String.format(Locale.US, "%.2f GB", gib)
    mib >= 1 -> String.format(Locale.US, "%.1f MB", mib)
    kib >= 1 -> String.format(Locale.US, "%.1f KB", kib)
    else -> "${'$'}bytes B"
  }
}

internal fun formatUpdated(updatedAt: Instant): String {
  val localDateTime = updatedAt.toLocalDateTime(TimeZone.currentSystemDefault())
  val month =
    localDateTime.month.name.lowercase(Locale.US).replaceFirstChar { char ->
      if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
    }
  return "$month ${localDateTime.dayOfMonth}, ${localDateTime.year}"
}
