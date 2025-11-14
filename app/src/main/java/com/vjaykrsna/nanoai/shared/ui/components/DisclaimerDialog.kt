package com.vjaykrsna.nanoai.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vjaykrsna.nanoai.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Displays the first-launch privacy disclaimer dialog. */
@Composable
fun DisclaimerDialog(
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  onDialogShow: (() -> Unit)? = null,
) {
  val strings = rememberDisclaimerStrings()
  val scrollState = rememberScrollState()
  var acceptEnabled by rememberSaveable { mutableStateOf(false) }
  val onDialogShowState = rememberUpdatedState(onDialogShow)

  LaunchedEffect(Unit) { onDialogShowState.value?.invoke() }
  ObserveScrollCompletion(onScrolledToEnd = { acceptEnabled = true }, scrollState = scrollState)

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
  ) {
    DisclaimerDialogSurface(
      strings = strings,
      scrollState = scrollState,
      acceptEnabled = acceptEnabled,
      onDecline = onDecline,
      onAccept = onAccept,
      modifier = modifier,
    )
  }
}

@Composable
private fun rememberDisclaimerStrings(): DisclaimerStrings =
  DisclaimerStrings(
    dialogContentDescription = stringResource(R.string.disclaimer_dialog_content_description),
    declineContentDescription = stringResource(R.string.disclaimer_decline_content_description),
    agreeContentDescription = stringResource(R.string.disclaimer_agree_content_description),
    scrollComplete = stringResource(R.string.disclaimer_scroll_complete),
    scrollIncomplete = stringResource(R.string.disclaimer_scroll_incomplete),
  )

@Composable
private fun ObserveScrollCompletion(
  onScrolledToEnd: () -> Unit,
  scrollState: androidx.compose.foundation.ScrollState,
) {
  val onScrolledToEndState = rememberUpdatedState(onScrolledToEnd)
  LaunchedEffect(scrollState) {
    snapshotFlow { !scrollState.canScrollForward }
      .distinctUntilChanged()
      .filter { it }
      .collect { onScrolledToEndState.value() }
  }
}

@Composable
private fun DisclaimerDialogSurface(
  strings: DisclaimerStrings,
  scrollState: androidx.compose.foundation.ScrollState,
  acceptEnabled: Boolean,
  onDecline: () -> Unit,
  onAccept: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier.fillMaxWidth().testTag("disclaimer_dialog_container").semantics {
        contentDescription = strings.dialogContentDescription
      },
    shape = MaterialTheme.shapes.extraLarge,
    tonalElevation = 6.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(all = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      DisclaimerDialogHeader()
      DisclaimerScrollableContent(scrollState = scrollState)
      DisclaimerDialogActions(strings, acceptEnabled, onDecline, onAccept)
    }
  }
}

@Composable
private fun DisclaimerDialogHeader() {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.disclaimer_title),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = stringResource(R.string.disclaimer_subtitle),
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
private fun DisclaimerDialogActions(
  strings: DisclaimerStrings,
  acceptEnabled: Boolean,
  onDecline: () -> Unit,
  onAccept: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
  ) {
    TextButton(
      onClick = onDecline,
      modifier =
        Modifier.testTag("disclaimer_decline_button").semantics {
          contentDescription = strings.declineContentDescription
        },
    ) {
      Text(stringResource(R.string.disclaimer_decline))
    }
    Button(
      onClick = onAccept,
      enabled = acceptEnabled,
      modifier =
        Modifier.testTag("disclaimer_accept_button").semantics {
          contentDescription = strings.agreeContentDescription
          stateDescription = if (acceptEnabled) strings.scrollComplete else strings.scrollIncomplete
        },
    ) {
      Text(stringResource(R.string.disclaimer_agree))
    }
  }
}

private data class DisclaimerStrings(
  val dialogContentDescription: String,
  val declineContentDescription: String,
  val agreeContentDescription: String,
  val scrollComplete: String,
  val scrollIncomplete: String,
)

@Composable
private fun DisclaimerScrollableContent(scrollState: androidx.compose.foundation.ScrollState) {
  val bulletPoints =
    listOf(
      stringResource(R.string.disclaimer_bullet_1),
      stringResource(R.string.disclaimer_bullet_2),
      stringResource(R.string.disclaimer_bullet_3),
      stringResource(R.string.disclaimer_bullet_4),
      stringResource(R.string.disclaimer_bullet_5),
    )
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 160.dp, max = 280.dp)
        .verticalScroll(scrollState)
        .testTag("disclaimer_scrollable_content")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      bulletPoints.forEach { point ->
        Text(text = "• $point", style = MaterialTheme.typography.bodyMedium)
      }

      Text(
        text = stringResource(R.string.disclaimer_responsibility_statement),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start,
        modifier = Modifier.testTag("disclaimer_last_text"),
      )
    }
  }
}
