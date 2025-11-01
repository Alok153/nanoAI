package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing all personas.
 *
 * Provides a clean domain interface for reactive persona observation, abstracting the repository
 * implementation details.
 */
class ObservePersonasUseCase @Inject constructor(private val personaRepository: PersonaRepository) {
  /**
   * Observes all personas with reactive updates.
   *
   * @return Flow of persona profile lists that emits whenever personas change
   */
  operator fun invoke(): Flow<List<PersonaProfile>> = personaRepository.observeAllPersonas()
}
