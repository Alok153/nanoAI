package com.vjaykrsna.nanoai.shared.state

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Base class for feature ViewModels exposing a single immutable state stream plus eager event
 * delivery channel.
 */
abstract class ViewModelStateHost<S : NanoAIViewState, E : NanoAIViewEvent>
protected constructor(
  initialState: S,
  protected val dispatcher: CoroutineDispatcher,
  eventBufferCapacity: Int = DEFAULT_EVENT_BUFFER_CAPACITY,
) : ViewModel() {

  private val _state = MutableStateFlow(initialState)
  private val eventChannel = NanoAIEventChannel<E>(extraBufferCapacity = eventBufferCapacity)

  val state: StateFlow<S> = _state.asStateFlowView()
  val events: SharedFlow<E> = eventChannel.flow

  /** Replaces the current state with [newState]. */
  protected fun setState(newState: S) {
    _state.value = newState
  }

  /** Applies [reducer] to the current state and emits the resulting snapshot. */
  protected fun updateState(reducer: StateReducer<S>) {
    _state.reduce(reducer)
  }

  /** Emits [event] to observers, suspending if backpressure is in effect. */
  protected suspend fun emitEvent(event: E) {
    eventChannel.emit(event)
  }

  /** Attempts to emit [event] without suspension, returning `true` if successful. */
  protected fun tryEmitEvent(event: E): Boolean = eventChannel.tryEmit(event)

  protected companion object {
    private const val DEFAULT_EVENT_BUFFER_CAPACITY = 16
  }
}
