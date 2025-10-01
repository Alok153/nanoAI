package com.vjaykrsna.nanoai.core.network

import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.network.dto.CompletionRequestDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionResponseDto
import com.vjaykrsna.nanoai.core.network.dto.GatewayErrorDto
import com.vjaykrsna.nanoai.core.network.dto.ModelListResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * Client wrapper that configures Retrofit dynamically per provider configuration.
 */
@Singleton
class CloudGatewayClient
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val json: Json,
    ) {
        private val jsonMediaType = "application/json".toMediaType()

        /** Execute a text completion request against the configured provider. */
        suspend fun createCompletion(
            provider: APIProviderConfig,
            request: CompletionRequestDto,
        ): CloudGatewayResult<CompletionResponseDto> =
            execute(provider) { service ->
                service.createCompletion(request)
            }

        /** Retrieve the provider's available model list. */
        suspend fun listModels(provider: APIProviderConfig): CloudGatewayResult<ModelListResponseDto> =
            execute(provider) { service ->
                service.listModels()
            }

        @OptIn(ExperimentalTime::class)
        private suspend fun <T : Any> execute(
            provider: APIProviderConfig,
            block: suspend (CloudGatewayService) -> T,
        ): CloudGatewayResult<T> =
            withContext(Dispatchers.IO) {
                val service = createService(provider)
                return@withContext try {
                    val timedResult = measureTimedValue { block(service) }
                    CloudGatewayResult.Success(timedResult.value, timedResult.duration.inWholeMilliseconds)
                } catch (throwable: Throwable) {
                    mapError(throwable)
                }
            }

        private fun createService(provider: APIProviderConfig): CloudGatewayService {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl(normalizeBaseUrl(provider.baseUrl))
                    .client(buildClient(provider))
                    .addConverterFactory(json.asConverterFactory(jsonMediaType))
                    .build()
            return retrofit.create(CloudGatewayService::class.java)
        }

        private fun buildClient(provider: APIProviderConfig): OkHttpClient =
            okHttpClient
                .newBuilder()
                .addInterceptor { chain ->
                    val requestBuilder: Request.Builder =
                        chain
                            .request()
                            .newBuilder()
                            .header("User-Agent", "nanoAI/0.1")

                    when (provider.apiType) {
                        APIType.OPENAI_COMPATIBLE, APIType.GEMINI ->
                            requestBuilder.header("Authorization", "Bearer ${provider.apiKey}")
                        APIType.CUSTOM ->
                            requestBuilder.header("X-API-Key", provider.apiKey)
                    }

                    // Explicit JSON content type for POST requests
                    if (chain.request().method.equals("POST", ignoreCase = true)) {
                        requestBuilder.header("Content-Type", "application/json")
                    }

                    chain.proceed(requestBuilder.build())
                }.build()

        private fun normalizeBaseUrl(baseUrl: String): String = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"

        private fun mapError(throwable: Throwable): CloudGatewayResult<Nothing> =
            when (throwable) {
                is HttpException -> {
                    when (throwable.code()) {
                        401 -> CloudGatewayResult.Unauthorized
                        429 -> CloudGatewayResult.RateLimited
                        else -> {
                            val message =
                                throwable.response()?.errorBody()?.use { body ->
                                    body.string()
                                }
                            val parsedMessage =
                                message?.let {
                                    runCatching { json.decodeFromString(GatewayErrorDto.serializer(), it).message }
                                        .getOrNull() ?: it
                                }
                            CloudGatewayResult.HttpError(throwable.code(), parsedMessage)
                        }
                    }
                }
                is java.io.IOException -> CloudGatewayResult.NetworkError(throwable)
                else -> CloudGatewayResult.UnknownError(throwable)
            }
    }
