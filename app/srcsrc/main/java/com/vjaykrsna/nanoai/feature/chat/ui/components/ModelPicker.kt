package com.vjaykrsna.nanoai.feature.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.library.domain.model.Model

@Composable
fun ModelPicker(
  models: List<Model>,
  onModelSelect: (Model) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Select a Model",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 16.dp),
      )
      LazyColumn {
        items(models) { model ->
          Row(
            modifier =
              Modifier.fillMaxWidth().clickable { onModelSelect(model) }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(text = model.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
              text = "${model.size} | ${model.parameter}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
