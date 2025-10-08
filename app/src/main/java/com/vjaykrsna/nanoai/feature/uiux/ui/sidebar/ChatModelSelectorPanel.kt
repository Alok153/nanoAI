package com.vjaykrsna.nanoai.feature.uiux.ui.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import java.util.UUID

@Composable
internal fun ChatModelSelectorPanel(
  availablePersonas: List<PersonaProfile>,
  currentPersonaId: UUID?,
  onPersonaSelect: (PersonaProfile) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.testTag("chat_model_selector_panel")
  ) {
    Text(
      text = "Select AI Model",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )

    availablePersonas.forEach { persona ->
      val isSelected = persona.personaId == currentPersonaId
      Button(
        onClick = { onPersonaSelect(persona) },
        modifier =
          Modifier.fillMaxWidth().semantics {
            contentDescription =
              "Select ${persona.name} model${if (isSelected) " (currently selected)" else ""}"
          },
        enabled = !isSelected,
      ) {
        Text(persona.name)
      }
    }
  }
}
