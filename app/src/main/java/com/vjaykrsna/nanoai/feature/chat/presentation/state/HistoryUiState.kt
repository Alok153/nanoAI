package com.vjaykrsna.nanoai.feature.chat.presentation.state

import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/** Immutable state model for the chat history surface. */
data class HistoryUiState(
  val threads: PersistentList<ChatThread> = persistentListOf(),
  val isLoading: Boolean = false,
  val lastErrorMessage: String? = null,
) : NanoAIViewState
