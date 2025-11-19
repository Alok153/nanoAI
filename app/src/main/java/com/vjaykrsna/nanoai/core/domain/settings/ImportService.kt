package com.vjaykrsna.nanoai.core.domain.settings

import android.net.Uri
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot

interface ImportService {
  @OneShot("Import backup bundle from local storage")
  suspend fun importBackup(uri: Uri): NanoAIResult<ImportSummary>
}
