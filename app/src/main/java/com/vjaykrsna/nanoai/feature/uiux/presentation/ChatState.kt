package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import java.util.UUID

/** State for chat-specific UI elements. */
data class ChatState(val availablePersonas: List<PersonaProfile>, val currentPersonaId: UUID?)
