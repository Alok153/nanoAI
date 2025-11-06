package com.vjaykrsna.nanoai.feature.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.library.Model

@Composable
fun ModelPicker(
  models: List<Model>,
  selectedModelId: String?,
  onModelSelect: (Model) -> Unit,
  onManageModelsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.padding(16.dp)) {
    Text(
      text = "Select a Model",
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(bottom = 16.dp),
    )
    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
      items(models) { model ->
        Row(
          modifier =
            Modifier.fillMaxWidth().clickable { onModelSelect(model) }.padding(vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = model.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
              text = "${model.size} | ${model.parameter}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (model.modelId == selectedModelId) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Selected",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp),
            )
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onManageModelsClick, modifier = Modifier.fillMaxWidth()) {
      Text("Manage Models")
    }
  }
}
