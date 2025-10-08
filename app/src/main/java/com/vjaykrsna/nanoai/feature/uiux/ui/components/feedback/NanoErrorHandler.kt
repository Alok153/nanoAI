package com.vjaykrsna.nanoai.feature.uiux.ui.components.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.vjaykrsna.nanoai.feature.uiux.state.NanoError
import com.vjaykrsna.nanoai.feature.uiux.ui.components.ConnectivityBanner
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoRadii
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

@Composable
fun NanoErrorHandler(
  error: NanoError?,
  modifier: Modifier = Modifier,
  snackbarHostState: SnackbarHostState? = null,
  onAction: (() -> Unit)? = null,
  onDismiss: (() -> Unit)? = null,
) {
  if (error is NanoError.Snackbar && snackbarHostState != null) {
    LaunchedEffect(error.message) { snackbarHostState.showSnackbar(message = error.message) }
  }

  val inlineError = error.takeIf { it != null && it !is NanoError.Snackbar }

  AnimatedVisibility(
    visible = inlineError != null,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
    modifier = modifier.fillMaxWidth(),
  ) {
    when (val resolved = inlineError) {
      is NanoError.Network ->
        ConnectivityBanner(
          state = resolved.banner,
          onCtaClick = onAction ?: {},
          onDismiss = onDismiss ?: {},
        )
      is NanoError.Inline ->
        InlineErrorMessage(
          title = resolved.title,
          description = resolved.description,
          actionLabel = resolved.actionLabel,
          onAction = onAction,
          onDismiss = onDismiss,
        )
      else -> {}
    }
  }
}

@Composable
private fun InlineErrorMessage(
  title: String,
  description: String?,
  actionLabel: String?,
  onAction: (() -> Unit)?,
  onDismiss: (() -> Unit)?,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title error" },
    shape = RoundedCornerShape(NanoRadii.medium),
    tonalElevation = NanoElevation.level1,
    color = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(NanoSpacing.md),
      horizontalArrangement = Arrangement.spacedBy(NanoSpacing.md),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      androidx.compose.material3.Icon(
        imageVector = Icons.Rounded.ErrorOutline,
        contentDescription = null,
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(NanoSpacing.xs),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
        description?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
      Column(
        verticalArrangement = Arrangement.spacedBy(NanoSpacing.xs),
        horizontalAlignment = Alignment.End,
      ) {
        if (!actionLabel.isNullOrBlank() && onAction != null) {
          Button(onClick = onAction) { Text(actionLabel) }
        }
        onDismiss?.let { dismiss -> TextButton(onClick = dismiss) { Text(text = "Dismiss") } }
      }
    }
  }
}
