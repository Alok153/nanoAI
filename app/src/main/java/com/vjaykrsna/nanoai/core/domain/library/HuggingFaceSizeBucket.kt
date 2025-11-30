package com.vjaykrsna.nanoai.core.domain.library

/**
 * Buckets for approximate Hugging Face model sizes based on total artifact footprint.
 *
 * The ranges are intentionally coarse so we can group models for faceted filtering without
 * requiring exact parameter counts from metadata.
 */
enum class HuggingFaceSizeBucket(val label: String, val minBytes: Long?, val maxBytes: Long?) {
  TINY(label = "< 1 GB", minBytes = null, maxBytes = 1073741824L),
  SMALL(label = "1 - 4 GB", minBytes = 1073741824L, maxBytes = 4294967296L),
  MEDIUM(label = "4 - 10 GB", minBytes = 4294967296L, maxBytes = 10737418240L),
  LARGE(label = ">= 10 GB", minBytes = 10737418240L, maxBytes = null);

  companion object {
    const val ONE_GB = 1L shl 30
    const val FOUR_GB = 4L shl 30
    const val TEN_GB = 10L shl 30
  }
}

internal fun Long?.belongsTo(bucket: HuggingFaceSizeBucket?): Boolean {
  return bucket == null ||
    this?.let { bytes ->
      val min = bucket.minBytes
      val max = bucket.maxBytes
      !(min != null && bytes < min || max != null && bytes >= max)
    } ?: false
}
