package com.vjaykrsna.nanoai.core.domain.library

import io.mockk.mockk
import org.junit.Before

internal abstract class ModelDownloadsAndExportUseCaseTestBase {
  protected lateinit var useCase: ModelDownloadsAndExportUseCase
  protected lateinit var modelCatalogRepository: ModelCatalogRepository
  protected lateinit var downloadManager: DownloadManager
  protected lateinit var exportService: ExportService

  @Before
  fun setupBase() {
    modelCatalogRepository = mockk(relaxed = true)
    downloadManager = mockk(relaxed = true)
    exportService = mockk(relaxed = true)

    useCase =
      ModelDownloadsAndExportUseCase(
        modelCatalogRepository = modelCatalogRepository,
        downloadManager = downloadManager,
        exportService = exportService,
      )
  }
}
