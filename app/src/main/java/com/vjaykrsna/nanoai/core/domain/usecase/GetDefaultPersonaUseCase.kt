package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import javax.inject.Inject

/**
 * Use case for getting the default persona.
 *
 * Provides a clean domain interface for retrieving the default persona, abstracting the repository
 * implementation details.
 */
class GetDefaultPersonaUseCase
@Inject
constructor(private val personaRepository: PersonaRepository) {
  /**
   * Gets the default persona for new threads.
   *
   * @return The default persona, or null if none exists
   */
  suspend operator fun invoke(): PersonaProfile? = personaRepository.getDefaultPersona()
}
