package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Dialog displayed on first launch to remind users about responsible usage of AI-generated content.
 */
@Composable
fun FirstLaunchDisclaimerDialog(
    isVisible: Boolean,
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Use nanoAI responsibly",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = "AI responses may be inaccurate or incomplete. You're responsible for verifying nanoAI outputs before using them.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onAcknowledge,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Acknowledge disclaimer and continue"
                    },
            ) {
                Text("Acknowledge")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Dismiss disclaimer"
                    },
            ) {
                Text("Dismiss")
            }
        },
    )
}
