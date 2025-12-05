package com.vjaykrsna.nanoai.core.network

import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.network.dto.CompletionRequestDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionResponseDto
import com.vjaykrsna.nanoai.core.network.dto.GatewayErrorDto
import com.vjaykrsna.nanoai.core.network.dto.ModelListResponseDto
import com.vjaykrsna.nanoai.core.security.ProviderCredentialStore
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Client wrapper that configures Retrofit dynamically per provider configuration. */
@Singleton
class CloudGatewayClient
@Inject
constructor(
  private val okHttpClient: OkHttpClient,
  private val json: Json,
  private val providerCredentialStore: ProviderCredentialStore,
) {
  private val jsonMediaType = "application/json".toMediaType()

  /** Execute a text completion request against the configured provider. */
  suspend fun createCompletion(
    provider: APIProviderConfig,
    request: CompletionRequestDto,
  ): CloudGatewayResult<CompletionResponseDto> =
    execute(provider) { service -> service.createCompletion(request) }

  /** Retrieve the provider's available model list. */
  suspend fun listModels(provider: APIProviderConfig): CloudGatewayResult<ModelListResponseDto> =
    execute(provider) { service -> service.listModels() }

  @OptIn(ExperimentalTime::class)
  private suspend fun <T : Any> execute(
    provider: APIProviderConfig,
    block: suspend (CloudGatewayService) -> T,
  ): CloudGatewayResult<T> =
    withContext(Dispatchers.IO) {
      val credentialValue = providerCredentialStore.resolve(provider.credentialId)
      if (credentialValue.isNullOrBlank() && provider.requiresCredential()) {
        return@withContext CloudGatewayResult.Unauthorized
      }
      val service = createService(provider, credentialValue)
      val timedResult = runCatching { measureTimedValue { block(service) } }
      timedResult.fold(
        onSuccess = { CloudGatewayResult.Success(it.value, it.duration.inWholeMilliseconds) },
        onFailure = { mapError(it) },
      )
    }

  private fun createService(provider: APIProviderConfig, credential: String?): CloudGatewayService {
    val retrofit =
      Retrofit.Builder()
        .baseUrl(normalizeBaseUrl(provider.baseUrl))
        .client(buildClient(provider, credential))
        .addConverterFactory(json.asConverterFactory(jsonMediaType))
        .build()
    return retrofit.create(CloudGatewayService::class.java)
  }

  private fun buildClient(provider: APIProviderConfig, credential: String?): OkHttpClient =
    okHttpClient
      .newBuilder()
      .addInterceptor { chain ->
        val requestBuilder: Request.Builder =
          chain.request().newBuilder().header("User-Agent", "nanoAI/0.1")

        when (provider.apiType) {
          APIType.OPENAI_COMPATIBLE,
          APIType.GEMINI -> credential?.let { requestBuilder.header("Authorization", "Bearer $it") }
          APIType.CUSTOM -> credential?.let { requestBuilder.header("X-API-Key", it) }
        }

        // Explicit JSON content type for POST requests
        if (chain.request().method.equals("POST", ignoreCase = true)) {
          requestBuilder.header("Content-Type", "application/json")
        }

        chain.proceed(requestBuilder.build())
      }
      .build()

  private fun normalizeBaseUrl(baseUrl: String): String =
    if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"

  private fun APIProviderConfig.requiresCredential(): Boolean =
    when (apiType) {
      APIType.OPENAI_COMPATIBLE,
      APIType.GEMINI,
      APIType.CUSTOM -> true
    }

  private fun mapError(throwable: Throwable): CloudGatewayResult<Nothing> =
    when (throwable) {
      is HttpException -> mapHttpException(throwable)
      is java.io.IOException -> CloudGatewayResult.NetworkError(throwable)
      else -> CloudGatewayResult.UnknownError(throwable)
    }

  private fun mapHttpException(exception: HttpException): CloudGatewayResult<Nothing> =
    when (exception.code()) {
      HttpURLConnection.HTTP_UNAUTHORIZED -> CloudGatewayResult.Unauthorized
      HTTP_TOO_MANY_REQUESTS -> CloudGatewayResult.RateLimited
      else -> CloudGatewayResult.HttpError(exception.code(), extractGatewayMessage(exception))
    }

  private fun extractGatewayMessage(exception: HttpException): String? {
    val message = exception.response()?.errorBody()?.use { body -> body.string() }
    return message?.let { raw -> parseGatewayMessage(raw) ?: raw }
  }

  private fun parseGatewayMessage(payload: String): String? =
    runCatching { json.decodeFromString(GatewayErrorDto.serializer(), payload).message }.getOrNull()

  companion object {
    private const val HTTP_TOO_MANY_REQUESTS = 429
  }
}
