package com.vjaykrsna.nanoai.feature.chat.presentation.state

import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState

/** Immutable state model for the chat history surface. */
data class HistoryUiState(
  val threads: List<ChatThread> = emptyList(),
  val isLoading: Boolean = false,
  val pendingErrorMessage: String? = null,
) : NanoAIViewState
