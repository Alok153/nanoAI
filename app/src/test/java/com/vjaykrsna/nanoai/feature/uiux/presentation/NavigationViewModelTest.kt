package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.core.domain.uiux.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class NavigationViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  private lateinit var navigationOperationsUseCase: NavigationOperationsUseCase
  private lateinit var viewModel: NavigationViewModel

  private val windowSizeClassFlow =
    MutableStateFlow(WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)))
  private val commandPaletteFlow = MutableStateFlow(CommandPaletteState.Empty)
  private val recentActivityFlow = MutableStateFlow<List<RecentActivityItem>>(emptyList())
  private val undoPayloadFlow = MutableStateFlow<UndoPayload?>(null)

  @BeforeEach
  fun setUp() {
    navigationOperationsUseCase = mockk(relaxed = true)
    every { navigationOperationsUseCase.windowSizeClass } returns windowSizeClassFlow
    every { navigationOperationsUseCase.commandPaletteState } returns commandPaletteFlow
    every { navigationOperationsUseCase.recentActivity } returns recentActivityFlow
    every { navigationOperationsUseCase.undoPayload } returns undoPayloadFlow

    viewModel = NavigationViewModel(navigationOperationsUseCase)
  }

  @Test
  fun `openMode updates activeMode to chat`() =
    runTest(dispatcher) {
      viewModel.openMode(ModeId.CHAT)
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.activeMode).isEqualTo(ModeId.CHAT)
    }

  @Test
  fun `openMode updates activeMode to image`() =
    runTest(dispatcher) {
      viewModel.openMode(ModeId.IMAGE)
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.activeMode).isEqualTo(ModeId.IMAGE)
    }

  @Test
  fun `toggleLeftDrawer opens closed drawer`() =
    runTest(dispatcher) {
      viewModel.toggleLeftDrawer()
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.leftDrawerState.isOpen).isTrue()
    }

  @Test
  fun `toggleLeftDrawer closes open drawer`() =
    runTest(dispatcher) {
      viewModel.setLeftDrawer(true)
      advanceUntilIdle()
      viewModel.toggleLeftDrawer()
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.leftDrawerState.isOpen).isFalse()
    }

  @Test
  fun `setLeftDrawer opens drawer when true`() =
    runTest(dispatcher) {
      viewModel.setLeftDrawer(true)
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.leftDrawerState.isOpen).isTrue()
    }

  @Test
  fun `setLeftDrawer closes drawer when false`() =
    runTest(dispatcher) {
      viewModel.setLeftDrawer(true)
      advanceUntilIdle()
      viewModel.setLeftDrawer(false)
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.leftDrawerState.isOpen).isFalse()
    }

  @Test
  fun `toggleRightDrawer opens drawer and sets panel`() =
    runTest(dispatcher) {
      viewModel.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.rightDrawerState.isOpen).isTrue()
      assertThat(state.activeRightPanel).isEqualTo(RightPanel.MODEL_SELECTOR)
    }

  @Test
  fun `toggleRightDrawer closes open drawer and clears panel`() =
    runTest(dispatcher) {
      viewModel.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
      advanceUntilIdle()
      viewModel.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.rightDrawerState.isOpen).isFalse()
      assertThat(state.activeRightPanel).isNull()
    }

  @Test
  fun `toggleRightDrawer with different panel keeps drawer open`() =
    runTest(dispatcher) {
      viewModel.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
      advanceUntilIdle()
      viewModel.toggleRightDrawer(RightPanel.SETTINGS_SHORTCUT)
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.rightDrawerState.isOpen).isFalse()
      assertThat(state.activeRightPanel).isNull()
    }

  @Test
  fun `showCommandPalette delegates to use case`() =
    runTest(dispatcher) {
      viewModel.showCommandPalette(PaletteSource.KEYBOARD_SHORTCUT)
      advanceUntilIdle()

      verify { navigationOperationsUseCase.showCommandPalette(PaletteSource.KEYBOARD_SHORTCUT) }
    }

  @Test
  fun `hideCommandPalette delegates to use case`() =
    runTest(dispatcher) {
      viewModel.hideCommandPalette()
      advanceUntilIdle()

      verify { navigationOperationsUseCase.hideCommandPalette() }
    }

  @Test
  fun `updateWindowSizeClass delegates to use case`() =
    runTest(dispatcher) {
      val newSizeClass = WindowSizeClass.calculateFromSize(DpSize(1200.dp, 800.dp))
      viewModel.updateWindowSizeClass(newSizeClass)
      advanceUntilIdle()

      verify { navigationOperationsUseCase.updateWindowSizeClass(newSizeClass) }
    }

  @Test
  fun `default navigation state has expected values`() =
    runTest(dispatcher) {
      val defaultState = NavigationState.default()

      assertThat(defaultState.activeMode).isEqualTo(ModeId.HOME)
      assertThat(defaultState.leftDrawerState.isOpen).isFalse()
      assertThat(defaultState.rightDrawerState.isOpen).isFalse()
      assertThat(defaultState.activeRightPanel).isNull()
      assertThat(defaultState.undoState.payload).isNull()
      assertThat(defaultState.commandPalette).isEqualTo(CommandPaletteState.Empty)
    }

  @Test
  fun `command palette state reflects use case flow`() =
    runTest(dispatcher) {
      val updatedPalette = CommandPaletteState(query = "test query")
      commandPaletteFlow.value = updatedPalette
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.commandPalette).isEqualTo(updatedPalette)
    }

  @Test
  fun `window size class reflects use case flow`() =
    runTest(dispatcher) {
      val newSizeClass = WindowSizeClass.calculateFromSize(DpSize(1000.dp, 600.dp))
      windowSizeClassFlow.value = newSizeClass
      advanceUntilIdle()

      val state = viewModel.navigationState.first()
      assertThat(state.windowState).isEqualTo(newSizeClass)
    }
}
