package com.vjaykrsna.nanoai.feature.uiux.state

/** Data required to revert an action surfaced through the undo system. */
data class UndoPayload(
  val actionId: String,
  val metadata: Map<String, Any?> = emptyMap(),
)
