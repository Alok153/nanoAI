package com.vjaykrsna.nanoai.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NanoAIResultAliasTest {
  @Test
  fun successAlias_returnsUnderlyingValue() {
    val result: NanoAIResult<String> = NanoAIResultSuccess("ok")

    assertThat(result).isInstanceOf(NanoAISuccess::class.java)
    val value = (result as NanoAISuccess<String>).value
    assertThat(value).isEqualTo("ok")
  }

  @Test
  fun recoverableAlias_matchesUnderlyingType() {
    val error: NanoAIResult<Nothing> =
      NanoAIRecoverableError(message = "network", retryAfterSeconds = 1, telemetryId = "NET-1")

    assertThat(error).isInstanceOf(NanoAIRecoverableError::class.java)
  }
}
