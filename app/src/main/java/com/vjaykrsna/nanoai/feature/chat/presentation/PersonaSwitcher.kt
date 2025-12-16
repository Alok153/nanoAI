package com.vjaykrsna.nanoai.feature.chat.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import java.util.UUID

@Composable
fun PersonaSwitcher(
  uiState: PersonaSwitcherUiState,
  onDismiss: () -> Unit,
  onContinueThread: (UUID) -> Unit,
  onStartNewThread: (UUID) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier.fillMaxWidth().padding(horizontal = NanoSpacing.lg, vertical = NanoSpacing.md),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
  ) {
    Text(
      text = "Personas",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.fillMaxWidth(),
    )

    if (uiState.errorMessage != null) {
      Text(
        text = uiState.errorMessage,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
      )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm)) {
      items(uiState.personas, key = { it.personaId }) { persona ->
        PersonaSwitcherCard(
          persona = persona,
          isSelected = persona.personaId == uiState.selectedPersonaId,
          canContinue = uiState.activeThreadId != null && !uiState.isSwitching,
          isSwitching = uiState.isSwitching,
          onContinueThread = { onContinueThread(persona.personaId) },
          onStartNewThread = { onStartNewThread(persona.personaId) },
        )
      }
    }

    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
      Text(text = "Close")
    }
  }
}

@Composable
private fun PersonaSwitcherCard(
  persona: PersonaProfile,
  isSelected: Boolean,
  canContinue: Boolean,
  isSwitching: Boolean,
  onContinueThread: () -> Unit,
  onStartNewThread: () -> Unit,
) {
  val cardDescription =
    if (isSelected) "Selected persona ${persona.name}" else "Persona ${persona.name}"
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    modifier = Modifier.fillMaxWidth().semantics { contentDescription = cardDescription },
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(NanoSpacing.md)) {
      Text(text = persona.name, style = MaterialTheme.typography.titleSmall)
      Text(
        text = persona.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = NanoSpacing.xs),
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = NanoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
      ) {
        OutlinedButton(
          enabled = canContinue,
          onClick = onContinueThread,
          modifier = Modifier.weight(1f),
        ) {
          Text(text = "Continue thread")
        }

        Button(
          enabled = !isSwitching,
          onClick = onStartNewThread,
          modifier = Modifier.weight(1f),
        ) {
          Text(text = "New thread")
        }
      }
    }
  }
}
