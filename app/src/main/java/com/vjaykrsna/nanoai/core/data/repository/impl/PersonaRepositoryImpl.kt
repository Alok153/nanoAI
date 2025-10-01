package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.data.db.daos.PersonaProfileDao
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.model.toDomain
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PersonaRepository.
 *
 * Wraps PersonaProfileDao, converting between entities and domain models.
 */
@Singleton
class PersonaRepositoryImpl
    @Inject
    constructor(
        private val personaProfileDao: PersonaProfileDao,
    ) : PersonaRepository {
        override suspend fun getAllPersonas(): List<PersonaProfile> = personaProfileDao.getAll().map { it.toDomain() }

        override suspend fun getPersona(personaId: UUID): PersonaProfile? = personaProfileDao.getById(personaId.toString())?.toDomain()

        override suspend fun getPersonaById(personaId: UUID): Flow<PersonaProfile?> =
            personaProfileDao.observeById(personaId.toString()).map {
                it?.toDomain()
            }

        override suspend fun createPersona(persona: PersonaProfile) {
            personaProfileDao.insert(persona.toEntity())
        }

        override suspend fun updatePersona(persona: PersonaProfile) {
            personaProfileDao.update(persona.toEntity())
        }

        override suspend fun deletePersona(personaId: UUID) {
            val persona = personaProfileDao.getById(personaId.toString())
            if (persona != null) {
                personaProfileDao.delete(persona)
            }
        }

        override suspend fun getDefaultPersona(): PersonaProfile? {
            // Simple strategy: return first persona
            // Could be enhanced with a "isDefault" flag in the future
            return personaProfileDao.getAll().firstOrNull()?.toDomain()
        }

        override fun observeAllPersonas(): Flow<List<PersonaProfile>> =
            personaProfileDao.observeAll().map { personas ->
                personas.map {
                    it.toDomain()
                }
            }
    }
