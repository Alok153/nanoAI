package com.vjaykrsna.nanoai.feature.library.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HuggingFaceFiltersTest {

  @Test
  fun `belongsTo returns true for null bucket (no filter)`() {
    assertThat(123L.belongsTo(null)).isTrue()
    assertThat(null.belongsTo(null)).isTrue()
  }

  @Test
  fun `belongsTo returns false for null size when bucket requires size`() {
    assertThat(null.belongsTo(HuggingFaceSizeBucket.TINY)).isFalse()
    assertThat(null.belongsTo(HuggingFaceSizeBucket.SMALL)).isFalse()
  }

  @Test
  fun `belongsTo correctly categorizes sizes into buckets`() {
    // TINY: < 1 GB
    assertThat(0L.belongsTo(HuggingFaceSizeBucket.TINY)).isTrue()
    assertThat((1024L * 1024 * 1024 - 1).belongsTo(HuggingFaceSizeBucket.TINY)).isTrue()
    assertThat((1024L * 1024 * 1024).belongsTo(HuggingFaceSizeBucket.TINY)).isFalse()

    // SMALL: 1 - 4 GB
    assertThat((1024L * 1024 * 1024).belongsTo(HuggingFaceSizeBucket.SMALL)).isTrue()
    assertThat((4L * 1024 * 1024 * 1024 - 1).belongsTo(HuggingFaceSizeBucket.SMALL)).isTrue()
    assertThat((4L * 1024 * 1024 * 1024).belongsTo(HuggingFaceSizeBucket.SMALL)).isFalse()

    // MEDIUM: 4 - 10 GB
    assertThat((4L * 1024 * 1024 * 1024).belongsTo(HuggingFaceSizeBucket.MEDIUM)).isTrue()
    assertThat((10L * 1024 * 1024 * 1024 - 1).belongsTo(HuggingFaceSizeBucket.MEDIUM)).isTrue()
    assertThat((10L * 1024 * 1024 * 1024).belongsTo(HuggingFaceSizeBucket.MEDIUM)).isFalse()

    // LARGE: >= 10 GB
    assertThat((10L * 1024 * 1024 * 1024).belongsTo(HuggingFaceSizeBucket.LARGE)).isTrue()
    assertThat(Long.MAX_VALUE.belongsTo(HuggingFaceSizeBucket.LARGE)).isTrue()
  }

  @Test
  fun `sizeBucket computed property categorizes correctly`() {
    val tinyModel =
      HuggingFaceModelSummary(
        modelId = "tiny",
        displayName = "Tiny Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        totalSizeBytes = 500L * 1024 * 1024, // 500 MB
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        isPrivate = false
      )

    val smallModel =
      HuggingFaceModelSummary(
        modelId = "small",
        displayName = "Small Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        isPrivate = false
      )

    val largeModel =
      HuggingFaceModelSummary(
        modelId = "large",
        displayName = "Large Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        isPrivate = false
      )

    val unknownSizeModel =
      HuggingFaceModelSummary(
        modelId = "unknown",
        displayName = "Unknown Size Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        totalSizeBytes = null,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        isPrivate = false
      )

    assertThat(tinyModel.sizeBucket).isEqualTo(HuggingFaceSizeBucket.TINY)
    assertThat(smallModel.sizeBucket).isEqualTo(HuggingFaceSizeBucket.SMALL)
    assertThat(largeModel.sizeBucket).isEqualTo(HuggingFaceSizeBucket.LARGE)
    assertThat(unknownSizeModel.sizeBucket).isNull()
  }

  @Test
  fun `sizeBucket constants are correct values`() {
    assertThat(HuggingFaceSizeBucket.ONE_GB).isEqualTo(1024L * 1024 * 1024)
    assertThat(HuggingFaceSizeBucket.FOUR_GB).isEqualTo(4L * 1024 * 1024 * 1024)
    assertThat(HuggingFaceSizeBucket.TEN_GB).isEqualTo(10L * 1024 * 1024 * 1024)
  }
}
