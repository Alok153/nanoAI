package com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
  modifier: Modifier = Modifier,
  title: String? = null,
  subtitle: String? = null,
  supportingText: String? = null,
  icon: ImageVector? = null,
  badge: String? = null,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null,
  trailingContent: (@Composable RowScope.() -> Unit)? = null,
  supportingContent: (@Composable () -> Unit)? = null,
  semanticsDescription: String? = null,
  content: (@Composable () -> Unit)? = null,
) {
  val semanticsModifier =
    modifier.cardSemantics(onClick, semanticsDescription, title, subtitle, supportingText)
  val shape = RoundedCornerShape(NanoRadii.large)
  val innerContent =
    content
      ?: {
        DefaultNanoCardContent(
          title = title,
          subtitle = subtitle,
          supportingText = supportingText,
          icon = icon,
          badge = badge,
          trailingContent = trailingContent,
          supportingContent = supportingContent,
        )
      }

  if (onClick != null) {
    Surface(
      modifier = semanticsModifier,
      onClick = onClick,
      enabled = enabled,
      shape = shape,
      tonalElevation = NanoElevation.level2,
    ) {
      innerContent()
    }
  } else {
    Surface(modifier = semanticsModifier, shape = shape, tonalElevation = NanoElevation.level1) {
      innerContent()
    }
  }
}

private fun Modifier.cardSemantics(
  onClick: (() -> Unit)?,
  semanticsDescription: String?,
  title: String?,
  subtitle: String?,
  supportingText: String?,
): Modifier {
  val defaultDescription =
    listOfNotNull(
        title?.takeIf { it.isNotBlank() },
        subtitle?.takeIf { it.isNotBlank() },
        supportingText?.takeIf { it.isNotBlank() },
      )
      .joinToString(separator = ", ")

  val description = semanticsDescription?.takeIf { it.isNotBlank() } ?: defaultDescription
  if (description.isBlank() && onClick == null) return this

  return semantics {
    if (onClick != null) role = Role.Button
    if (description.isNotBlank()) contentDescription = description
  }
}

@Composable
private fun DefaultNanoCardContent(
  title: String?,
  subtitle: String?,
  supportingText: String?,
  icon: ImageVector?,
  badge: String?,
  trailingContent: (@Composable RowScope.() -> Unit)?,
  supportingContent: (@Composable () -> Unit)?,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth().padding(horizontal = NanoSpacing.lg, vertical = NanoSpacing.md),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
  ) {
    NanoCardHeader(title, subtitle, icon, badge, trailingContent)
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

@Composable
private fun NanoCardHeader(
  title: String?,
  subtitle: String?,
  icon: ImageVector?,
  badge: String?,
  trailingContent: (@Composable RowScope.() -> Unit)?,
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
        Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        title?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
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
      trailingContent?.invoke(this)
    }
  }
}
