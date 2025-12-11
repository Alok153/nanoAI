package com.vjaykrsna.nanoai.feature.settings.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.usecase.UpdatePrivacyPreferencesUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPrivacyActionsTest {

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)
  private lateinit var updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase
  private lateinit var errors: MutableList<NanoAIErrorEnvelope>
  private lateinit var clock: Clock
  private lateinit var actions: SettingsPrivacyActions

  @BeforeEach
  fun setUp() {
    updatePrivacyPreferencesUseCase = mockk(relaxed = true)
    errors = mutableListOf()
    clock =
      object : Clock {
        override fun now(): Instant = Instant.parse("2025-01-01T00:00:00Z")
      }

    actions =
      SettingsPrivacyActions(
        scope = testScope,
        updatePrivacyPreferencesUseCase = updatePrivacyPreferencesUseCase,
        emitError = { error -> errors.add(error) },
        clock = clock,
      )
  }

  @Test
  fun `setTelemetryOptIn calls use case`() =
    testScope.runTest {
      coEvery { updatePrivacyPreferencesUseCase.setTelemetryOptIn(true) } just Runs

      actions.setTelemetryOptIn(true)
      advanceUntilIdle()

      coVerify { updatePrivacyPreferencesUseCase.setTelemetryOptIn(true) }
      assertThat(errors).isEmpty()
    }

  @Test
  fun `setTelemetryOptIn emits error on failure`() =
    testScope.runTest {
      coEvery { updatePrivacyPreferencesUseCase.setTelemetryOptIn(any()) } throws
        RuntimeException("Failed")

      actions.setTelemetryOptIn(true)
      advanceUntilIdle()

      assertThat(errors).hasSize(1)
    }

  @Test
  fun `acknowledgeConsent calls use case with current time`() =
    testScope.runTest {
      coEvery { updatePrivacyPreferencesUseCase.acknowledgeConsent(any()) } just Runs

      actions.acknowledgeConsent()
      advanceUntilIdle()

      coVerify { updatePrivacyPreferencesUseCase.acknowledgeConsent(clock.now()) }
      assertThat(errors).isEmpty()
    }

  @Test
  fun `acknowledgeConsent emits error on failure`() =
    testScope.runTest {
      coEvery { updatePrivacyPreferencesUseCase.acknowledgeConsent(any()) } throws
        RuntimeException("Failed")

      actions.acknowledgeConsent()
      advanceUntilIdle()

      assertThat(errors).hasSize(1)
    }

  @Test
  fun `setRetentionPolicy calls use case`() =
    testScope.runTest {
      val policy = RetentionPolicy.MANUAL_PURGE_ONLY
      coEvery { updatePrivacyPreferencesUseCase.setRetentionPolicy(policy) } just Runs

      actions.setRetentionPolicy(policy)
      advanceUntilIdle()

      coVerify { updatePrivacyPreferencesUseCase.setRetentionPolicy(policy) }
      assertThat(errors).isEmpty()
    }

  @Test
  fun `setRetentionPolicy emits error on failure`() =
    testScope.runTest {
      coEvery { updatePrivacyPreferencesUseCase.setRetentionPolicy(any()) } throws
        RuntimeException("Failed")

      actions.setRetentionPolicy(RetentionPolicy.INDEFINITE)
      advanceUntilIdle()

      assertThat(errors).hasSize(1)
    }

  @Test
  fun `dismissExportWarnings calls use case`() =
    testScope.runTest {
      coEvery { updatePrivacyPreferencesUseCase.setExportWarningsDismissed(true) } just Runs

      actions.dismissExportWarnings()
      advanceUntilIdle()

      coVerify { updatePrivacyPreferencesUseCase.setExportWarningsDismissed(true) }
      assertThat(errors).isEmpty()
    }

  @Test
  fun `dismissExportWarnings emits error on failure`() =
    testScope.runTest {
      coEvery { updatePrivacyPreferencesUseCase.setExportWarningsDismissed(any()) } throws
        RuntimeException("Failed")

      actions.dismissExportWarnings()
      advanceUntilIdle()

      assertThat(errors).hasSize(1)
    }
}
