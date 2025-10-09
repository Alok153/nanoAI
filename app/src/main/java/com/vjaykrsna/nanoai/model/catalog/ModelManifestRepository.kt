package com.vjaykrsna.nanoai.model.catalog

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.device.DeviceIdentityProvider
import com.vjaykrsna.nanoai.model.catalog.network.ModelCatalogService
import com.vjaykrsna.nanoai.model.catalog.network.dto.ErrorEnvelopeDto
import com.vjaykrsna.nanoai.model.catalog.network.dto.ManifestVerificationRequestDto
import com.vjaykrsna.nanoai.model.catalog.network.dto.ManifestVerificationResponseStatusDto
import com.vjaykrsna.nanoai.model.catalog.network.dto.ManifestVerificationStatusDto
import com.vjaykrsna.nanoai.model.catalog.network.dto.ModelManifestDto
import com.vjaykrsna.nanoai.model.huggingface.HuggingFaceManifestFetcher
import com.vjaykrsna.nanoai.model.huggingface.HuggingFaceManifestRequest
import com.vjaykrsna.nanoai.telemetry.TelemetryReporter
import java.net.HttpURLConnection
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException

private val HEX_64_REGEX = Regex("^[0-9a-fA-F]{64}$")
private const val MAX_RETRY_AFTER_SECONDS = 300

/** Abstraction over manifest API + Room cache. */
interface ModelManifestRepository {
  /** Fetches the latest manifest for a model/version and caches it locally. */
  suspend fun refreshManifest(modelId: String, version: String): NanoAIResult<DownloadManifest>

  /** Reports verification result back to the catalog service. */
  suspend fun reportVerification(
    modelId: String,
    version: String,
    checksumSha256: String,
    status: VerificationOutcome,
    failureReason: String? = null,
  ): NanoAIResult<Unit>
}

/** Result status for reporting manifest verification. */
enum class VerificationOutcome {
  SUCCESS,
  CORRUPTED,
}

@Singleton
class ModelManifestRepositoryImpl
@Inject
constructor(
  private val service: ModelCatalogService,
  private val localDataSource: ModelCatalogLocalDataSource,
  private val json: Json,
  private val deviceIdentityProvider: DeviceIdentityProvider,
  private val telemetryReporter: TelemetryReporter,
  private val huggingFaceManifestFetcher: HuggingFaceManifestFetcher,
  private val clock: Clock = Clock.System,
) : ModelManifestRepository {
  override suspend fun refreshManifest(
    modelId: String,
    version: String,
  ): NanoAIResult<DownloadManifest> {
    val modelRecord = localDataSource.getModel(modelId)?.model
    val locator = modelRecord?.let { ModelManifestLocator.parse(it.manifestUrl) }

    return when (locator) {
      is ModelManifestLocator.HuggingFace -> resolveHuggingFaceManifest(modelId, version, locator)
      else ->
        runCatching { service.getModelManifest(modelId, version) }
          .mapCatching { dto -> validateManifest(dto) }
          .mapCatching { dto -> dto.toDomain(clock.now()) }
          .fold(
            onSuccess = { manifest ->
              localDataSource.cacheManifest(manifest.toEntity())
              localDataSource.updateIntegrityMetadata(
                modelId = manifest.modelId,
                checksum = manifest.checksumSha256,
                signature = manifest.signature,
              )
              NanoAIResult.success(manifest)
            },
            onFailure = { error -> failure("Failed to fetch manifest", modelId, error) },
          )
    }
  }

  override suspend fun reportVerification(
    modelId: String,
    version: String,
    checksumSha256: String,
    status: VerificationOutcome,
    failureReason: String?,
  ): NanoAIResult<Unit> {
    val locator =
      localDataSource.getModel(modelId)?.model?.let { ModelManifestLocator.parse(it.manifestUrl) }
    if (locator is ModelManifestLocator.HuggingFace) {
      return NanoAIResult.success(Unit)
    }
    val request =
      ManifestVerificationRequestDto(
        version = version,
        checksumSha256 = checksumSha256,
        deviceId = deviceIdentityProvider.deviceId(),
        verifiedAt = clock.now().toString(),
        status =
          when (status) {
            VerificationOutcome.SUCCESS -> ManifestVerificationStatusDto.SUCCESS
            VerificationOutcome.CORRUPTED -> ManifestVerificationStatusDto.CORRUPTED
          },
        failureReason = failureReason,
      )

    return runCatching { service.verifyModelPackage(modelId, request) }
      .fold(
        onSuccess = { response ->
          when (response.status) {
            ManifestVerificationResponseStatusDto.ACCEPTED -> NanoAIResult.success(Unit)
            ManifestVerificationResponseStatusDto.RETRY -> {
              val retryResult =
                NanoAIResult.recoverable(
                  message = "Verification deferred by server",
                  retryAfterSeconds =
                    response.nextRetryAfterSeconds?.coerceIn(0, MAX_RETRY_AFTER_SECONDS)?.toLong(),
                  telemetryId = UUID.randomUUID().toString(),
                  context = mapOf("modelId" to modelId, "version" to version),
                )
              telemetryReporter.report(
                source = "ModelManifestRepository.reportVerification",
                result = retryResult,
                extraContext = mapOf("modelId" to modelId, "version" to version),
              )
              retryResult
            }
          }
        },
        onFailure = { error -> failure("Failed to report verification", modelId, error) },
      )
  }

  private fun validateManifest(dto: ModelManifestDto): ModelManifestDto {
    require(dto.modelId.isNotBlank()) { "Manifest missing modelId" }
    require(dto.version.isNotBlank()) { "Manifest missing version" }
    require(HEX_64_REGEX.matches(dto.checksumSha256)) { "Manifest checksum invalid" }
    require(dto.sizeBytes > 0) { "Manifest size must be > 0" }
    val parsedUrl = dto.downloadUrl.toHttpUrlOrNull()
    require(parsedUrl != null && parsedUrl.isHttps) { "Manifest downloadUrl must be HTTPS" }
    dto.signature?.let { signature ->
      require(signature.isNotBlank()) { "Manifest signature must not be blank when provided" }
    }
    return dto
  }

  private fun <T> failure(
    message: String,
    modelId: String,
    error: Throwable,
  ): NanoAIResult<T> {
    if (error is HttpException) {
      return httpError(message, modelId, error)
    }

    val result: NanoAIResult<T> =
      when (error) {
        is IllegalArgumentException ->
          NanoAIResult.fatal(
            message =
              buildString {
                append(message)
                error.message
                  ?.takeIf { it.isNotBlank() }
                  ?.let {
                    append(": ")
                    append(it)
                  }
              },
            supportContact = SUPPORT_CONTACT,
            telemetryId = UUID.randomUUID().toString(),
            cause = error,
          )
        else ->
          NanoAIResult.recoverable(
            message = message,
            retryAfterSeconds = DEFAULT_RETRY_AFTER_SECONDS,
            telemetryId = UUID.randomUUID().toString(),
            cause = error,
            context = mapOf("modelId" to modelId),
          )
      }
    telemetryReporter.report(
      source = "ModelManifestRepository",
      result = result as NanoAIResult<*>,
      extraContext = mapOf("modelId" to modelId),
    )
    return result
  }

  private fun <T> httpError(
    message: String,
    modelId: String,
    exception: HttpException,
  ): NanoAIResult<T> {
    val errorEnvelope = decodeErrorEnvelope(exception)
    val result: NanoAIResult<T> =
      if (exception.code() in CLIENT_ERROR_STATUS_RANGE) {
        NanoAIResult.fatal(
          message = errorEnvelope?.message ?: message,
          supportContact = SUPPORT_CONTACT,
          telemetryId = errorEnvelope?.telemetryId ?: UUID.randomUUID().toString(),
          cause = exception,
        )
      } else {
        NanoAIResult.recoverable(
          message = errorEnvelope?.message ?: message,
          retryAfterSeconds =
            errorEnvelope?.retryAfterSeconds?.toLong() ?: DEFAULT_RETRY_AFTER_SECONDS,
          telemetryId = errorEnvelope?.telemetryId ?: UUID.randomUUID().toString(),
          cause = exception,
          context = errorEnvelope?.details ?: mapOf("modelId" to modelId),
        )
      }
    telemetryReporter.report(
      source = "ModelManifestRepository",
      result = result as NanoAIResult<*>,
      extraContext =
        buildMap {
          put("modelId", modelId)
          put("statusCode", exception.code().toString())
          errorEnvelope?.details?.let { putAll(it) }
        },
    )
    return result
  }

  private fun decodeErrorEnvelope(exception: HttpException): ErrorEnvelopeDto? {
    val errorBody = exception.response()?.errorBody()?.string() ?: return null
    return runCatching { json.decodeFromString(ErrorEnvelopeDto.serializer(), errorBody) }
      .getOrNull()
  }

  companion object {
    private val CLIENT_ERROR_STATUS_RANGE =
      HttpURLConnection.HTTP_BAD_REQUEST..MAX_CLIENT_ERROR_STATUS
    private const val SUPPORT_CONTACT = "support@nanoai.app"
    private const val DEFAULT_RETRY_AFTER_SECONDS = 60L
    private const val MAX_CLIENT_ERROR_STATUS = 499
  }

  private suspend fun resolveHuggingFaceManifest(
    modelId: String,
    version: String,
    locator: ModelManifestLocator.HuggingFace,
  ): NanoAIResult<DownloadManifest> {
    val revision = locator.revision?.takeIf { it.isNotBlank() } ?: version
    val request =
      HuggingFaceManifestRequest(
        modelId = modelId,
        repository = locator.repository,
        revision = revision,
        artifactPath = locator.artifactPath,
        version = version,
      )

    return runCatching { huggingFaceManifestFetcher.fetchManifest(request) }
      .fold(
        onSuccess = { manifest ->
          localDataSource.cacheManifest(manifest.toEntity())
          localDataSource.updateIntegrityMetadata(
            modelId = manifest.modelId,
            checksum = manifest.checksumSha256,
            signature = manifest.signature,
          )
          NanoAIResult.success(manifest)
        },
        onFailure = {
          failure(
            message = "Failed to fetch Hugging Face manifest",
            modelId = modelId,
            error = it,
          )
        },
      )
  }
}
