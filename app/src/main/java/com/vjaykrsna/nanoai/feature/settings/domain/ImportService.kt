package com.vjaykrsna.nanoai.feature.settings.domain

import android.net.Uri

interface ImportService {
    suspend fun importBackup(uri: Uri): Result<ImportSummary>
}
