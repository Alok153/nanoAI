package com.vjaykrsna.nanoai.feature.image.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.core.domain.image.model.GeneratedImage
import com.vjaykrsna.nanoai.feature.image.presentation.ImageGalleryViewModel
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Gallery screen displaying all generated images with metadata.
 *
 * Shows images in a grid layout with prompt and generation parameters.
 */
@Composable
fun ImageGalleryScreen(
  modifier: Modifier = Modifier,
  onNavigateBack: () -> Unit = {},
  onImageClick: (GeneratedImage) -> Unit = {},
  viewModel: ImageGalleryViewModel = hiltViewModel(),
) {
  val images by viewModel.images.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedImage by remember { mutableStateOf<GeneratedImage?>(null) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.events.collectLatest { event ->
      when (event) {
        is com.vjaykrsna.nanoai.feature.image.presentation.ImageGalleryEvent.ImageDeleted ->
          snackbarHostState.showSnackbar("Image deleted")
        is com.vjaykrsna.nanoai.feature.image.presentation.ImageGalleryEvent.AllImagesDeleted ->
          snackbarHostState.showSnackbar("All images deleted")
      }
    }
  }

  Box(modifier = modifier.fillMaxSize().semantics { contentDescription = "Image gallery screen" }) {
    Column(modifier = Modifier.fillMaxSize().padding(NanoSpacing.lg)) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
        ) {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
          Text(
            text = "Image Gallery",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = "${images.size} images",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(NanoSpacing.md))

      // Gallery grid
      if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
              "No generated images yet",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 150.dp),
          contentPadding = PaddingValues(vertical = NanoSpacing.sm),
          horizontalArrangement = Arrangement.spacedBy(NanoSpacing.md),
          verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
          modifier = Modifier.fillMaxSize().testTag("image_gallery_grid"),
        ) {
          items(images, key = { it.id }) { image ->
            ImageGalleryItem(
              image = image,
              onClick = { onImageClick(image) },
              onDeleteClick = {
                selectedImage = image
                showDeleteDialog = true
              },
            )
          }
        }
      }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedImage != null) {
      AlertDialog(
        onDismissRequest = {
          showDeleteDialog = false
          selectedImage = null
        },
        title = { Text("Delete Image?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
          TextButton(
            onClick = {
              selectedImage?.let { viewModel.deleteImage(it.id) }
              showDeleteDialog = false
              selectedImage = null
            }
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              showDeleteDialog = false
              selectedImage = null
            }
          ) {
            Text("Cancel")
          }
        },
      )
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(NanoSpacing.md),
    )
  }
}

@Composable
private fun ImageGalleryItem(
  image: GeneratedImage,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.clickable(onClick = onClick).testTag("gallery_item_${image.id}"),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(NanoSpacing.sm)) {
      // Image placeholder
      Surface(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(NanoSpacing.sm))

      // Metadata
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = image.prompt,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${image.width}×${image.height} • ${image.steps} steps",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text =
              image.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).let {
                "${it.date} ${it.hour}:${it.minute.toString().padStart(2, '0')}"
              },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "Delete image",
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}
