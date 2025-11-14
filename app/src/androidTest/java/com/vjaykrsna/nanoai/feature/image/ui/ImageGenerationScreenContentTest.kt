package com.vjaykrsna.nanoai.feature.image.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.vjaykrsna.nanoai.feature.image.presentation.ImageGenerationUiState
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import org.junit.Rule
import org.junit.Test

class ImageGenerationScreenContentTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun generateButtonReflectsPromptState() {
    val state = mutableStateOf(ImageGenerationUiState())
    val actions = buildActions(state)

    composeTestRule.setContent {
      val currentState = state.value
      val snackbarHostState = remember { SnackbarHostState() }
      TestingTheme {
        ImageGenerationScreenContent(
          state = currentState,
          snackbarHostState = snackbarHostState,
          onGalleryClick = {},
          actions = actions,
        )
      }
    }

    composeTestRule.onNodeWithTag("generate_image_button").assertIsNotEnabled()

    composeTestRule.runOnIdle { state.value = state.value.copy(prompt = "Sunset") }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("generate_image_button").assertIsEnabled()
  }

  private fun buildActions(
    state: MutableState<ImageGenerationUiState>
  ): ImageGenerationScreenActions =
    ImageGenerationScreenActions(
      onPromptChange = { value -> state.value = state.value.copy(prompt = value) },
      onNegativePromptChange = { value -> state.value = state.value.copy(negativePrompt = value) },
      onWidthChange = { value -> state.value = state.value.copy(width = value) },
      onHeightChange = { value -> state.value = state.value.copy(height = value) },
      onStepsChange = { value -> state.value = state.value.copy(steps = value) },
      onGuidanceScaleChange = { value -> state.value = state.value.copy(guidanceScale = value) },
      onGenerateClick = {},
      onClearImage = { state.value = state.value.copy(generatedImagePath = null) },
      onClearError = { state.value = state.value.copy(errorMessage = null) },
    )
}
