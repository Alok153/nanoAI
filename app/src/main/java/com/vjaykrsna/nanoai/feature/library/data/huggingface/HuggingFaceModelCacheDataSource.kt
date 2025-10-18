package com.vjaykrsna.nanoai.feature.library.data.huggingface

import com.vjaykrsna.nanoai.feature.library.data.huggingface.cache.HuggingFaceModelCacheMapper
import com.vjaykrsna.nanoai.feature.library.data.huggingface.dao.HuggingFaceModelCacheDao
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Provides cached Hugging Face model summaries backed by Room. */
@Singleton
class HuggingFaceModelCacheDataSource
@Inject
constructor(
  private val dao: HuggingFaceModelCacheDao,
  private val clock: Clock = Clock.System,
) {
  suspend fun getFreshModels(limit: Int, offset: Int, ttl: Instant): List<HuggingFaceModelSummary> {
    return dao.getFreshModels(ttl, limit, offset).map(HuggingFaceModelCacheMapper::toDomain)
  }

  suspend fun storeModels(models: List<HuggingFaceModelSummary>) {
    if (models.isEmpty()) return
    val now = clock.now()
    val entities = models.map { model -> HuggingFaceModelCacheMapper.toEntity(model, now) }
    dao.upsertAll(entities)
  }

  suspend fun replaceAll(models: List<HuggingFaceModelSummary>) {
    val now = clock.now()
    val entities = models.map { model -> HuggingFaceModelCacheMapper.toEntity(model, now) }
    dao.replaceAll(entities)
  }

  suspend fun pruneOlderThan(ttl: Instant) {
    dao.deleteOlderThan(ttl)
  }
}
