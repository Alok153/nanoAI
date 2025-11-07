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
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelUiStateAggregationTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcher = MainDispatcherExtension(dispatcher)

  @Test
  fun uiStateReflectsCombinedSources() =
    runTest(dispatcher) {
      val repositories = createFakeRepositories()
      val navigationOperationsUseCase =
        NavigationOperationsUseCase(repositories.navigationRepository, dispatcher)

      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)
      val progressViewModel = createProgressViewModel(repositories, dispatcher)

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
          dispatcher,
        )

      val activityItem =
        RecentActivityItem(
          id = "recent",
          modeId = ModeId.CHAT,
          title = "Chat session",
          timestamp = Instant.now(),
          status = RecentStatus.COMPLETED,
        )
      val undoPayload = UndoPayload(actionId = "undo.chat", metadata = mapOf("origin" to "test"))
      val job =
        ProgressJob(
          jobId = UUID.randomUUID(),
          type = JobType.MODEL_DOWNLOAD,
          status = JobStatus.RUNNING,
          progress = 0.5f,
        )
      val persona =
        PersonaProfile(
          personaId = UUID.randomUUID(),
          name = "Test Persona",
          description = "",
          systemPrompt = "Help the user",
          createdAt = Clock.System.now(),
          updatedAt = Clock.System.now(),
        )
      val chatState =
        ChatState(availablePersonas = listOf(persona), currentPersonaId = persona.personaId)

      val fakeNavigationRepository = repositories.navigationRepository as FakeNavigationRepository
      viewModel.updateChatState(chatState)
      viewModel.onEvent(ShellUiEvent.QueueJob(job))
      viewModel.onEvent(ShellUiEvent.ShowCoverageDashboard)

      advanceUntilIdle()

      fakeNavigationRepository.emitRecentActivity(listOf(activityItem))
      fakeNavigationRepository.emitUndoPayload(undoPayload)

      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          state.layout.recentActivity.isNotEmpty() &&
            state.layout.pendingUndoAction == undoPayload &&
            state.layout.progressJobs.any { it.jobId == job.jobId }
        }

      assertThat(uiState.layout.recentActivity).containsExactly(activityItem)
      assertThat(uiState.layout.pendingUndoAction).isEqualTo(undoPayload)
      assertThat(uiState.layout.progressJobs).contains(job)
      assertThat(uiState.layout.showCoverageDashboard).isTrue()
      assertThat(uiState.chatState).isEqualTo(chatState)
      assertThat(uiState.quickActions).isNotEmpty()
    }
}
