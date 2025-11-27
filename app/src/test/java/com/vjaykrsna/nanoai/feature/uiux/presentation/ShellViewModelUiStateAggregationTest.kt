package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.core.domain.uiux.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelUiStateAggregationTest {
  private val dispatcher: TestDispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcher = MainDispatcherExtension(dispatcher)

  @Test
  fun uiStateReflectsCombinedSources() =
    runTest(dispatcher) {
      val repositories = createFakeRepositories()
      val viewModel = createShellViewModel(repositories, dispatcher)
      val fixtures = uiAggregationFixtures()

      viewModel.applyInitialState(fixtures)

      advanceUntilIdle()

      val navigationRepository = repositories.navigationRepository as FakeNavigationRepository
      navigationRepository.emitRecentActivity(listOf(fixtures.activityItem))
      navigationRepository.emitUndoPayload(fixtures.undoPayload)

      advanceUntilIdle()

      val uiState = viewModel.awaitAggregatedState(fixtures)

      assertThat(uiState.layout.recentActivity).containsExactly(fixtures.activityItem)
      assertThat(uiState.layout.pendingUndoAction).isEqualTo(fixtures.undoPayload)
      assertThat(uiState.layout.progressJobs).contains(fixtures.job)
      assertThat(uiState.layout.showCoverageDashboard).isTrue()
      assertThat(uiState.chatState).isEqualTo(fixtures.chatState)
      assertThat(uiState.quickActions).isNotEmpty()
    }
}

private fun createShellViewModel(
  repositories: FakeRepositories,
  dispatcher: TestDispatcher,
): ShellViewModel {
  val navigationOperationsUseCase =
    NavigationOperationsUseCase(repositories.navigationRepository, dispatcher)
  val observeUserProfileUseCase =
    ObserveUserProfileUseCase(repositories.userProfileRepository, dispatcher)
  val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
  val connectivityViewModel = createConnectivityViewModel(repositories, dispatcher)
  val themeViewModel = createThemeViewModel(repositories, dispatcher)
  val progressViewModel = createProgressViewModel(repositories, dispatcher)

  return ShellViewModel(
    navigationOperationsUseCase,
    observeUserProfileUseCase,
    navigationViewModel,
    connectivityViewModel,
    progressViewModel,
    themeViewModel,
    dispatcher,
  )
}

private data class UiAggregationFixtures(
  val activityItem: RecentActivityItem,
  val undoPayload: UndoPayload,
  val job: ProgressJob,
  val chatState: ChatState,
)

private fun uiAggregationFixtures(): UiAggregationFixtures {
  val persona =
    PersonaProfile(
      personaId = UUID.randomUUID(),
      name = "Test Persona",
      description = "",
      systemPrompt = "Help the user",
      createdAt = Clock.System.now(),
      updatedAt = Clock.System.now(),
    )

  return UiAggregationFixtures(
    activityItem =
      RecentActivityItem(
        id = "recent",
        modeId = ModeId.CHAT,
        title = "Chat session",
        timestamp = Instant.now(),
        status = RecentStatus.COMPLETED,
      ),
    undoPayload = UndoPayload(actionId = "undo.chat", metadata = mapOf("origin" to "test")),
    job =
      ProgressJob(
        jobId = UUID.randomUUID(),
        type = JobType.MODEL_DOWNLOAD,
        status = JobStatus.RUNNING,
        progress = 0.5f,
      ),
    chatState = ChatState(availablePersonas = listOf(persona), currentPersonaId = persona.personaId),
  )
}

private fun ShellViewModel.applyInitialState(fixtures: UiAggregationFixtures) {
  updateChatState(fixtures.chatState)
  onEvent(ShellUiEvent.QueueJob(fixtures.job))
  onEvent(ShellUiEvent.ShowCoverageDashboard)
}

private suspend fun ShellViewModel.awaitAggregatedState(
  fixtures: UiAggregationFixtures
): ShellUiState {
  return uiState.first { state ->
    state.layout.recentActivity.isNotEmpty() &&
      state.layout.pendingUndoAction == fixtures.undoPayload &&
      state.layout.progressJobs.any { it.jobId == fixtures.job.jobId }
  }
}
