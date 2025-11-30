package com.vjaykrsna.nanoai.core.domain.uiux

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowHeightClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowSizeClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowWidthClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.core.domain.repository.NavigationRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationOperationsUseCaseTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  private lateinit var repository: NavigationRepository
  private lateinit var useCase: NavigationOperationsUseCase

  private val commandPaletteFlow = MutableStateFlow(CommandPaletteState())
  private val recentActivityFlow = MutableStateFlow<List<RecentActivityItem>>(emptyList())
  private val windowSizeClassFlow = MutableStateFlow(ShellWindowSizeClass.Default)
  private val undoPayloadFlow = MutableStateFlow<UndoPayload?>(null)

  @BeforeEach
  fun setUp() {
    repository = mockk(relaxed = true)
    every { repository.commandPaletteState } returns commandPaletteFlow
    every { repository.recentActivity } returns recentActivityFlow
    every { repository.windowSizeClass } returns windowSizeClassFlow
    every { repository.undoPayload } returns undoPayloadFlow

    useCase = NavigationOperationsUseCase(repository, dispatcher)
  }

  @Test
  fun `commandPaletteState exposes repository flow`() =
    runTest(dispatcher) {
      val expected = CommandPaletteState(query = "test")
      commandPaletteFlow.value = expected

      val actual = useCase.commandPaletteState.first()

      assertThat(actual).isEqualTo(expected)
    }

  @Test
  fun `recentActivity exposes repository flow`() =
    runTest(dispatcher) {
      val items =
        listOf(
          RecentActivityItem(
            id = "1",
            modeId = ModeId.HOME,
            title = "Test",
            timestamp = java.time.Instant.now(),
            status = RecentStatus.COMPLETED,
          )
        )
      recentActivityFlow.value = items

      val actual = useCase.recentActivity.first()

      assertThat(actual).isEqualTo(items)
    }

  @Test
  fun `windowSizeClass exposes repository flow`() =
    runTest(dispatcher) {
      val sizeClass =
        ShellWindowSizeClass(
          widthSizeClass = ShellWindowWidthClass.EXPANDED,
          heightSizeClass = ShellWindowHeightClass.MEDIUM,
        )
      windowSizeClassFlow.value = sizeClass

      val actual = useCase.windowSizeClass.first()

      assertThat(actual).isEqualTo(sizeClass)
    }

  @Test
  fun `undoPayload exposes repository flow`() =
    runTest(dispatcher) {
      val payload = UndoPayload(actionId = "delete")
      undoPayloadFlow.value = payload

      val actual = useCase.undoPayload.first()

      assertThat(actual).isEqualTo(payload)
    }

  @Test
  fun `openMode calls repository openMode`() =
    runTest(dispatcher) {
      useCase.openMode(ModeId.CHAT)
      advanceUntilIdle()

      coVerify { repository.openMode(ModeId.CHAT) }
    }

  @Test
  fun `toggleLeftDrawer calls repository toggleLeftDrawer`() =
    runTest(dispatcher) {
      useCase.toggleLeftDrawer()
      advanceUntilIdle()

      coVerify { repository.toggleLeftDrawer() }
    }

  @Test
  fun `setLeftDrawer calls repository setLeftDrawer with correct value`() =
    runTest(dispatcher) {
      useCase.setLeftDrawer(true)
      advanceUntilIdle()

      coVerify { repository.setLeftDrawer(true) }
    }

  @Test
  fun `toggleRightDrawer calls repository with panel`() =
    runTest(dispatcher) {
      useCase.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
      advanceUntilIdle()

      coVerify { repository.toggleRightDrawer(RightPanel.MODEL_SELECTOR) }
    }

  @Test
  fun `showCommandPalette calls repository with source`() =
    runTest(dispatcher) {
      useCase.showCommandPalette(PaletteSource.KEYBOARD_SHORTCUT)
      advanceUntilIdle()

      coVerify { repository.showCommandPalette(PaletteSource.KEYBOARD_SHORTCUT) }
    }

  @Test
  fun `hideCommandPalette calls repository hideCommandPalette`() =
    runTest(dispatcher) {
      useCase.hideCommandPalette()
      advanceUntilIdle()

      coVerify { repository.hideCommandPalette() }
    }

  @Test
  fun `updateWindowSizeClass calls repository with sizeClass`() =
    runTest(dispatcher) {
      val sizeClass =
        ShellWindowSizeClass(
          widthSizeClass = ShellWindowWidthClass.MEDIUM,
          heightSizeClass = ShellWindowHeightClass.EXPANDED,
        )
      useCase.updateWindowSizeClass(sizeClass)
      advanceUntilIdle()

      coVerify { repository.updateWindowSizeClass(sizeClass) }
    }
}
