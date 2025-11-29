package com.vjaykrsna.nanoai.core.domain.uiux

import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsOperationsUseCaseTest {
  private lateinit var useCase: SettingsOperationsUseCase
  private lateinit var repository: UserProfileRepository
  private val dispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setup() {
    repository = mockk<UserProfileRepository>(relaxed = true)

    useCase = SettingsOperationsUseCase(repository, dispatcher)
  }

  @Test
  fun `updateTheme succeeds and calls repository with correct parameters`() =
    runTest(dispatcher) {
      val theme = ThemePreference.DARK
      val userId = "test-user"

      coEvery { repository.updateThemePreference(userId, theme.name) } returns Unit

      val result = useCase.updateTheme(theme, userId)

      result.assertSuccess()
    }

  @Test
  fun `updateTheme returns recoverable error when repository throws exception`() =
    runTest(dispatcher) {
      val theme = ThemePreference.LIGHT
      val exception = RuntimeException("Repository error")

      coEvery { repository.updateThemePreference(any(), any()) } throws exception

      val result = useCase.updateTheme(theme)

      result.assertRecoverableError()
    }

  @Test
  fun `updateVisualDensity succeeds and calls repository with correct parameters`() =
    runTest(dispatcher) {
      val density = VisualDensity.COMPACT
      val userId = "test-user"

      coEvery { repository.updateVisualDensity(userId, density.name) } returns Unit

      val result = useCase.updateVisualDensity(density, userId)

      result.assertSuccess()
    }

  @Test
  fun `updateVisualDensity returns recoverable error when repository throws exception`() =
    runTest(dispatcher) {
      val density = VisualDensity.DEFAULT
      val exception = RuntimeException("Repository error")

      coEvery { repository.updateVisualDensity(any(), any()) } throws exception

      val result = useCase.updateVisualDensity(density)

      result.assertRecoverableError()
    }
}
