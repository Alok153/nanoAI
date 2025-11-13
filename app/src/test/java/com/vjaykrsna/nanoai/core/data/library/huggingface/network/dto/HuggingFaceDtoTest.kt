package com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

class HuggingFaceDtoTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `isGated returns true when gated flag boolean`() {
    val dto = HuggingFaceModelListingDto(gated = JsonPrimitive(true))

    assertThat(dto.isGated).isTrue()
  }

  @Test
  fun `isGated returns true when gated string manual`() {
    val dto = HuggingFaceModelListingDto(gated = JsonPrimitive("manual"))

    assertThat(dto.isGated).isTrue()
  }

  @Test
  fun `isGated returns false for null payload`() {
    val dto = HuggingFaceModelListingDto(gated = null)

    assertThat(dto.isGated).isFalse()
  }

  @Test
  fun `model listing dto round-trip preserves nested content`() {
    val payload =
      """
      {
        "modelId": "nanoai/test-model",
        "author": "nanoai",
        "pipeline_tag": "text-generation",
        "tags": ["text-generation", "nanotag"],
        "gated": true,
        "cardData": {
          "license": "mit",
          "summary": "Test summary"
        },
        "siblings": [
          { "rfilename": "config.json", "size": 2048 },
          { "rfilename": "model.safetensors", "size": 4096 }
        ]
      }
      """
        .trimIndent()

    val dto = json.decodeFromString<HuggingFaceModelListingDto>(payload)

    assertThat(dto.modelId).isEqualTo("nanoai/test-model")
    assertThat(dto.pipelineTag).isEqualTo("text-generation")
    assertThat(dto.isGated).isTrue()
    assertThat(dto.cardData?.summary).isEqualTo("Test summary")
    assertThat(dto.siblings).isNotNull()
    assertThat(dto.siblings).hasSize(2)
    assertThat(dto.siblings!!.first().filename).isEqualTo("config.json")
  }

  @Test
  fun `model summary dto serializes siblings`() {
    val dto =
      HuggingFaceModelDto(
        modelId = "nanoai/test",
        revisionSha = "abcdef",
        siblings =
          listOf(
            HuggingFaceSiblingDto(
              relativeFilename = "README.md",
              sizeBytes = 512,
              lfs = HuggingFaceLfsDto(oid = "oid-1", sizeBytes = 512, sha256 = "sha"),
              gitOid = "git-1",
              sha256 = "sha",
            )
          ),
      )

    val encoded = json.encodeToString(dto)
    val decoded = json.decodeFromString<HuggingFaceModelDto>(encoded)

    assertThat(decoded.modelId).isEqualTo("nanoai/test")
    assertThat(decoded.revisionSha).isEqualTo("abcdef")
    assertThat(decoded.siblings).hasSize(1)
    val sibling = decoded.siblings.first()
    assertThat(sibling.relativeFilename).isEqualTo("README.md")
    assertThat(sibling.lfs?.oid).isEqualTo("oid-1")
  }
}
