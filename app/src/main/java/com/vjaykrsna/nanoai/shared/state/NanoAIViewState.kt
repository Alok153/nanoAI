package com.vjaykrsna.nanoai.shared.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Marker interface for immutable UI state snapshots owned by a [ViewModelStateHost]. */
interface NanoAIViewState

/** Reducer function type that transforms an existing UI state into a new snapshot. */
typealias StateReducer<S> = S.() -> S

/** Applies [reducer] to the receiver [MutableStateFlow] and updates its value. */
inline fun <S : NanoAIViewState> MutableStateFlow<S>.reduce(reducer: StateReducer<S>) {
  update { current -> current.reducer() }
}

/** Exposes the read-only view of a [MutableStateFlow] representing a UI state. */
fun <S : NanoAIViewState> MutableStateFlow<S>.asStateFlowView(): StateFlow<S> = asStateFlow()
