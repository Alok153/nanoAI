package com.vjaykrsna.nanoai.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * TDD contract for the upcoming NanoAIResult sealed hierarchy.
 *
 * These assertions intentionally fail until the production code introduces the sealed result with
 * Success and RecoverableError subtypes exposing telemetry metadata and retry hints.
 */
class NanoAIResultTest {
  @Test
  fun `NanoAIResult sealed type should exist`() {
    val result = runCatching { Class.forName("com.vjaykrsna.nanoai.core.common.NanoAIResult") }
    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun `NanoAIResult should expose Success subtype`() {
    val successClass = runCatching {
      Class.forName("com.vjaykrsna.nanoai.core.common.NanoAIResult\$Success")
    }
    assertThat(successClass.isSuccess).isTrue()
  }

  @Test
  fun `RecoverableError should expose retry metadata`() {
    val recoverableClass =
      Class.forName("com.vjaykrsna.nanoai.core.common.NanoAIResult\$RecoverableError")
    val fieldNames = recoverableClass.declaredFields.map { it.name }

    assertThat(fieldNames).containsAtLeast("message", "retryAfterSeconds", "telemetryId")
  }
}
