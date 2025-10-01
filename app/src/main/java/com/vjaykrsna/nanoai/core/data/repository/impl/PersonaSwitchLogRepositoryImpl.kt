package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.data.db.daos.PersonaSwitchLogDao
import com.vjaykrsna.nanoai.core.data.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import com.vjaykrsna.nanoai.core.domain.model.toDomain
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PersonaSwitchLogRepository.
 *
 * Wraps PersonaSwitchLogDao, converting between entities and domain models.
 */
@Singleton
class PersonaSwitchLogRepositoryImpl
    @Inject
    constructor(
        private val personaSwitchLogDao: PersonaSwitchLogDao,
    ) : PersonaSwitchLogRepository {
        override suspend fun logSwitch(log: PersonaSwitchLog) {
            personaSwitchLogDao.insert(log.toEntity())
        }

        override suspend fun getSwitchHistory(threadId: UUID): List<PersonaSwitchLog> =
            personaSwitchLogDao.getByThreadId(threadId.toString()).map {
                it.toDomain()
            }

        override suspend fun getLatestSwitch(threadId: UUID): PersonaSwitchLog? =
            personaSwitchLogDao.getLatestForThread(threadId.toString())?.toDomain()

        override suspend fun getLogsByThreadId(threadId: UUID): Flow<List<PersonaSwitchLog>> =
            personaSwitchLogDao.observeByThreadId(threadId.toString()).map { logs ->
                logs.map { it.toDomain() }
            }
    }
