package com.vjaykrsna.nanoai.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
          contentDescription = "nanoAI privacy disclaimer dialog"
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
            text = "Review nanoAI disclaimer",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text =
              "nanoAI may generate inaccurate or unexpected content. Please review outputs before sharing.",
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
                contentDescription = "Decline and review later"
              },
          ) {
            Text("Decline")
          }
          Button(
            onClick = onAccept,
            enabled = acceptEnabled,
            modifier =
              Modifier.testTag("disclaimer_accept_button").semantics {
                contentDescription = "Accept privacy terms"
                stateDescription =
                  if (acceptEnabled) "Scroll complete" else "Scroll to the end to enable Accept"
              },
          ) {
            Text("Agree")
          }
        }
      }
    }
  }
}

@Composable
private fun DisclaimerScrollableContent(scrollState: androidx.compose.foundation.ScrollState) {
  val bulletPoints = remember {
    listOf(
      "You remain responsible for how generated content is used.",
      "Verify facts before sharing or acting on AI-produced responses.",
      "Generated content may reference third-party material; respect applicable policies.",
      "Contact support if you encounter safety issues or unexpectedly harmful outputs so we can investigate.",
      "Continuing indicates you acknowledge the privacy policy and terms of use.",
    )
  }
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 160.dp, max = 280.dp)
        .verticalScroll(scrollState)
        .testTag("disclaimer_scrollable_content"),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      bulletPoints.forEach { point ->
        Text(text = "• $point", style = MaterialTheme.typography.bodyMedium)
      }

      Text(
        text =
          "By selecting Agree and continue you confirm you have read this disclaimer and accept responsibility for your use of nanoAI.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start,
        modifier = Modifier.testTag("disclaimer_last_text"),
      )
    }
  }
}
