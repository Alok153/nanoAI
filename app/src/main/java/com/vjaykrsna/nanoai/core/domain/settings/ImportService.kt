package com.vjaykrsna.nanoai.core.domain.settings

import android.net.Uri

interface ImportService {
  suspend fun importBackup(uri: Uri): Result<ImportSummary>
}
