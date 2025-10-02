package com.vjaykrsna.nanoai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun OfflineBanner(
    isOffline: Boolean,
    queuedActions: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isOffline) return

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("offline_banner_container"),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .semantics { contentDescription = "Offline status banner" },
        ) {
            Text(
                text = "You're offline. Some features are temporarily unavailable.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("offline_banner_message"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Actions will resume when you're back online.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("offline_banner_disabled_actions"),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Queued: $queuedActions",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.testTag("offline_banner_queue_status"),
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("offline_banner_retry"),
                ) {
                    Text("Retry now")
                }
            }
        }
    }
}
