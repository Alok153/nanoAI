package com.vjaykrsna.nanoai.core.data.preferences

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PrivacyPreferenceStoreTest {
  private lateinit var store: PrivacyPreferenceStore

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    store = PrivacyPreferenceStore(context)
  }

  @Test
  fun `privacyPreference exposes defaults`() = runTest {
    store.reset()
    advanceUntilIdle()

    val preference = store.privacyPreference.first()

    assertThat(preference.exportWarningsDismissed).isFalse()
    assertThat(preference.telemetryOptIn).isFalse()
    assertThat(preference.consentAcknowledgedAt).isNull()
    assertThat(preference.disclaimerShownCount).isEqualTo(0)
    assertThat(preference.retentionPolicy).isEqualTo(RetentionPolicy.INDEFINITE)
  }

  @Test
  fun `acknowledgeConsent persists timestamp`() = runTest {
    store.reset()
    advanceUntilIdle()

    val expectedInstant = Instant.fromEpochMilliseconds(1_696_000_000_000)

    store.acknowledgeConsent(expectedInstant)
    advanceUntilIdle()

    val preference = store.privacyPreference.first()

    assertThat(preference.consentAcknowledgedAt).isEqualTo(expectedInstant)
  }

  @Test
  fun `incrementDisclaimerShown increases counter`() = runTest {
    store.reset()
    advanceUntilIdle()

    repeat(3) { store.incrementDisclaimerShown() }
    advanceUntilIdle()

    val preference = store.privacyPreference.first()

    assertThat(preference.disclaimerShownCount).isEqualTo(3)
  }

  @Test
  fun `disclaimerExposure defaults to requiring dialog`() = runTest {
    store.reset()
    advanceUntilIdle()

    val exposure = store.disclaimerExposure.first()

    assertThat(exposure.shouldShowDialog).isTrue()
    assertThat(exposure.acknowledged).isFalse()
    assertThat(exposure.acknowledgedAt).isNull()
    assertThat(exposure.shownCount).isEqualTo(0)
  }

  @Test
  fun `acknowledgeConsent updates disclaimerExposure`() = runTest {
    store.reset()
    advanceUntilIdle()

    val timestamp = Instant.fromEpochMilliseconds(1_700_000_000_000)

    store.incrementDisclaimerShown()
    store.acknowledgeConsent(timestamp)
    advanceUntilIdle()

    val exposure = store.disclaimerExposure.first()

    assertThat(exposure.shouldShowDialog).isFalse()
    assertThat(exposure.acknowledged).isTrue()
    assertThat(exposure.acknowledgedAt).isEqualTo(timestamp)
    assertThat(exposure.shownCount).isEqualTo(1)
  }

  @Test
  fun `resetDisclaimerExposure clears acknowledgement`() = runTest {
    store.reset()
    advanceUntilIdle()

    val timestamp = Instant.fromEpochMilliseconds(1_700_100_000_000)
    store.incrementDisclaimerShown()
    store.acknowledgeConsent(timestamp)
    advanceUntilIdle()

    store.resetDisclaimerExposure()
    advanceUntilIdle()

    val exposure = store.disclaimerExposure.first()

    assertThat(exposure.shouldShowDialog).isTrue()
    assertThat(exposure.acknowledged).isFalse()
    assertThat(exposure.acknowledgedAt).isNull()
    assertThat(exposure.shownCount).isEqualTo(0)
  }

  @Test
  fun `setTelemetryOptIn persists opt-in preference`() = runTest {
    store.reset()
    advanceUntilIdle()

    store.setTelemetryOptIn(true)
    advanceUntilIdle()

    assertThat(store.privacyPreference.first().telemetryOptIn).isTrue()

    store.setTelemetryOptIn(false)
    advanceUntilIdle()

    assertThat(store.privacyPreference.first().telemetryOptIn).isFalse()
  }

  @Test
  fun `setExportWarningsDismissed toggles preference`() = runTest {
    store.reset()
    advanceUntilIdle()

    store.setExportWarningsDismissed(true)
    advanceUntilIdle()
    assertThat(store.privacyPreference.first().exportWarningsDismissed).isTrue()

    store.setExportWarningsDismissed(false)
    advanceUntilIdle()
    assertThat(store.privacyPreference.first().exportWarningsDismissed).isFalse()
  }

  @Test
  fun `setRetentionPolicy saves and restores selection`() = runTest {
    store.reset()
    advanceUntilIdle()

    store.setRetentionPolicy(RetentionPolicy.MANUAL_PURGE_ONLY)
    advanceUntilIdle()

    assertThat(store.privacyPreference.first().retentionPolicy)
      .isEqualTo(RetentionPolicy.MANUAL_PURGE_ONLY)

    store.setRetentionPolicy(RetentionPolicy.INDEFINITE)
    advanceUntilIdle()

    assertThat(store.privacyPreference.first().retentionPolicy)
      .isEqualTo(RetentionPolicy.INDEFINITE)
  }

  @Test
  fun `acknowledgeConsent updates flows with latest timestamp`() = runTest {
    store.reset()
    advanceUntilIdle()

    val initial = Instant.fromEpochMilliseconds(1_700_500_000_000)
    val latest = Instant.fromEpochMilliseconds(1_700_600_000_000)

    store.acknowledgeConsent(initial)
    advanceUntilIdle()
    store.acknowledgeConsent(latest)
    advanceUntilIdle()

    val preference = store.privacyPreference.first()
    val exposure = store.disclaimerExposure.first()

    assertThat(preference.consentAcknowledgedAt).isEqualTo(latest)
    assertThat(exposure.acknowledgedAt).isEqualTo(latest)
    assertThat(exposure.shouldShowDialog).isFalse()
    assertThat(exposure.acknowledged).isTrue()
  }
}
