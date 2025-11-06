package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab
import com.vjaykrsna.nanoai.shared.testing.DomainTestBuilders
import org.junit.Test

class ModelLibraryScreenFilteringTest : BaseModelLibraryScreenTest() {

  @Test
  fun modelLibraryScreen_displaysModels() {
    val model1 =
      DomainTestBuilders.buildModelPackage(
        modelId = "model-1",
        displayName = "Test Model 1",
        providerType = ProviderType.MEDIA_PIPE,
      )
    val model2 =
      DomainTestBuilders.buildModelPackage(
        modelId = "model-2",
        displayName = "Test Model 2",
        providerType = ProviderType.CLOUD_API,
      )

    replaceCatalog(listOf(model1, model2))

    renderModelLibraryScreen()
    composeTestRule.runOnIdle { viewModel.selectTab(ModelLibraryTab.CURATED) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Test Model 1", substring = true).assertExists()
    composeTestRule.onNodeWithText("Test Model 2", substring = true).assertExists()
  }

  @Test
  fun modelLibraryScreen_filterBySearchQuery() {
    val model1 =
      DomainTestBuilders.buildModelPackage(modelId = "model-1", displayName = "Qwen Model")
    val model2 =
      DomainTestBuilders.buildModelPackage(modelId = "model-2", displayName = "Gemma Model")

    replaceCatalog(listOf(model1, model2))

    renderModelLibraryScreen()
    composeTestRule.runOnIdle { viewModel.selectTab(ModelLibraryTab.CURATED) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ModelLibraryUiConstants.SEARCH_FIELD_TAG).performTextInput("Qwen")

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Qwen Model", substring = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule.onNodeWithText("Qwen Model", substring = true).assertExists()
    composeTestRule.onAllNodesWithText("Gemma Model", substring = true).assertCountEquals(0)
  }

  @Test
  fun modelLibraryScreen_filterByProvider() {
    val hfModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "hf-model",
        displayName = "HF Model",
        providerType = ProviderType.MEDIA_PIPE,
      )
    val googleModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "google-model",
        displayName = "Google Model",
        providerType = ProviderType.CLOUD_API,
      )

    replaceCatalog(listOf(hfModel, googleModel))

    renderModelLibraryScreen()
    composeTestRule.runOnIdle { viewModel.selectTab(ModelLibraryTab.CURATED) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("HF Model", substring = true).assertExists()
    composeTestRule.onNodeWithText("Google Model", substring = true).assertExists()

    composeTestRule.onNodeWithTag(ModelLibraryUiConstants.FILTER_TOGGLE_TAG).performClick()

    composeTestRule
      .onNode(
        hasText("Media pipe", substring = false) and
          hasClickAction() and
          hasAnyAncestor(hasTestTag(ModelLibraryUiConstants.FILTER_PANEL_TAG))
      )
      .assertHasClickAction()
      .performClick()

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Google Model", substring = true)
        .fetchSemanticsNodes()
        .isEmpty()
    }

    composeTestRule.onNodeWithText("HF Model", substring = true).assertExists()
    composeTestRule.onAllNodesWithText("Google Model", substring = true).assertCountEquals(0)
  }

  @Test
  fun modelLibraryScreen_filterByCapability() {
    val chatModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "chat-model",
        displayName = "Chat Model",
        capabilities = setOf("chat"),
      )
    val embeddingModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "embedding-model",
        displayName = "Embedding Model",
        capabilities = setOf("embeddings"),
      )

    replaceCatalog(listOf(chatModel, embeddingModel))

    renderModelLibraryScreen()
    composeTestRule.runOnIdle { viewModel.selectTab(ModelLibraryTab.CURATED) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Chat Model", substring = true).assertExists()
    composeTestRule.onNodeWithText("Embedding Model", substring = true).assertExists()

    // Expand filter panel
    composeTestRule.onNodeWithTag(ModelLibraryUiConstants.FILTER_TOGGLE_TAG).performClick()

    composeTestRule
      .onNode(
        hasText("Chat", substring = false) and
          hasClickAction() and
          hasAnyAncestor(hasTestTag(ModelLibraryUiConstants.FILTER_PANEL_TAG))
      )
      .assertHasClickAction()
      .performClick()

    composeTestRule.waitUntil {
      composeTestRule
        .onAllNodesWithText("Embedding Model", substring = true)
        .fetchSemanticsNodes()
        .isEmpty()
    }

    composeTestRule.onNodeWithText("Chat Model", substring = true).assertExists()
    composeTestRule.onAllNodesWithText("Embedding Model", substring = true).assertCountEquals(0)
  }

  @Test
  fun modelLibraryScreen_clearFilters() {
    val model = DomainTestBuilders.buildModelPackage(modelId = "test-model")
    replaceCatalog(listOf(model))

    renderModelLibraryScreen()
    composeTestRule.runOnIdle { viewModel.selectTab(ModelLibraryTab.CURATED) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ModelLibraryUiConstants.SEARCH_FIELD_TAG).performTextInput("Test")

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithContentDescription("Clear search query")
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule.onNodeWithContentDescription("Clear search query").performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithContentDescription("Clear search query")
        .fetchSemanticsNodes()
        .isEmpty()
    }

    composeTestRule.runOnIdle { assertThat(viewModel.filters.value.currentSearchQuery()).isEmpty() }
  }

  @Test
  fun modelLibraryScreen_filterChips_haveAccessibility() {
    val model = DomainTestBuilders.buildModelPackage(modelId = "test-model")
    replaceCatalog(listOf(model))

    renderModelLibraryScreen()

    composeTestRule.onNodeWithTag(ModelLibraryUiConstants.FILTER_TOGGLE_TAG).performClick()
    composeTestRule.onNodeWithText("All providers", substring = false).assertExists()
    composeTestRule.onNodeWithText("Recommended", substring = false).assertExists()
  }
}
