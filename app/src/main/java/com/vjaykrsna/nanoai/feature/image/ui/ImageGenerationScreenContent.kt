package com.vjaykrsna.nanoai.feature.image.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.image.presentation.ImageGenerationUiState
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

@Composable
internal fun ImageGenerationScreenContent(
  state: ImageGenerationUiState,
  snackbarHostState: SnackbarHostState,
  onGalleryClick: () -> Unit,
  actions: ImageGenerationScreenActions,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize().semantics { contentDescription = "Image generation screen" },
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(NanoSpacing.md))
    },
  ) { paddingValues ->
    ImageGenerationBody(
      state = state,
      actions = actions,
      onGalleryClick = onGalleryClick,
      modifier = Modifier.fillMaxSize().padding(paddingValues),
    )
  }
}

@Composable
private fun ImageGenerationBody(
  state: ImageGenerationUiState,
  actions: ImageGenerationScreenActions,
  onGalleryClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(NanoSpacing.lg).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
  ) {
    ImageGenerationHeader(onGalleryClick = onGalleryClick, isPreviewMode = state.isPreviewMode)
    ImageDisplayArea(
      generatedImagePath = state.generatedImagePath,
      isGenerating = state.isGenerating,
      onClear = actions.onClearImage,
      modifier = Modifier.fillMaxWidth(),
    )
    ImagePromptSection(state = state, actions = actions)
    ImageParameterSection(state = state, actions = actions)
    ImageGenerateButton(state = state, onGenerateClick = actions.onGenerateClick)
    ImageGenerationErrorMessage(
      errorMessage = state.errorMessage,
      onClearError = actions.onClearError,
    )
  }
}

@Composable
private fun ImageGenerationHeader(onGalleryClick: () -> Unit, isPreviewMode: Boolean = true) {
  Column(verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Image Generation",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )
      IconButton(onClick = onGalleryClick, modifier = Modifier.testTag("gallery_button")) {
        Icon(
          Icons.Default.PhotoLibrary,
          contentDescription = "Open gallery",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
    if (isPreviewMode) {
      PreviewModeBadge()
    }
  }
}

@Composable
private fun PreviewModeBadge() {
  Surface(
    color = MaterialTheme.colorScheme.tertiaryContainer,
    shape = RoundedCornerShape(NanoSpacing.sm),
    modifier = Modifier.testTag("preview_mode_badge"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = NanoSpacing.md, vertical = NanoSpacing.sm),
      horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        Icons.Default.Science,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Text(
        text = "Preview Mode – Simulated generation",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
  }
}

@Composable
private fun ImagePromptSection(
  state: ImageGenerationUiState,
  actions: ImageGenerationScreenActions,
) {
  Column(verticalArrangement = Arrangement.spacedBy(NanoSpacing.md)) {
    OutlinedTextField(
      value = state.prompt,
      onValueChange = actions.onPromptChange,
      label = { Text("Prompt") },
      placeholder = { Text("Describe the image you want to generate…") },
      modifier = Modifier.fillMaxWidth().testTag("image_prompt_input"),
      enabled = !state.isGenerating,
      minLines = 3,
    )

    OutlinedTextField(
      value = state.negativePrompt,
      onValueChange = actions.onNegativePromptChange,
      label = { Text("Negative Prompt (Optional)") },
      placeholder = { Text("What to avoid in the image…") },
      modifier = Modifier.fillMaxWidth().testTag("image_negative_prompt_input"),
      enabled = !state.isGenerating,
      minLines = 2,
    )
  }
}

@Composable
private fun ImageParameterSection(
  state: ImageGenerationUiState,
  actions: ImageGenerationScreenActions,
) {
  Column(verticalArrangement = Arrangement.spacedBy(NanoSpacing.md)) {
    Text(
      text = "Parameters",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(NanoSpacing.md),
    ) {
      DimensionInput(
        label = "Width",
        value = state.width,
        onValueChange = actions.onWidthChange,
        enabled = !state.isGenerating,
        modifier = Modifier.weight(1f),
      )
      DimensionInput(
        label = "Height",
        value = state.height,
        onValueChange = actions.onHeightChange,
        enabled = !state.isGenerating,
        modifier = Modifier.weight(1f),
      )
    }

    ImageStepsSlider(
      steps = state.steps,
      enabled = !state.isGenerating,
      onStepsChange = actions.onStepsChange,
    )
    ImageGuidanceSlider(
      guidanceScale = state.guidanceScale,
      enabled = !state.isGenerating,
      onGuidanceScaleChange = actions.onGuidanceScaleChange,
    )
  }
}

@Composable
private fun ImageStepsSlider(steps: Int, enabled: Boolean, onStepsChange: (Int) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(text = "Steps: $steps", style = MaterialTheme.typography.bodyMedium)
    Slider(
      value = steps.toFloat(),
      onValueChange = { onStepsChange(it.toInt()) },
      valueRange = 10f..150f,
      enabled = enabled,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun ImageGuidanceSlider(
  guidanceScale: Float,
  enabled: Boolean,
  onGuidanceScaleChange: (Float) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Guidance Scale: ${"%.1f".format(guidanceScale)}",
      style = MaterialTheme.typography.bodyMedium,
    )
    Slider(
      value = guidanceScale,
      onValueChange = onGuidanceScaleChange,
      valueRange = 1f..20f,
      enabled = enabled,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun ImageGenerateButton(state: ImageGenerationUiState, onGenerateClick: () -> Unit) {
  Button(
    onClick = onGenerateClick,
    enabled = !state.isGenerating && state.prompt.isNotBlank(),
    modifier = Modifier.fillMaxWidth().testTag("generate_image_button"),
  ) {
    if (state.isGenerating) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = MaterialTheme.colorScheme.onPrimary,
      )
      Spacer(modifier = Modifier.size(NanoSpacing.sm))
    }
    Text(if (state.isGenerating) "Generating…" else "Generate Image")
  }
}

@Composable
private fun ImageGenerationErrorMessage(errorMessage: String?, onClearError: () -> Unit) {
  if (errorMessage == null) return

  Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(NanoSpacing.md),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onClearError) {
        Icon(
          Icons.Default.Close,
          contentDescription = "Dismiss error",
          tint = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    }
  }
}

internal data class ImageGenerationScreenActions(
  val onPromptChange: (String) -> Unit,
  val onNegativePromptChange: (String) -> Unit,
  val onWidthChange: (Int) -> Unit,
  val onHeightChange: (Int) -> Unit,
  val onStepsChange: (Int) -> Unit,
  val onGuidanceScaleChange: (Float) -> Unit,
  val onGenerateClick: () -> Unit,
  val onClearImage: () -> Unit,
  val onClearError: () -> Unit,
)
