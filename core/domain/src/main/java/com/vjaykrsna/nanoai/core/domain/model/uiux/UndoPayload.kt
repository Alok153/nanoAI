package com.vjaykrsna.nanoai.core.domain.model.uiux

/** Data required to revert an action surfaced through the undo system. */
data class UndoPayload(val actionId: String, val metadata: Map<String, Any?> = emptyMap())
