package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.uiux.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelConnectivityTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun updateConnectivity_flushesQueuedJobsAndBanner() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val navigationOperationsUseCase =
        NavigationOperationsUseCase(fakeRepos.navigationRepository, dispatcher)

      val observeUserProfileUseCase =
        ObserveUserProfileUseCase(fakeRepos.userProfileRepository, dispatcher)

      val navigationCoordinator = mockk<NavigationCoordinator>(relaxed = true)
      val connectivityCoordinator = createConnectivityCoordinator(fakeRepos, dispatcher)
      val progressCoordinator = createProgressCoordinator(fakeRepos, dispatcher)
      val themeCoordinator = createThemeCoordinator(fakeRepos, dispatcher)

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          observeUserProfileUseCase,
          navigationCoordinator,
          connectivityCoordinator,
          progressCoordinator,
          themeCoordinator,
          dispatcher,
        )

      viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.ONLINE))
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state -> state.layout.connectivity == ConnectivityStatus.ONLINE }
      assertThat(uiState.layout.connectivity).isEqualTo(ConnectivityStatus.ONLINE)
      assertThat(uiState.connectivityBanner.status).isEqualTo(ConnectivityStatus.ONLINE)
    }

  @Test
  fun uiPreferences_flow_reflectsInShellState() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val navigationOperationsUseCase =
        NavigationOperationsUseCase(fakeRepos.navigationRepository, dispatcher)

      val observeUserProfileUseCase =
        ObserveUserProfileUseCase(fakeRepos.userProfileRepository, dispatcher)

      val navigationCoordinator = mockk<NavigationCoordinator>(relaxed = true)
      val connectivityCoordinator = createConnectivityCoordinator(fakeRepos, dispatcher)
      val progressCoordinator = createProgressCoordinator(fakeRepos, dispatcher)
      val themeCoordinator = createThemeCoordinator(fakeRepos, dispatcher)

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          observeUserProfileUseCase,
          navigationCoordinator,
          connectivityCoordinator,
          progressCoordinator,
          themeCoordinator,
          dispatcher,
        )

      themeCoordinator.updateThemePreference(this, ThemePreference.DARK)
      advanceUntilIdle()

      val uiState = viewModel.uiState.first { it.preferences.theme == ThemePreference.DARK }
      assertThat(uiState.preferences.theme).isEqualTo(ThemePreference.DARK)
    }

  @Test
  fun offlineConnectivity_filtersModeCards() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val navigationOperationsUseCase =
        NavigationOperationsUseCase(fakeRepos.navigationRepository, dispatcher)

      val observeUserProfileUseCase =
        ObserveUserProfileUseCase(fakeRepos.userProfileRepository, dispatcher)

      val navigationCoordinator = mockk<NavigationCoordinator>(relaxed = true)
      val connectivityCoordinator = createConnectivityCoordinator(fakeRepos, dispatcher)
      val progressCoordinator = createProgressCoordinator(fakeRepos, dispatcher)
      val themeCoordinator = createThemeCoordinator(fakeRepos, dispatcher)

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          observeUserProfileUseCase,
          navigationCoordinator,
          connectivityCoordinator,
          progressCoordinator,
          themeCoordinator,
          dispatcher,
        )

      viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.OFFLINE))
      advanceUntilIdle()

      val uiState = viewModel.uiState.first { it.layout.connectivity == ConnectivityStatus.OFFLINE }
      assertThat(uiState.layout.connectivity).isEqualTo(ConnectivityStatus.OFFLINE)
      assertThat(uiState.modeCards.any { it.id == ModeId.IMAGE }).isFalse()
    }
}
