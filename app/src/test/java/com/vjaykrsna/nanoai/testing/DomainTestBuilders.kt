package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Test data builders for domain models. */
object DomainTestBuilders {

  /** Creates a test [PersonaProfile] with sensible defaults. */
  fun buildPersona(
    personaId: UUID = UUID.randomUUID(),
    name: String = "Test Persona",
    description: String = "A test persona",
    systemPrompt: String = "You are a helpful assistant",
    defaultModelPreference: String? = null,
    temperature: Float = 0.7f,
    topP: Float = 1.0f,
    defaultVoice: String? = null,
    defaultImageStyle: String? = null,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant = Clock.System.now(),
  ): PersonaProfile =
    PersonaProfile(
      personaId = personaId,
      name = name,
      description = description,
      systemPrompt = systemPrompt,
      defaultModelPreference = defaultModelPreference,
      temperature = temperature,
      topP = topP,
      defaultVoice = defaultVoice,
      defaultImageStyle = defaultImageStyle,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )

  /** Creates a test [ChatThread] with sensible defaults. */
  fun buildChatThread(
    threadId: UUID = UUID.randomUUID(),
    personaId: UUID? = UUID.randomUUID(),
    title: String = "Test Thread",
    activeModelId: String = "default-model",
    isArchived: Boolean = false,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant = Clock.System.now(),
  ): ChatThread =
    ChatThread(
      threadId = threadId,
      title = title,
      personaId = personaId,
      activeModelId = activeModelId,
      createdAt = createdAt,
      updatedAt = updatedAt,
      isArchived = isArchived,
    )

  /** Creates a test [Message] with sensible defaults. */
  fun buildMessage(
    messageId: UUID = UUID.randomUUID(),
    threadId: UUID = UUID.randomUUID(),
    role: MessageRole = MessageRole.USER,
    text: String? = "Test message",
    audioUri: String? = null,
    imageUri: String? = null,
    source: MessageSource = MessageSource.LOCAL_MODEL,
    latencyMs: Long? = null,
    createdAt: Instant = Clock.System.now(),
    errorCode: String? = null,
  ): Message =
    Message(
      messageId = messageId,
      threadId = threadId,
      role = role,
      text = text,
      audioUri = audioUri,
      imageUri = imageUri,
      source = source,
      latencyMs = latencyMs,
      createdAt = createdAt,
      errorCode = errorCode,
    )

  /** Creates a user message. */
  fun buildUserMessage(
    text: String = "User message",
    threadId: UUID = UUID.randomUUID(),
    messageId: UUID = UUID.randomUUID(),
  ): Message =
    buildMessage(
      messageId = messageId,
      threadId = threadId,
      role = MessageRole.USER,
      text = text,
      source = MessageSource.LOCAL_MODEL,
    )

  /** Creates an assistant message. */
  fun buildAssistantMessage(
    text: String? = "Assistant response",
    threadId: UUID = UUID.randomUUID(),
    messageId: UUID = UUID.randomUUID(),
    latencyMs: Long = 100,
  ): Message =
    buildMessage(
      messageId = messageId,
      threadId = threadId,
      role = MessageRole.ASSISTANT,
      text = text,
      source = MessageSource.LOCAL_MODEL,
      latencyMs = latencyMs,
    )

  /** Creates a test [ModelPackage] with sensible defaults. */
  fun buildModelPackage(
    modelId: String = "test-model-${UUID.randomUUID()}",
    displayName: String = "Test Model",
    version: String = "1.0.0",
    providerType: ProviderType = ProviderType.MEDIA_PIPE,
    deliveryType: DeliveryType = DeliveryType.LOCAL_ARCHIVE,
    minAppVersion: Int = 1,
    sizeBytes: Long = 1024 * 1024, // 1MB
    capabilities: Set<String> = setOf("text"),
    installState: InstallState = InstallState.NOT_INSTALLED,
    downloadTaskId: UUID? = null,
    manifestUrl: String = "https://example.com/manifest",
    checksumSha256: String? = null,
    signature: String? = null,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant = Clock.System.now(),
  ): ModelPackage =
    ModelPackage(
      modelId = modelId,
      displayName = displayName,
      version = version,
      providerType = providerType,
      deliveryType = deliveryType,
      minAppVersion = minAppVersion,
      sizeBytes = sizeBytes,
      capabilities = capabilities,
      installState = installState,
      downloadTaskId = downloadTaskId,
      manifestUrl = manifestUrl,
      checksumSha256 = checksumSha256,
      signature = signature,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )

  /** Creates a test [APIProviderConfig] with sensible defaults. */
  fun buildProviderConfig(
    providerId: String = "openai",
    providerName: String = "OpenAI",
    baseUrl: String = "https://api.openai.com/v1",
    apiKey: String = "sk-test",
    apiType: APIType = APIType.OPENAI_COMPATIBLE,
    isEnabled: Boolean = true,
    quotaResetAt: Instant? = null,
    lastStatus: ProviderStatus = ProviderStatus.OK,
  ): APIProviderConfig =
    APIProviderConfig(
      providerId = providerId,
      providerName = providerName,
      baseUrl = baseUrl,
      apiKey = apiKey,
      apiType = apiType,
      isEnabled = isEnabled,
      quotaResetAt = quotaResetAt,
      lastStatus = lastStatus,
    )

  /** Creates a test [DownloadTask] with sensible defaults. */
  fun buildDownloadTask(
    taskId: UUID = UUID.randomUUID(),
    modelId: String = "model-${taskId}",
    status: DownloadStatus = DownloadStatus.DOWNLOADING,
    progress: Float = 0f,
    errorMessage: String? = null,
    bytesDownloaded: Long = 0,
  ): DownloadTask =
    DownloadTask(
      taskId = taskId,
      modelId = modelId,
      status = status,
      progress = progress,
      errorMessage = errorMessage,
      bytesDownloaded = bytesDownloaded,
    )
}
