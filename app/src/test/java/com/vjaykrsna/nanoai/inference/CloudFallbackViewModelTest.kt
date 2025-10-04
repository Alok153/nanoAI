package com.vjaykrsna.nanoai.inference

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit spec for CloudFallbackViewModel error handling (Scenario 6).
 *
 * Expectations (red until fallback orchestration implemented):
 * - ViewModel exposes a RecoverableError state with telemetry ID metadata when local runtime
 *   declines.
 * - Retry events surface structured guidance (minutes / next steps).
 * - Fatal errors bypass retry and surface support contact metadata.
 */
class CloudFallbackViewModelTest {
  @Test
  fun `recoverable error emits telemetry metadata and retry guidance`() {
    val viewModelClass = runCatching {
      Class.forName("com.vjaykrsna.nanoai.inference.CloudFallbackViewModel")
    }
    assertThat(viewModelClass.isSuccess).isTrue()

    val stateClass = runCatching {
      Class.forName("com.vjaykrsna.nanoai.inference.CloudFallbackViewModel\$State")
    }
    assertThat(stateClass.isSuccess).isTrue()

    val state = stateClass.getOrNull()?.declaredFields?.associateBy { it.name }
    assertThat(state).isNotNull()
    assertThat(state?.keys).containsAtLeast("error", "telemetryId", "retryInSeconds")
  }

  @Test
  fun `fatal error surfaces support escalation metadata`() {
    val resultClass = runCatching {
      Class.forName("com.vjaykrsna.nanoai.core.common.NanoAIResult\$FatalError")
    }
    assertThat(resultClass.isSuccess).isTrue()

    val fields = resultClass.getOrNull()?.declaredFields?.map { it.name }
    assertThat(fields).isNotNull()
    assertThat(fields).contains("supportContact")
  }
}
