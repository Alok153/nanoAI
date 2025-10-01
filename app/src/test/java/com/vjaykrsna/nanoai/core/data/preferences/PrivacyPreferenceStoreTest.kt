package com.vjaykrsna.nanoai.core.data.preferences

import android.os.Build
import com.google.common.truth.Truth.assertThat
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
    fun `privacyPreference exposes defaults`() =
        runTest {
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
    fun `acknowledgeConsent persists timestamp`() =
        runTest {
            store.reset()
            advanceUntilIdle()

            val expectedInstant = Instant.fromEpochMilliseconds(1_696_000_000_000)

            store.acknowledgeConsent(expectedInstant)
            advanceUntilIdle()

            val preference = store.privacyPreference.first()

            assertThat(preference.consentAcknowledgedAt).isEqualTo(expectedInstant)
        }

    @Test
    fun `incrementDisclaimerShown increases counter`() =
        runTest {
            store.reset()
            advanceUntilIdle()

            repeat(3) { store.incrementDisclaimerShown() }
            advanceUntilIdle()

            val preference = store.privacyPreference.first()

            assertThat(preference.disclaimerShownCount).isEqualTo(3)
        }
}
