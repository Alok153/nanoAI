package com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoRadii
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

@Composable
fun NanoCard(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  supportingText: String? = null,
  icon: ImageVector? = null,
  badge: String? = null,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null,
  trailingContent: (@Composable () -> Unit)? = null,
  supportingContent: (@Composable () -> Unit)? = null,
  semanticsDescription: String? = null,
) {
  val defaultDescription =
    listOfNotNull(
        title.takeIf { it.isNotBlank() },
        subtitle?.takeIf { it.isNotBlank() },
        supportingText?.takeIf { it.isNotBlank() },
      )
      .joinToString(separator = ", ")

  val semanticsModifier =
    modifier.semantics {
      if (onClick != null) {
        role = Role.Button
      }

      val description = semanticsDescription?.takeIf { it.isNotBlank() } ?: defaultDescription

      if (description.isNotBlank()) {
        contentDescription = description
      }
    }

  val shape = RoundedCornerShape(NanoRadii.large)
  val content: @Composable () -> Unit = {
    Column(
      modifier =
        Modifier.fillMaxWidth().padding(horizontal = NanoSpacing.lg, vertical = NanoSpacing.md),
      verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
        ) {
          icon?.let {
            Icon(
              imageVector = it,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let { subtitleText ->
              Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          badge?.let { badgeText -> Badge { Text(badgeText) } }
          trailingContent?.invoke()
        }
      }

      supportingText?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      supportingContent?.invoke()
    }
  }

  if (onClick != null) {
    Surface(
      modifier = semanticsModifier,
      onClick = onClick,
      enabled = enabled,
      shape = shape,
      tonalElevation = NanoElevation.level2,
    ) {
      content()
    }
  } else {
    Surface(
      modifier = semanticsModifier,
      shape = shape,
      tonalElevation = NanoElevation.level1,
    ) {
      content()
    }
  }
}
