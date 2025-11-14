package com.vjaykrsna.nanoai.feature.chat.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.chat.SendPromptUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

abstract class MessageComposerViewModelTestBase {

  protected val dispatcherExtension = MainDispatcherExtension()
  protected lateinit var sendPromptUseCase: SendPromptUseCase
  protected lateinit var viewModel: MessageComposerViewModel

  @BeforeEach
  fun setUpBase() {
    sendPromptUseCase = mockk(relaxed = true)
    coEvery { sendPromptUseCase(any(), any(), any()) } returns NanoAIResult.success(Unit)
    viewModel = MessageComposerViewModel(sendPromptUseCase, dispatcherExtension.dispatcher)
  }
}

class MessageComposerViewModelStateTest : MessageComposerViewModelTestBase() {

  @JvmField @RegisterExtension val mainDispatcherExtension = dispatcherExtension

  @Test
  fun `updateMessageText updates the message text state`() = runTest {
    viewModel.updateMessageText("Hello world")

    assertThat(viewModel.state.value.messageText).isEqualTo("Hello world")
  }

  @Test
  fun `sendMessage sets sending state during operation`() = runTest {
    viewModel.updateMessageText("Test")

    viewModel.state.test {
      assertThat(awaitItem().isSending).isFalse()

      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())
      assertThat(awaitItem().isSending).isTrue()
      assertThat(awaitItem().isSending).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `clearMessage resets the message text`() = runTest {
    viewModel.updateMessageText("Some text")
    viewModel.clearMessage()

    assertThat(viewModel.state.value.messageText).isEmpty()
  }
}

class MessageComposerViewModelSendTest : MessageComposerViewModelTestBase() {

  @JvmField @RegisterExtension val mainDispatcherExtension = dispatcherExtension

  @Test
  fun `sendMessage with empty text emits EmptyMessage error`() = runTest {
    viewModel.events.test {
      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())

      val event = awaitItem() as MessageComposerUiEvent.ErrorRaised
      assertThat(event.error).isInstanceOf(MessageComposerError.EmptyMessage::class.java)
      assertThat(event.envelope.userMessage).isEqualTo("Message cannot be empty")
      cancelAndIgnoreRemainingEvents()
    }

    val state = viewModel.state.value
    assertThat(state.isSending).isFalse()
    assertThat(state.sendError).isNull()
  }

  @Test
  fun `sendMessage with whitespace only text emits EmptyMessage error`() = runTest {
    viewModel.updateMessageText("   ")
    viewModel.events.test {
      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())

      val event = awaitItem() as MessageComposerUiEvent.ErrorRaised
      assertThat(event.error).isInstanceOf(MessageComposerError.EmptyMessage::class.java)
      assertThat(event.envelope.userMessage).isEqualTo("Message cannot be empty")
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(viewModel.state.value.isSending).isFalse()
  }

  @Test
  fun `sendMessage with valid text calls use case and clears text on success`() = runTest {
    viewModel.updateMessageText("Valid message")

    viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())
    runCurrent()

    assertThat(viewModel.state.value.messageText).isEmpty()
    assertThat(viewModel.state.value.isSending).isFalse()
  }

  @Test
  fun `sendMessage emits SendFailed error on use case failure`() = runTest {
    coEvery { sendPromptUseCase(any(), any(), any()) } returns
      NanoAIResult.recoverable(message = "Network error")

    viewModel.updateMessageText("Test message")

    viewModel.events.test {
      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())

      val event = awaitItem() as MessageComposerUiEvent.ErrorRaised
      assertThat(event.error).isInstanceOf(MessageComposerError.SendFailed::class.java)
      assertThat(event.envelope.userMessage).isEqualTo("Network error")
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(viewModel.state.value.sendError).isEqualTo("Network error")
  }

  @Test
  fun `sendMessage emits SendFailed with unknown error on failure without message`() = runTest {
    coEvery { sendPromptUseCase(any(), any(), any()) } returns
      NanoAIResult.recoverable(message = "")

    viewModel.updateMessageText("Test message")

    viewModel.events.test {
      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())

      val event = awaitItem() as MessageComposerUiEvent.ErrorRaised
      assertThat(event.error).isInstanceOf(MessageComposerError.SendFailed::class.java)
      assertThat(event.envelope.userMessage).isEqualTo("Failed to send message")
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(viewModel.state.value.sendError).isEqualTo("Failed to send message")
  }
}
