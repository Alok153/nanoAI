package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsError
import com.vjaykrsna.nanoai.feature.settings.presentation.model.SettingsUiEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun CollectSettingsEvents(
  events: Flow<SettingsUiEvent>,
  snackbarHostState: SnackbarHostState,
) {
  LaunchedEffect(Unit) {
    events.collectLatest { event ->
      when (event) {
        is SettingsUiEvent.ErrorRaised ->
          snackbarHostState.showSnackbar(event.error.toUserMessage())
        is SettingsUiEvent.ExportCompleted ->
          snackbarHostState.showSnackbar(exportSuccessMessage(event.destinationPath))
        is SettingsUiEvent.ImportCompleted ->
          snackbarHostState.showSnackbar(buildImportSuccessMessage(event.summary))
      }
    }
  }
}

private fun SettingsError.toUserMessage(): String =
  when (this) {
    is SettingsError.ProviderAddFailed -> "Failed to add provider: $message"
    is SettingsError.ProviderUpdateFailed -> "Failed to update provider: $message"
    is SettingsError.ProviderDeleteFailed -> "Failed to delete provider: $message"
    is SettingsError.ExportFailed -> "Export failed: $message"
    is SettingsError.ImportFailed -> "Import failed: $message"
    is SettingsError.PreferenceUpdateFailed -> "Failed to update preference: $message"
    is SettingsError.UnexpectedError -> "Unexpected error: $message"
    is SettingsError.HuggingFaceAuthFailed -> "Hugging Face authentication failed: $message"
  }

private fun exportSuccessMessage(path: String): String =
  "Backup exported to ${path.substringAfterLast('/')}…"

private fun buildImportSuccessMessage(summary: ImportSummary): String {
  val personasTotal = summary.personasImported + summary.personasUpdated
  val providersTotal = summary.providersImported + summary.providersUpdated
  return buildString {
    append("Imported backup: ")
    append("$personasTotal persona${if (personasTotal == 1) "" else "s"}")
    append(", ")
    append("$providersTotal provider${if (providersTotal == 1) "" else "s"}")
  }
}
