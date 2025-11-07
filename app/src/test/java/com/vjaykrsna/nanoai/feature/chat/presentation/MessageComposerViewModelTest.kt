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

    assertThat(viewModel.messageText.value).isEqualTo("Hello world")
  }

  @Test
  fun `sendMessage sets sending state during operation`() = runTest {
    viewModel.updateMessageText("Test")

    viewModel.isSending.test {
      assertThat(awaitItem()).isFalse()

      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())
      assertThat(awaitItem()).isTrue()
      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `clearMessage resets the message text`() = runTest {
    viewModel.updateMessageText("Some text")
    viewModel.clearMessage()

    assertThat(viewModel.messageText.value).isEmpty()
  }
}

class MessageComposerViewModelSendTest : MessageComposerViewModelTestBase() {

  @JvmField @RegisterExtension val mainDispatcherExtension = dispatcherExtension

  @Test
  fun `sendMessage with empty text emits EmptyMessage error`() = runTest {
    viewModel.errors.test {
      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())

      val error = awaitItem()
      assertThat(error).isInstanceOf(MessageComposerError.EmptyMessage::class.java)
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(viewModel.isSending.value).isFalse()
  }

  @Test
  fun `sendMessage with whitespace only text emits EmptyMessage error`() = runTest {
    viewModel.updateMessageText("   ")
    viewModel.errors.test {
      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())

      val error = awaitItem()
      assertThat(error).isInstanceOf(MessageComposerError.EmptyMessage::class.java)
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(viewModel.isSending.value).isFalse()
  }

  @Test
  fun `sendMessage with valid text calls use case and clears text on success`() = runTest {
    viewModel.updateMessageText("Valid message")

    viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())
    runCurrent()

    assertThat(viewModel.messageText.value).isEmpty()
    assertThat(viewModel.isSending.value).isFalse()
  }

  @Test
  fun `sendMessage emits SendFailed error on use case failure`() = runTest {
    coEvery { sendPromptUseCase(any(), any(), any()) } returns
      NanoAIResult.recoverable(message = "Network error")

    viewModel.updateMessageText("Test message")

    viewModel.errors.test {
      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())

      val error = awaitItem()
      assertThat(error).isInstanceOf(MessageComposerError.SendFailed::class.java)
      assertThat((error as MessageComposerError.SendFailed).message).isEqualTo("Network error")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `sendMessage emits SendFailed with unknown error on failure without message`() = runTest {
    coEvery { sendPromptUseCase(any(), any(), any()) } returns
      NanoAIResult.recoverable(message = "")

    viewModel.updateMessageText("Test message")

    viewModel.errors.test {
      viewModel.sendMessage(UUID.randomUUID(), UUID.randomUUID())

      val error = awaitItem()
      assertThat(error).isInstanceOf(MessageComposerError.SendFailed::class.java)
      assertThat((error as MessageComposerError.SendFailed).message).isEqualTo("")
      cancelAndIgnoreRemainingEvents()
    }
  }
}
