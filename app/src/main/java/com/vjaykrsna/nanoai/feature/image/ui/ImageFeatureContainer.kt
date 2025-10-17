package com.vjaykrsna.nanoai.feature.image.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Container that manages navigation between image generation and gallery screens.
 *
 * This handles the nested navigation within the IMAGE mode.
 */
@Composable
fun ImageFeatureContainer(modifier: Modifier = Modifier) {
  var showGallery by rememberSaveable { mutableStateOf(false) }

  if (showGallery) {
    ImageGalleryScreen(
      modifier = modifier,
      onNavigateBack = { showGallery = false },
    )
  } else {
    ImageGenerationScreen(
      modifier = modifier,
      onGalleryClick = { showGallery = true },
    )
  }
}
