package com.vjaykrsna.nanoai.feature.settings.data

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.NanoAIResult.Companion.recoverable
import com.vjaykrsna.nanoai.core.common.NanoAIResult.Companion.success
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository as DomainPersonaRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.settings.domain.PersonaRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

/** Data source contract for persona persistence and logging. */
interface PersonaDataSource {
  fun observePersonas(): Flow<List<PersonaProfile>>

  suspend fun getDefaultPersona(): NanoAIResult<PersonaProfile?>

  suspend fun switchPersona(
    currentThreadId: UUID?,
    personaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID>

  suspend fun savePersona(persona: PersonaProfile): NanoAIResult<Unit>

  suspend fun deletePersona(personaId: UUID): NanoAIResult<Unit>

  suspend fun recordPersonaSwitch(
    threadId: UUID,
    previousPersonaId: UUID?,
    newPersonaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<Unit>
}

/** Data source implementation that bridges to core persona and conversation repositories. */
@Singleton
class CorePersonaDataSource
@Inject
constructor(
  private val conversationRepository: ConversationRepository,
  private val personaRepository: DomainPersonaRepository,
  private val personaSwitchLogRepository: PersonaSwitchLogRepository,
  private val clock: Clock = Clock.System,
) : PersonaDataSource {

  override fun observePersonas(): Flow<List<PersonaProfile>> =
    personaRepository.observeAllPersonas()

  override suspend fun getDefaultPersona(): NanoAIResult<PersonaProfile?> =
    success(personaRepository.getDefaultPersona())

  override suspend fun switchPersona(
    currentThreadId: UUID?,
    personaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID> =
    runCatching {
        val previousPersonaId =
          currentThreadId?.let { threadId ->
            conversationRepository.getCurrentPersonaForThread(threadId)
          }

        val targetThreadId =
          when (action) {
            PersonaSwitchAction.CONTINUE_THREAD -> {
              val resolvedThreadId =
                currentThreadId ?: conversationRepository.createNewThread(personaId)
              if (currentThreadId != null) {
                conversationRepository.updateThreadPersona(resolvedThreadId, personaId)
              }
              resolvedThreadId
            }
            PersonaSwitchAction.START_NEW_THREAD ->
              conversationRepository.createNewThread(personaId)
          }

        val log =
          PersonaSwitchLog(
            logId = UUID.randomUUID(),
            threadId = targetThreadId,
            previousPersonaId = previousPersonaId,
            newPersonaId = personaId,
            actionTaken = action,
            createdAt = clock.now(),
          )
        personaSwitchLogRepository.logSwitch(log)

        targetThreadId
      }
      .fold(
        onSuccess = { threadId -> success(threadId) },
        onFailure = { error -> recoverable(message = "Failed to switch persona", cause = error) },
      )

  override suspend fun savePersona(persona: PersonaProfile): NanoAIResult<Unit> =
    runCatching { personaRepository.createPersona(persona) }
      .fold(
        onSuccess = { success(Unit) },
        onFailure = { recoverable("Failed to save persona", cause = it) },
      )

  override suspend fun deletePersona(personaId: UUID): NanoAIResult<Unit> =
    runCatching { personaRepository.deletePersona(personaId) }
      .fold(
        onSuccess = { success(Unit) },
        onFailure = { recoverable("Failed to delete persona", cause = it) },
      )

  override suspend fun recordPersonaSwitch(
    threadId: UUID,
    previousPersonaId: UUID?,
    newPersonaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<Unit> =
    runCatching {
        val log =
          PersonaSwitchLog(
            logId = UUID.randomUUID(),
            threadId = threadId,
            previousPersonaId = previousPersonaId,
            newPersonaId = newPersonaId,
            actionTaken = action,
            createdAt = clock.now(),
          )
        personaSwitchLogRepository.logSwitch(log)
      }
      .fold(
        onSuccess = { success(Unit) },
        onFailure = {
          recoverable(
            message = "Failed to record persona switch",
            cause = it,
            context = mapOf("threadId" to threadId.toString()),
          )
        },
      )
}

/** Repository implementation that shields the domain contract from concrete data sources. */
@Singleton
class DefaultPersonaRepository @Inject constructor(private val dataSource: PersonaDataSource) :
  PersonaRepository {

  override fun observePersonas(): Flow<List<PersonaProfile>> = dataSource.observePersonas()

  override suspend fun getDefaultPersona(): NanoAIResult<PersonaProfile?> =
    dataSource.getDefaultPersona()

  override suspend fun switchPersona(
    currentThreadId: UUID?,
    personaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID> = dataSource.switchPersona(currentThreadId, personaId, action)

  override suspend fun savePersona(persona: PersonaProfile): NanoAIResult<Unit> =
    dataSource.savePersona(persona)

  override suspend fun deletePersona(personaId: UUID): NanoAIResult<Unit> =
    dataSource.deletePersona(personaId)

  override suspend fun recordPersonaSwitch(
    threadId: UUID,
    previousPersonaId: UUID?,
    newPersonaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<Unit> =
    dataSource.recordPersonaSwitch(threadId, previousPersonaId, newPersonaId, action)
}
