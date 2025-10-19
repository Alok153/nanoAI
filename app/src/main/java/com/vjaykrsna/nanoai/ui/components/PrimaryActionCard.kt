package com.vjaykrsna.nanoai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R

@Composable
fun PrimaryActionCard(
  title: String,
  description: String,
  tag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // String resources
  val contentDescriptionFormat = stringResource(R.string.primary_action_card_content_description)

  val haptics = LocalHapticFeedback.current

  Card(
    onClick = {
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      onClick()
    },
    modifier =
      modifier.fillMaxWidth().testTag(tag).semantics {
        contentDescription = contentDescriptionFormat.format(title)
        stateDescription = "$description"
        role = Role.Button
      },
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(
          text = stringResource(R.string.primary_action_card_run),
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.padding(start = 4.dp),
        )
      }
    }
  }
}
