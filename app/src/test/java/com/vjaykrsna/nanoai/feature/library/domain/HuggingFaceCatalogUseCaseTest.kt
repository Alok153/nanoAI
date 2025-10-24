package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.data.huggingface.HuggingFaceCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HuggingFaceCatalogUseCaseTest {
  private lateinit var useCase: HuggingFaceCatalogUseCase
  private lateinit var huggingFaceCatalogRepository: HuggingFaceCatalogRepository

  @Before
  fun setup() {
    huggingFaceCatalogRepository = mockk(relaxed = true)

    useCase = HuggingFaceCatalogUseCase(huggingFaceCatalogRepository)
  }

  @Test
  fun `listModels returns success with model list`() = runTest {
    val query = HuggingFaceCatalogQuery(search = "bert", limit = 10)
    val models =
      listOf(
        HuggingFaceModelSummary(
          modelId = "bert-base",
          displayName = "BERT Base",
          author = "google",
          pipelineTag = "text-classification",
          libraryName = "transformers",
          tags = listOf("transformers", "text-classification"),
          likes = 500,
          downloads = 1000000,
          trendingScore = 100,
          createdAt = null,
          lastModified = null,
          isPrivate = false,
        )
      )
    coEvery { huggingFaceCatalogRepository.listModels(query) } returns NanoAIResult.success(models)

    val result = useCase.listModels(query)

    val returnedModels = result.assertSuccess()
    assert(returnedModels == models)
  }

  @Test
  fun `listModels returns recoverable error when repository fails`() = runTest {
    val query = HuggingFaceCatalogQuery(search = "bert", limit = 10)
    val exception = RuntimeException("API error")
    coEvery { huggingFaceCatalogRepository.listModels(query) } returns
      NanoAIResult.recoverable(message = "API error", cause = exception)

    val result = useCase.listModels(query)

    result.assertRecoverableError()
  }
}
