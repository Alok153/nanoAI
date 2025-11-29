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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class NavigationCoordinatorTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  private lateinit var navigationOperationsUseCase: NavigationOperationsUseCase
  private lateinit var coordinator: NavigationCoordinator

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

    coordinator = NavigationCoordinator(navigationOperationsUseCase)
  }

  @Test
  fun `openMode updates activeMode to chat`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        coordinator.openMode(ModeId.CHAT)
        val state = stateFlow.first { it.activeMode == ModeId.CHAT }
        assertThat(state.activeMode).isEqualTo(ModeId.CHAT)
      }
      advanceUntilIdle()
    }

  @Test
  fun `openMode updates activeMode to image`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        coordinator.openMode(ModeId.IMAGE)
        val state = stateFlow.first { it.activeMode == ModeId.IMAGE }
        assertThat(state.activeMode).isEqualTo(ModeId.IMAGE)
      }
      advanceUntilIdle()
    }

  @Test
  fun `toggleLeftDrawer opens closed drawer`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        coordinator.toggleLeftDrawer()
        val state = stateFlow.first { it.leftDrawerState.isOpen }
        assertThat(state.leftDrawerState.isOpen).isTrue()
      }
      advanceUntilIdle()
    }

  @Test
  fun `toggleLeftDrawer closes open drawer`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        coordinator.setLeftDrawer(true)
        stateFlow.first { it.leftDrawerState.isOpen }
        coordinator.toggleLeftDrawer()
        val state = stateFlow.first { !it.leftDrawerState.isOpen }
        assertThat(state.leftDrawerState.isOpen).isFalse()
      }
      advanceUntilIdle()
    }

  @Test
  fun `setLeftDrawer opens drawer when true`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        coordinator.setLeftDrawer(true)
        val state = stateFlow.first { it.leftDrawerState.isOpen }
        assertThat(state.leftDrawerState.isOpen).isTrue()
      }
      advanceUntilIdle()
    }

  @Test
  fun `setLeftDrawer closes drawer when false`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        coordinator.setLeftDrawer(true)
        stateFlow.first { it.leftDrawerState.isOpen }
        coordinator.setLeftDrawer(false)
        val state = stateFlow.first { !it.leftDrawerState.isOpen }
        assertThat(state.leftDrawerState.isOpen).isFalse()
      }
      advanceUntilIdle()
    }

  @Test
  fun `toggleRightDrawer opens drawer and sets panel`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        coordinator.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
        val state = stateFlow.first { it.rightDrawerState.isOpen }
        assertThat(state.rightDrawerState.isOpen).isTrue()
        assertThat(state.activeRightPanel).isEqualTo(RightPanel.MODEL_SELECTOR)
      }
      advanceUntilIdle()
    }

  @Test
  fun `toggleRightDrawer closes open drawer and clears panel`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        coordinator.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
        stateFlow.first { it.rightDrawerState.isOpen }
        coordinator.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
        val state = stateFlow.first { !it.rightDrawerState.isOpen }
        assertThat(state.rightDrawerState.isOpen).isFalse()
        assertThat(state.activeRightPanel).isNull()
      }
      advanceUntilIdle()
    }

  @Test
  fun `toggleRightDrawer with different panel keeps drawer open`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        coordinator.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
        stateFlow.first { it.rightDrawerState.isOpen }
        coordinator.toggleRightDrawer(RightPanel.SETTINGS_SHORTCUT)
        val state = stateFlow.first { !it.rightDrawerState.isOpen }
        assertThat(state.rightDrawerState.isOpen).isFalse()
        assertThat(state.activeRightPanel).isNull()
      }
      advanceUntilIdle()
    }

  @Test
  fun `showCommandPalette delegates to use case`() =
    runTest(dispatcher) {
      coordinator.showCommandPalette(PaletteSource.KEYBOARD_SHORTCUT)
      advanceUntilIdle()

      verify { navigationOperationsUseCase.showCommandPalette(PaletteSource.KEYBOARD_SHORTCUT) }
    }

  @Test
  fun `hideCommandPalette delegates to use case`() =
    runTest(dispatcher) {
      coordinator.hideCommandPalette()
      advanceUntilIdle()

      verify { navigationOperationsUseCase.hideCommandPalette() }
    }

  @Test
  fun `updateWindowSizeClass delegates to use case`() =
    runTest(dispatcher) {
      val newSizeClass = WindowSizeClass.calculateFromSize(DpSize(1200.dp, 800.dp))
      coordinator.updateWindowSizeClass(newSizeClass)
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
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        val updatedPalette = CommandPaletteState(query = "test query")
        commandPaletteFlow.value = updatedPalette
        val state = stateFlow.first { it.commandPalette == updatedPalette }
        assertThat(state.commandPalette).isEqualTo(updatedPalette)
      }
      advanceUntilIdle()
    }

  @Test
  fun `window size class reflects use case flow`() =
    runTest(dispatcher) {
      backgroundScope.launch {
        val stateFlow = coordinator.navigationState(this)
        val newSizeClass = WindowSizeClass.calculateFromSize(DpSize(1000.dp, 600.dp))
        windowSizeClassFlow.value = newSizeClass
        val state = stateFlow.first { it.windowState == newSizeClass }
        assertThat(state.windowState).isEqualTo(newSizeClass)
      }
      advanceUntilIdle()
    }
}
