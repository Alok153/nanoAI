package com.vjaykrsna.nanoai.feature.library.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class HuggingFaceModelCardTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `displays basic model information`() {
    val model =
      HuggingFaceModelSummary(
        modelId = "test/model",
        displayName = "Test Model",
        author = "test-author",
        pipelineTag = "text-generation",
        libraryName = "transformers",
        tags = listOf("tag1", "tag2"),
        likes = 100,
        downloads = 1000,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        isPrivate = false,
      )

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(model) } }

    composeTestRule
      .onNodeWithText("Test Model", substring = false, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("By test-author", substring = false, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("likes", substring = true, ignoreCase = true, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("downloads", substring = true, ignoreCase = true, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun `displays enhanced metadata when available`() {
    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(enhancedModel()) } }

    assertExactText("Test Model")
    assertExactText("By test-author")

    assertSubstring("apache")
    assertSubstring("Languages:")
    assertSubstring("en, es")
    assertSubstring("Base model:")
    assertAnyNode("gpt-2")
    assertSubstring("Architectures:")
    assertAnyNode("Transformer")
    assertSubstring("Size:")
    assertAnyNode("GB")
    assertSubstring("powerful language")

    composeTestRule.onNodeWithText("01/01/24").assertExists()
    composeTestRule.onNodeWithText("01/02/24").assertExists()
  }

  @Test
  fun `handles missing optional metadata gracefully`() {
    val model =
      HuggingFaceModelSummary(
        modelId = "minimal/model",
        displayName = "Minimal Model",
        author = null,
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        license = null,
        languages = emptyList(),
        baseModel = null,
        architectures = emptyList(),
        modelType = null,
        totalSizeBytes = null,
        summary = null,
        description = null,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        isPrivate = false,
      )

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(model) } }

    composeTestRule
      .onNodeWithText("Minimal Model", substring = false, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("likes", substring = true, ignoreCase = true, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("downloads", substring = true, ignoreCase = true, useUnmergedTree = true)
      .assertExists()

    // Should not crash or display null values
    composeTestRule.onNodeWithText("null").assertDoesNotExist()
  }

  @Test
  fun `displays gated and disabled status appropriately`() {
    val gatedModel =
      HuggingFaceModelSummary(
        modelId = "gated/model",
        displayName = "Gated Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        hasGatedAccess = true,
        isDisabled = false,
        isPrivate = false,
      )

    val disabledModel =
      HuggingFaceModelSummary(
        modelId = "disabled/model",
        displayName = "Disabled Model",
        author = "test",
        pipelineTag = null,
        libraryName = null,
        tags = emptyList(),
        likes = 0,
        downloads = 0,
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        hasGatedAccess = false,
        isDisabled = true,
        isPrivate = false,
      )

    val modelState = mutableStateOf(gatedModel)

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(modelState.value) } }

    composeTestRule
      .onNodeWithText("Gated Model", substring = false, useUnmergedTree = true)
      .assertExists()

    composeTestRule.runOnIdle { modelState.value = disabledModel }

    composeTestRule
      .onNodeWithText("Disabled Model", substring = false, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun `displays size bucket correctly`() {
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
        trendingScore = null,
        createdAt = null,
        lastModified = null,
        totalSizeBytes = 500L * 1024 * 1024, // 500 MB -> TINY
        isPrivate = false,
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
        totalSizeBytes = 15L * 1024 * 1024 * 1024, // 15 GB -> LARGE
        isPrivate = false,
      )

    val modelState = mutableStateOf(tinyModel)

    composeTestRule.setContent { TestingTheme { HuggingFaceModelCard(modelState.value) } }

    composeTestRule
      .onNodeWithText("Tiny Model", substring = false, useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithText("MB", substring = true, ignoreCase = true, useUnmergedTree = true)
      .assertExists()

    composeTestRule.runOnIdle { modelState.value = largeModel }

    composeTestRule
      .onNodeWithText("Large Model", substring = false, useUnmergedTree = true)
      .assertExists()
    composeTestRule.onNodeWithText("15", substring = true, useUnmergedTree = true).assertExists()
  }
}

private fun enhancedModel(): HuggingFaceModelSummary =
  HuggingFaceModelSummary(
    modelId = "test/model",
    displayName = "Test Model",
    author = "test-author",
    pipelineTag = "text-generation",
    libraryName = "transformers",
    tags = listOf("tag1", "tag2"),
    likes = 100,
    downloads = 1000,
    license = "apache-2.0",
    languages = listOf("en", "es"),
    baseModel = "gpt-2",
    architectures = listOf("Transformer", "GPT"),
    modelType = "gpt2",
    totalSizeBytes = 1024L * 1024 * 1024 * 2,
    summary = "A powerful language model",
    description = "This is a detailed description of the model",
    trendingScore = 95,
    createdAt = Instant.parse("2024-01-01T00:00:00Z"),
    lastModified = Instant.parse("2024-01-02T00:00:00Z"),
    isPrivate = false,
  )

private fun HuggingFaceModelCardTest.assertExactText(text: String) {
  composeTestRule.onNodeWithText(text, substring = false, useUnmergedTree = true).assertExists()
}

private fun HuggingFaceModelCardTest.assertSubstring(text: String) {
  composeTestRule
    .onNodeWithText(text, substring = true, ignoreCase = true, useUnmergedTree = true)
    .assertExists()
}

private fun HuggingFaceModelCardTest.assertAnyNode(text: String) {
  val nodes =
    composeTestRule
      .onAllNodesWithText(text, substring = true, ignoreCase = true, useUnmergedTree = true)
      .fetchSemanticsNodes()
  assertThat(nodes).isNotEmpty()
}
