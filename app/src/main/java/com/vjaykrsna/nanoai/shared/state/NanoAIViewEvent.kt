package com.vjaykrsna.nanoai.shared.state

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Marker interface for one-off UI events exposed by a [ViewModelStateHost]. */
interface NanoAIViewEvent

/**
 * Thin wrapper around [MutableSharedFlow] tailored for UI event delivery with predictable
 * backpressure behaviour.
 */
class NanoAIEventChannel<E : NanoAIViewEvent>
constructor(
  replay: Int = 0,
  extraBufferCapacity: Int = 16,
  onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST,
) {
  private val delegate = MutableSharedFlow<E>(replay, extraBufferCapacity, onBufferOverflow)

  val flow: SharedFlow<E> = delegate.asSharedFlow()

  suspend fun emit(event: E) {
    delegate.emit(event)
  }

  fun tryEmit(event: E): Boolean = delegate.tryEmit(event)
}
