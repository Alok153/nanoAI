package com.vjaykrsna.nanoai.model.catalog

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelManifestLocatorTest {
  @Test
  fun `parse returns remote for https url`() {
    val locator = ModelManifestLocator.parse("https://cdn.nanoai.app/catalog/model.json")

    assertThat(locator).isInstanceOf(ModelManifestLocator.Remote::class.java)
  }

  @Test
  fun `parse returns hugging face locator`() {
    val locator =
      ModelManifestLocator.parse(
        "hf://google/gemma-2-2b-it?artifact=LiteRT/model.bin&revision=main",
      )

    assertThat(locator).isInstanceOf(ModelManifestLocator.HuggingFace::class.java)
    val huggingFace = locator as ModelManifestLocator.HuggingFace
    assertThat(huggingFace.repository).isEqualTo("google/gemma-2-2b-it")
    assertThat(huggingFace.artifactPath).isEqualTo("LiteRT/model.bin")
    assertThat(huggingFace.revision).isEqualTo("main")
  }
}
