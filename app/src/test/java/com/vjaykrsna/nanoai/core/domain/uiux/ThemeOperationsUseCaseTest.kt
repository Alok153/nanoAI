package com.vjaykrsna.nanoai.core.domain.uiux

import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.repository.ThemeRepository
import com.vjaykrsna.nanoai.testing.assertIsSuccess
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.io.IOException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ThemeOperationsUseCaseTest {

  private lateinit var repository: ThemeRepository
  private lateinit var useCase: ThemeOperationsUseCase

  @BeforeEach
  fun setUp() {
    repository = mockk(relaxed = true)
    useCase = ThemeOperationsUseCase(repository)
  }

  @Test
  fun `updateTheme returns success when repository succeeds`() = runTest {
    coEvery { repository.updateThemePreference(any()) } just runs

    val result = useCase.updateTheme(ThemePreference.SYSTEM)

    result.assertIsSuccess()
  }

  @Test
  fun `updateTheme returns recoverable when repository throws io exception`() = runTest {
    coEvery { repository.updateThemePreference(any()) } throws IOException("disk full")

    val result = useCase.updateTheme(ThemePreference.DARK)

    result.assertRecoverableError()
  }

  @Test
  fun `updateVisualDensity returns recoverable when repository throws illegal state`() = runTest {
    coEvery { repository.updateVisualDensity(any()) } throws
      IllegalStateException("datastore locked")

    val result = useCase.updateVisualDensity(VisualDensity.DEFAULT)

    result.assertRecoverableError()
  }

  @Test
  fun `updateHighContrastEnabled returns recoverable when repository throws io exception`() =
    runTest {
      coEvery { repository.updateHighContrastEnabled(any()) } throws IOException("io error")

      val result = useCase.updateHighContrastEnabled(true)

      result.assertRecoverableError()
    }

  @Test
  fun `updateTheme rethrows cancellation exception`() = runTest {
    coEvery { repository.updateThemePreference(any()) } throws CancellationException("cancelled")

    assertFailsWith<CancellationException> { useCase.updateTheme(ThemePreference.LIGHT) }
  }
}
