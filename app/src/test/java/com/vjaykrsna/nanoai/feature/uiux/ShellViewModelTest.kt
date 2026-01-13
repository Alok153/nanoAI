package com.vjaykrsna.nanoai.feature.uiux

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowHeightClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowSizeClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowWidthClass
import com.vjaykrsna.nanoai.feature.uiux.presentation.createFakeRepositories
import com.vjaykrsna.nanoai.feature.uiux.presentation.createShellViewModelForTest
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * T032: ViewModel tests for navigation shell including connectivity banners and context
 * preservation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Nested
  @DisplayName("Connectivity Banner Tests")
  inner class ConnectivityBannerTests {

    @Test
    @DisplayName(
      "GIVEN online status WHEN connectivity changes to offline THEN banner shows offline"
    )
    fun offlineBanner_showsWhenConnectivityChangesToOffline() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        // Trigger offline status
        viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.OFFLINE))
        advanceUntilIdle()

        val uiState =
          viewModel.uiState.first { state ->
            state.connectivityBanner.status == ConnectivityStatus.OFFLINE
          }

        assertThat(uiState.connectivityBanner.status).isEqualTo(ConnectivityStatus.OFFLINE)
        assertThat(uiState.layout.isOffline).isTrue()
        assertThat(uiState.layout.connectivityStatusDescription).isEqualTo("Offline")
      }

    @Test
    @DisplayName(
      "GIVEN offline status WHEN connectivity changes to online THEN banner shows online"
    )
    fun onlineBanner_showsWhenConnectivityChangesToOnline() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        // Go offline first
        viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.OFFLINE))
        advanceUntilIdle()

        // Then go online
        viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.ONLINE))
        advanceUntilIdle()

        val uiState =
          viewModel.uiState.first { state ->
            state.connectivityBanner.status == ConnectivityStatus.ONLINE
          }

        assertThat(uiState.connectivityBanner.status).isEqualTo(ConnectivityStatus.ONLINE)
        assertThat(uiState.layout.isOffline).isFalse()
        assertThat(uiState.layout.connectivityStatusDescription).isEqualTo("Online")
      }

    @Test
    @DisplayName("GIVEN limited connectivity THEN banner reflects limited state")
    fun limitedBanner_showsLimitedConnectivityState() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.LIMITED))
        advanceUntilIdle()

        val uiState =
          viewModel.uiState.first { state ->
            state.connectivityBanner.status == ConnectivityStatus.LIMITED
          }

        assertThat(uiState.connectivityBanner.status).isEqualTo(ConnectivityStatus.LIMITED)
        assertThat(uiState.layout.connectivityStatusDescription).isEqualTo("Limited connectivity")
      }

    @Test
    @DisplayName("GIVEN offline status THEN online-requiring mode cards are filtered out")
    fun offlineMode_filtersOnlineRequiredModeCards() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.OFFLINE))
        advanceUntilIdle()

        val uiState =
          viewModel.uiState.first { state ->
            state.layout.connectivity == ConnectivityStatus.OFFLINE
          }

        // IMAGE mode requires online and is experimental - should be filtered
        assertThat(uiState.modeCards.any { it.id == ModeId.IMAGE }).isFalse()
        // CHAT mode is available offline
        assertThat(uiState.modeCards.any { it.id == ModeId.CHAT }).isTrue()
      }
  }

  @Nested
  @DisplayName("Context Preservation Tests")
  inner class ContextPreservationTests {

    @Test
    @DisplayName("WHEN navigating between modes THEN activeMode is preserved")
    fun navigation_preservesActiveMode() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        // Navigate to chat
        viewModel.onEvent(ShellUiEvent.ModeSelected(ModeId.CHAT))
        advanceUntilIdle()

        var uiState = viewModel.uiState.first { state -> state.layout.activeMode == ModeId.CHAT }
        assertThat(uiState.layout.activeMode).isEqualTo(ModeId.CHAT)

        // Navigate to library
        viewModel.onEvent(ShellUiEvent.ModeSelected(ModeId.LIBRARY))
        advanceUntilIdle()

        uiState = viewModel.uiState.first { state -> state.layout.activeMode == ModeId.LIBRARY }
        assertThat(uiState.layout.activeMode).isEqualTo(ModeId.LIBRARY)
      }

    @Test
    @DisplayName("WHEN window size changes THEN layout state updates accordingly")
    fun windowSizeChange_updatesLayoutState() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        val expandedSize =
          ShellWindowSizeClass(
            widthSizeClass = ShellWindowWidthClass.EXPANDED,
            heightSizeClass = ShellWindowHeightClass.EXPANDED,
          )

        viewModel.updateWindowSizeClass(expandedSize)
        advanceUntilIdle()

        val uiState =
          viewModel.uiState.first { state ->
            state.layout.windowSizeClass.widthSizeClass == ShellWindowWidthClass.EXPANDED
          }

        assertThat(uiState.layout.windowSizeClass.widthSizeClass)
          .isEqualTo(ShellWindowWidthClass.EXPANDED)
        assertThat(uiState.layout.usesPermanentLeftDrawer).isTrue()
      }

    @Test
    @DisplayName("WHEN connectivity changes THEN progress jobs state is preserved")
    fun connectivityChange_preservesProgressJobs() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        val job =
          ProgressJob(
            jobId = UUID.randomUUID(),
            type = JobType.MODEL_DOWNLOAD,
            status = JobStatus.PENDING,
            progress = 0.5f,
            eta = Duration.ofSeconds(30),
            canRetry = true,
            queuedAt = Instant.now(),
          )

        // Queue a job
        viewModel.onEvent(ShellUiEvent.QueueJob(job))
        advanceUntilIdle()

        var uiState = viewModel.uiState.first { state -> state.layout.progressJobs.isNotEmpty() }
        assertThat(uiState.layout.progressJobs).hasSize(1)

        // Change connectivity
        viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.OFFLINE))
        advanceUntilIdle()

        uiState =
          viewModel.uiState.first { state ->
            state.layout.connectivity == ConnectivityStatus.OFFLINE
          }

        // Job should still be present
        assertThat(uiState.layout.progressJobs).hasSize(1)
        assertThat(uiState.layout.progressJobs.first().jobId).isEqualTo(job.jobId)
      }

    @Test
    @DisplayName("WHEN drawer state changes THEN navigation context is maintained")
    fun drawerStateChange_maintainsNavigationContext() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        // Navigate to chat first
        viewModel.onEvent(ShellUiEvent.ModeSelected(ModeId.CHAT))
        advanceUntilIdle()

        // Toggle left drawer
        viewModel.onEvent(ShellUiEvent.ToggleLeftDrawer)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first { state -> state.layout.activeMode == ModeId.CHAT }

        // Active mode should remain CHAT
        assertThat(uiState.layout.activeMode).isEqualTo(ModeId.CHAT)
      }
  }

  @Nested
  @DisplayName("Progress Center Integration Tests")
  inner class ProgressCenterIntegrationTests {

    @Test
    @DisplayName("GIVEN queued jobs WHEN offline THEN job count reflects in layout")
    fun offlineJobs_reflectsInLayoutJobCount() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        // Go offline
        viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.OFFLINE))
        advanceUntilIdle()

        // Queue multiple jobs
        repeat(3) {
          viewModel.onEvent(
            ShellUiEvent.QueueJob(
              ProgressJob(
                jobId = UUID.randomUUID(),
                type = JobType.IMAGE_GENERATION,
                status = JobStatus.PENDING,
                progress = 0f,
                eta = Duration.ofSeconds(60),
                canRetry = true,
                queuedAt = Instant.now(),
              )
            )
          )
        }
        advanceUntilIdle()

        val uiState = viewModel.uiState.first { state -> state.layout.progressJobs.size >= 3 }

        assertThat(uiState.layout.jobCount).isEqualTo(3)
        assertThat(uiState.layout.hasActiveJobs).isTrue()
      }

    @Test
    @DisplayName("GIVEN running job WHEN completed THEN job removed from list")
    fun jobCompletion_removesJobFromList() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        val jobId = UUID.randomUUID()
        val job =
          ProgressJob(
            jobId = jobId,
            type = JobType.MODEL_DOWNLOAD,
            status = JobStatus.RUNNING,
            progress = 0.8f,
            eta = Duration.ofSeconds(10),
            canRetry = false,
            queuedAt = Instant.now(),
          )

        // Queue job
        viewModel.onEvent(ShellUiEvent.QueueJob(job))
        advanceUntilIdle()

        val queuedState =
          viewModel.uiState.first { state -> state.layout.progressJobs.isNotEmpty() }
        assertThat(queuedState.layout.progressJobs).hasSize(1)

        // Complete job
        viewModel.onEvent(ShellUiEvent.CompleteJob(jobId))
        advanceUntilIdle()

        val completedState =
          viewModel.uiState.first { state -> state.layout.progressJobs.isEmpty() }
        assertThat(completedState.layout.progressJobs).isEmpty()
      }
  }

  @Nested
  @DisplayName("Shell Layout State Tests")
  inner class ShellLayoutStateTests {

    @Test
    @DisplayName("GIVEN compact width THEN isCompactWidth returns true")
    fun compactWidth_isCompactWidthReturnsTrue() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        val compactSize =
          ShellWindowSizeClass(
            widthSizeClass = ShellWindowWidthClass.COMPACT,
            heightSizeClass = ShellWindowHeightClass.MEDIUM,
          )

        viewModel.updateWindowSizeClass(compactSize)
        advanceUntilIdle()

        val uiState =
          viewModel.uiState.first { state ->
            state.layout.windowSizeClass.widthSizeClass == ShellWindowWidthClass.COMPACT
          }

        assertThat(uiState.layout.isCompactWidth).isTrue()
        assertThat(uiState.layout.usesPermanentLeftDrawer).isFalse()
        assertThat(uiState.layout.useModalNavigation).isTrue()
      }

    @Test
    @DisplayName("GIVEN medium width THEN isMediumOrWider returns true")
    fun mediumWidth_isMediumOrWiderReturnsTrue() =
      runTest(dispatcher) {
        val fakeRepos = createFakeRepositories()
        val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

        val mediumSize =
          ShellWindowSizeClass(
            widthSizeClass = ShellWindowWidthClass.MEDIUM,
            heightSizeClass = ShellWindowHeightClass.MEDIUM,
          )

        viewModel.updateWindowSizeClass(mediumSize)
        advanceUntilIdle()

        val uiState =
          viewModel.uiState.first { state ->
            state.layout.windowSizeClass.widthSizeClass == ShellWindowWidthClass.MEDIUM
          }

        assertThat(uiState.layout.isMediumOrWider).isTrue()
        assertThat(uiState.layout.usesPermanentLeftDrawer).isTrue()
      }
  }
}
