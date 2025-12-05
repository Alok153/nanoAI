package com.vjaykrsna.nanoai.core.network.dto

import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class CompletionRequestDtoTest {

  private val sampleMessage = CompletionMessageDto(role = CompletionRole.USER, content = "hi")

  @Test
  fun `constructing request validates optional bounds`() {
    val request =
      CompletionRequestDto(
        model = "nano",
        messages = listOf(sampleMessage),
        temperature = 1.5,
        topP = 0.5,
        maxTokens = 1024,
      )

    assert(request.stream == false)
    assert(request.metadata == null)
  }

  @Test
  fun `messages must not be empty`() {
    assertFailsWith<IllegalArgumentException> { CompletionRequestDto("nano", emptyList()) }
  }

  @Test
  fun `rejects temperatures outside supported range`() {
    assertFailsWith<IllegalArgumentException> {
      CompletionRequestDto(model = "nano", messages = listOf(sampleMessage), temperature = 3.0)
    }
  }

  @Test
  fun `rejects topP outside supported range`() {
    assertFailsWith<IllegalArgumentException> {
      CompletionRequestDto(model = "nano", messages = listOf(sampleMessage), topP = -0.1)
    }
  }

  @Test
  fun `rejects max tokens outside supported range`() {
    assertFailsWith<IllegalArgumentException> {
      CompletionRequestDto(model = "nano", messages = listOf(sampleMessage), maxTokens = 0)
    }
  }
}
