package com.vjaykrsna.nanoai.core.data.library.huggingface

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.HuggingFaceService
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto.HuggingFaceModelListingDto
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto.ModelCardDataDto
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto.ModelConfigDto
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Retrofit-backed implementation for [HuggingFaceCatalogRepository]. */
@Singleton
class HuggingFaceCatalogRepositoryImpl
@Inject
constructor(
  private val service: HuggingFaceService,
  private val cacheDataSource: HuggingFaceModelCacheDataSource,
  private val connectivityStatusProvider: ConnectivityStatusProvider,
  private val clock: Clock = Clock.System,
) : HuggingFaceCatalogRepository {
  @Suppress("ReturnCount")
  override suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>> {
    if (!connectivityStatusProvider.isOnline()) {
      return NanoAIResult.recoverable(
        message =
          "Unable to fetch models from Hugging Face. Please check your internet connection.",
        retryAfterSeconds = 60L,
        telemetryId = null,
        cause = null,
        context = mapOf("query" to query.toString()),
      )
    }

    return try {
      val cacheExpiry = clock.now().minus(DEFAULT_CACHE_TTL)
      val cachedModels = getCachedModelsIfApplicable(query, cacheExpiry)
      if (cachedModels != null) {
        NanoAIResult.success(cachedModels)
      } else {
        val models = fetchModelsFromNetwork(query, cacheExpiry)
        NanoAIResult.success(models)
      }
    } catch (e: IOException) {
      NanoAIResult.recoverable(
        message =
          "Unable to fetch models from Hugging Face. Please check your internet connection.",
        retryAfterSeconds = 60L,
        telemetryId = null,
        cause = e,
        context = mapOf("query" to query.toString()),
      )
    } catch (e: Exception) {
      NanoAIResult.fatal(
        message = "An unexpected error occurred while fetching models.",
        supportContact = null,
        cause = e,
      )
    }
  }

  private suspend fun getCachedModelsIfApplicable(
    query: HuggingFaceCatalogQuery,
    cacheExpiry: Instant,
  ): List<HuggingFaceModelSummary>? {
    if (hasActiveFilters(query)) {
      return null
    }
    val cachedResults =
      cacheDataSource.getFreshModels(limit = query.limit, offset = query.offset, ttl = cacheExpiry)
    return cachedResults.takeIf { it.isNotEmpty() }
  }

  private fun hasActiveFilters(query: HuggingFaceCatalogQuery): Boolean =
    query.search?.isNotBlank() == true ||
      query.pipelineTag?.takeIf { it.isNotBlank() } != null ||
      query.library?.takeIf { it.isNotBlank() } != null ||
      query.includePrivate ||
      query.sortField != null

  private suspend fun fetchModelsFromNetwork(
    query: HuggingFaceCatalogQuery,
    cacheExpiry: Instant,
  ): List<HuggingFaceModelSummary> =
    service
      .listModels(
        search = query.search?.takeIf { it.isNotBlank() },
        sort = query.sortField?.apiValue,
        direction = query.sortDirection?.apiValue?.takeIf { query.sortField != null },
        limit = query.limit,
        skip = query.offset.takeIf { it > 0 },
        pipelineTag = query.pipelineTag?.takeIf { it.isNotBlank() },
        library = query.library?.takeIf { it.isNotBlank() },
        includePrivate = query.includePrivate.takeIf { it },
        expandAuthor = "author",
        expandDownloads = "downloads",
        expandLikes = "likes",
        expandPipelineTag = "pipeline_tag",
        expandTags = "tags",
        expandLibraryName = "library_name",
        expandCreatedAt = "createdAt",
        expandLastModified = "lastModified",
        expandTrendingScore = "trendingScore",
        expandPrivate = "private",
        expandGated = "gated",
        expandDisabled = "disabled",
        expandCardData = "cardData",
        expandConfig = "config",
        expandBaseModels = "baseModels",
        expandSiblings = "siblings",
      )
      .map { it.toDomain() }
      .also { models ->
        cacheDataSource.storeModels(models)
        cacheDataSource.pruneOlderThan(cacheExpiry)
      }

  internal fun JsonElement?.asStringList(): List<String> {
    return when (this) {
      null -> emptyList()
      is JsonPrimitive ->
        jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let(::listOf)
          ?: emptyList()
      is JsonArray ->
        mapNotNull { element ->
          element.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        }
      else -> emptyList()
    }
  }

  internal fun JsonElement?.asString(): String? {
    return when (this) {
      null -> null
      is JsonPrimitive -> jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
      is JsonArray ->
        firstOrNull()?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
      else -> null
    }
  }

  internal fun JsonElement.asBoolean(): Boolean? {
    return when (this) {
      is JsonPrimitive -> jsonPrimitive.contentOrNull?.toBoolean()
      else -> null
    }
  }

  private fun HuggingFaceModelListingDto.toDomain(): HuggingFaceModelSummary {
    val resolvedId = resolveModelId()
    val normalizedCard = cardData?.toNormalizedCardData()
    val normalizedConfig = config?.toNormalizedConfigData()
    val normalizedBaseModels = normalizeBaseModels()
    val totalSize = calculateTotalSize()
    val isGated = gated?.asBoolean() ?: false

    return HuggingFaceModelSummary(
      modelId = resolvedId,
      displayName = resolvedId,
      author = author?.takeIf { it.isNotBlank() },
      pipelineTag = pipelineTag,
      libraryName = libraryName?.takeIf { it.isNotBlank() },
      tags = normalizeTags(),
      likes = likes ?: 0L,
      downloads = downloads ?: 0L,
      license = normalizedCard?.license,
      languages = normalizedCard?.languages ?: emptyList(),
      baseModel = normalizedCard?.baseModel,
      datasets = normalizedCard?.datasets ?: emptyList(),
      architectures = normalizedConfig?.architectures ?: emptyList(),
      modelType = normalizedConfig?.modelType,
      baseModelRelations = normalizedBaseModels,
      hasGatedAccess = isGated,
      isDisabled = disabled ?: false,
      totalSizeBytes = totalSize,
      summary = normalizedCard?.summary,
      description = normalizedCard?.description,
      trendingScore = trendingScore,
      createdAt = createdAt?.let(Instant::parse),
      lastModified = lastModified?.let(Instant::parse),
      isPrivate = isPrivate ?: false,
    )
  }

  private fun HuggingFaceModelListingDto.resolveModelId(): String =
    modelId ?: id ?: error("Hugging Face model missing identifier")

  private fun HuggingFaceModelListingDto.normalizeBaseModels(): List<String> =
    baseModels?.models.orEmpty().mapNotNull { candidate ->
      candidate.id?.trim()?.takeIf { it.isNotEmpty() }
    }

  private fun HuggingFaceModelListingDto.calculateTotalSize(): Long? =
    siblings.orEmpty().mapNotNull { it.sizeBytes }.takeUnless { it.isEmpty() }?.sum()

  private fun HuggingFaceModelListingDto.normalizeTags(): List<String> =
    tags.mapNotNull { it?.trim() }.filter { it.isNotBlank() }

  private fun ModelCardDataDto.toNormalizedCardData(): NormalizedCardData {
    val languageList = languages.asStringList()
    val baseModelId = baseModel.asString()
    val datasetList = datasets.asStringList()
    return NormalizedCardData(
      license = license,
      languages = languageList,
      baseModel = baseModelId,
      datasets = datasetList,
      pipelineTag = pipelineTag,
      summary = summary,
      description = description,
    )
  }

  private fun ModelConfigDto.toNormalizedConfigData(): NormalizedConfigData =
    NormalizedConfigData(architectures = architectures.orEmpty(), modelType = modelType)

  private companion object {
    val DEFAULT_CACHE_TTL: Duration = 6.hours
    val DEFAULT_EXPANSIONS =
      listOf(
        "author",
        "downloads",
        "likes",
        "pipeline_tag",
        "tags",
        "library_name",
        "createdAt",
        "lastModified",
        "trendingScore",
        "private",
        "gated",
        "disabled",
        "cardData",
        "config",
        "baseModels",
        "siblings",
      )
  }
}

private data class NormalizedCardData(
  val license: String?,
  val languages: List<String>,
  val baseModel: String?,
  val datasets: List<String>,
  val pipelineTag: String?,
  val summary: String?,
  val description: String?,
)

private data class NormalizedConfigData(val architectures: List<String>, val modelType: String?)
