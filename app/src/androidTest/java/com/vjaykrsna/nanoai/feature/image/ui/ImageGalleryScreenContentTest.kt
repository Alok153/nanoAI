package com.vjaykrsna.nanoai.feature.image.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import java.util.UUID
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class ImageGalleryScreenContentTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun showsEmptyStateWhenNoImages() {
    composeTestRule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      TestingTheme {
        ImageGalleryScreenContent(
          images = emptyList(),
          snackbarHostState = snackbarHostState,
          onNavigateBack = {},
          onImageClick = {},
          onDeleteRequest = {},
          selectedImage = null,
          showDeleteDialog = false,
          onConfirmDelete = {},
          onDismissDelete = {},
        )
      }
    }

    composeTestRule
      .onNodeWithText("No generated images yet", substring = false, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun showsDeleteDialogWhenRequested() {
    composeTestRule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      TestingTheme {
        ImageGalleryScreenContent(
          images = listOf(sampleImage()),
          snackbarHostState = snackbarHostState,
          onNavigateBack = {},
          onImageClick = {},
          onDeleteRequest = {},
          selectedImage = sampleImage(),
          showDeleteDialog = true,
          onConfirmDelete = {},
          onDismissDelete = {},
        )
      }
    }

    composeTestRule
      .onNodeWithText("Delete Image?", substring = false, useUnmergedTree = true)
      .assertExists()
  }

  private fun sampleImage(): GeneratedImage =
    GeneratedImage(
      id = UUID.randomUUID(),
      prompt = "Sunset",
      negativePrompt = "",
      width = 512,
      height = 512,
      steps = 50,
      guidanceScale = 7.5f,
      filePath = "sample.png",
      createdAt = Instant.parse("2024-01-01T00:00:00Z"),
    )
}
