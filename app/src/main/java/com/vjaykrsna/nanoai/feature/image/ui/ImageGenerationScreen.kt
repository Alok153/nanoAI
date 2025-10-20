package com.vjaykrsna.nanoai.feature.image.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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

  Box(
    modifier = modifier.fillMaxSize().semantics { contentDescription = "Image generation screen" }
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize().padding(NanoSpacing.lg).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
    ) {
      // Header
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

      // Image display area
      ImageDisplayArea(
        generatedImagePath = uiState.generatedImagePath,
        isGenerating = uiState.isGenerating,
        onClear = viewModel::clearImage,
        modifier = Modifier.fillMaxWidth(),
      )

      // Prompt input
      OutlinedTextField(
        value = uiState.prompt,
        onValueChange = viewModel::updatePrompt,
        label = { Text("Prompt") },
        placeholder = { Text("Describe the image you want to generate…") },
        modifier = Modifier.fillMaxWidth().testTag("image_prompt_input"),
        enabled = !uiState.isGenerating,
        minLines = 3,
      )

      // Negative prompt input
      OutlinedTextField(
        value = uiState.negativePrompt,
        onValueChange = viewModel::updateNegativePrompt,
        label = { Text("Negative Prompt (Optional)") },
        placeholder = { Text("What to avoid in the image…") },
        modifier = Modifier.fillMaxWidth().testTag("image_negative_prompt_input"),
        enabled = !uiState.isGenerating,
        minLines = 2,
      )

      // Parameters section
      Text(
        text = "Parameters",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )

      // Dimensions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NanoSpacing.md),
      ) {
        DimensionInput(
          label = "Width",
          value = uiState.width,
          onValueChange = viewModel::updateWidth,
          enabled = !uiState.isGenerating,
          modifier = Modifier.weight(1f),
        )
        DimensionInput(
          label = "Height",
          value = uiState.height,
          onValueChange = viewModel::updateHeight,
          enabled = !uiState.isGenerating,
          modifier = Modifier.weight(1f),
        )
      }

      // Steps slider
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Steps: ${uiState.steps}", style = MaterialTheme.typography.bodyMedium)
        Slider(
          value = uiState.steps.toFloat(),
          onValueChange = { viewModel.updateSteps(it.toInt()) },
          valueRange = 10f..150f,
          enabled = !uiState.isGenerating,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      // Guidance scale slider
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Guidance Scale: ${"%.1f".format(uiState.guidanceScale)}",
          style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
          value = uiState.guidanceScale,
          onValueChange = viewModel::updateGuidanceScale,
          valueRange = 1f..20f,
          enabled = !uiState.isGenerating,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      // Generate button
      Button(
        onClick = viewModel::generateImage,
        enabled = !uiState.isGenerating && uiState.prompt.isNotBlank(),
        modifier = Modifier.fillMaxWidth().testTag("generate_image_button"),
      ) {
        if (uiState.isGenerating) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Spacer(modifier = Modifier.size(NanoSpacing.sm))
        }
        Text(if (uiState.isGenerating) "Generating…" else "Generate Image")
      }

      // Error display
      if (uiState.errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(NanoSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = uiState.errorMessage ?: "",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::clearError) {
              Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(NanoSpacing.md),
    )
  }
}

@Composable
private fun ImageDisplayArea(
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      when {
        isGenerating -> {
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
        generatedImagePath != null -> {
          // TODO: Load and display actual image
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(NanoSpacing.md),
            contentAlignment = Alignment.Center,
          ) {
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
        else -> {
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
      }
    }
  }
}

@Composable
private fun DimensionInput(
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
      onValueChange = { newValue -> newValue.toIntOrNull()?.let { if (it > 0) onValueChange(it) } },
      enabled = enabled,
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
