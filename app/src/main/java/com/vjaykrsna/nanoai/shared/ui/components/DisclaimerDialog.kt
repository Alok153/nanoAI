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
  // String resources
  val dialogContentDescription = stringResource(R.string.disclaimer_dialog_content_description)
  val declineContentDescription = stringResource(R.string.disclaimer_decline_content_description)
  val agreeContentDescription = stringResource(R.string.disclaimer_agree_content_description)
  val scrollComplete = stringResource(R.string.disclaimer_scroll_complete)
  val scrollIncomplete = stringResource(R.string.disclaimer_scroll_incomplete)

  val scrollState = rememberScrollState()
  var acceptEnabled by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) { onDialogShow?.invoke() }

  LaunchedEffect(scrollState) {
    snapshotFlow { !scrollState.canScrollForward }
      .distinctUntilChanged()
      .filter { it }
      .collect { acceptEnabled = true }
  }

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
  ) {
    Surface(
      modifier =
        modifier.fillMaxWidth().testTag("disclaimer_dialog_container").semantics {
          contentDescription = dialogContentDescription
        },
      shape = MaterialTheme.shapes.extraLarge,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
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

        DisclaimerScrollableContent(scrollState = scrollState)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
          TextButton(
            onClick = onDecline,
            modifier =
              Modifier.testTag("disclaimer_decline_button").semantics {
                contentDescription = declineContentDescription
              },
          ) {
            Text(stringResource(R.string.disclaimer_decline))
          }
          Button(
            onClick = onAccept,
            enabled = acceptEnabled,
            modifier =
              Modifier.testTag("disclaimer_accept_button").semantics {
                contentDescription = agreeContentDescription
                stateDescription = if (acceptEnabled) scrollComplete else scrollIncomplete
              },
          ) {
            Text(stringResource(R.string.disclaimer_agree))
          }
        }
      }
    }
  }
}

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
