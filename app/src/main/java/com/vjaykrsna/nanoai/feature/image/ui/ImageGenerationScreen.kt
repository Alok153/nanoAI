package com.vjaykrsna.nanoai.feature.image.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.feature.image.presentation.ImageGenerationViewModel
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlinx.coroutines.flow.collectLatest

/**
 * Image generation screen with prompt input, parameters, and image display.
 *
 * Follows Material 3 design and nanoAI design patterns.
 */
@Composable
fun ImageGenerationScreen(
  modifier: Modifier = Modifier,
  onGalleryClick: () -> Unit = {},
  viewModel: ImageGenerationViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val actions =
    remember(viewModel) {
      ImageGenerationScreenActions(
        onPromptChange = viewModel::updatePrompt,
        onNegativePromptChange = viewModel::updateNegativePrompt,
        onWidthChange = viewModel::updateWidth,
        onHeightChange = viewModel::updateHeight,
        onStepsChange = viewModel::updateSteps,
        onGuidanceScaleChange = viewModel::updateGuidanceScale,
        onGenerateClick = viewModel::generateImage,
        onClearImage = viewModel::clearImage,
        onClearError = viewModel::clearError,
      )
    }

  LaunchedEffect(Unit) {
    viewModel.errorEvents.collectLatest { error ->
      val message =
        when (error) {
          is com.vjaykrsna.nanoai.feature.image.presentation.ImageGenerationError.ValidationError ->
            error.message
          is com.vjaykrsna.nanoai.feature.image.presentation.ImageGenerationError.GenerationError ->
            error.message
        }
      snackbarHostState.showSnackbar(message)
    }
  }

  ImageGenerationScreenContent(
    state = uiState,
    snackbarHostState = snackbarHostState,
    onGalleryClick = onGalleryClick,
    actions = actions,
    modifier = modifier,
  )
}

@Composable
internal fun ImageDisplayArea(
  generatedImagePath: String?,
  isGenerating: Boolean,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.aspectRatio(1f),
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = MaterialTheme.shapes.medium,
  ) {
    ImageDisplayState(
      generatedImagePath = generatedImagePath,
      isGenerating = isGenerating,
      onClear = onClear,
    )
  }
}

@Composable
internal fun DimensionInput(
  label: String,
  value: Int,
  onValueChange: (Int) -> Unit,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
      value = value.toString(),
      onValueChange = { updated ->
        updated.toIntOrNull()?.let { parsed -> if (parsed > 0) onValueChange(parsed) }
      },
      enabled = enabled,
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun ImageDisplayState(
  generatedImagePath: String?,
  isGenerating: Boolean,
  onClear: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when {
      isGenerating -> ImageGeneratingPlaceholder()
      generatedImagePath != null -> ImageResultPreview(onClear = onClear)
      else -> ImageEmptyState()
    }
  }
}

@Composable
private fun ImageGeneratingPlaceholder() {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
  ) {
    CircularProgressIndicator(modifier = Modifier.size(48.dp))
    Text(
      "Generating image…",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ImageResultPreview(onClear: () -> Unit) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(NanoSpacing.md),
    contentAlignment = Alignment.Center,
  ) {
    ImagePlaceholderContent()
    IconButton(
      onClick = onClear,
      modifier = Modifier.align(Alignment.TopEnd).padding(NanoSpacing.sm),
    ) {
      Icon(
        Icons.Default.Close,
        contentDescription = "Clear image",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ImagePlaceholderContent() {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
  ) {
    Icon(
      Icons.Default.Image,
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      "Image display placeholder",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ImageEmptyState() {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
  ) {
    Icon(
      Icons.Default.Image,
      contentDescription = null,
      modifier = Modifier.size(64.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      "Generated image will appear here",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
