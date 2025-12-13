package com.vjaykrsna.nanoai.feature.chat.data

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogRepository
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.core.runtime.LocalRuntimeGateway
import com.vjaykrsna.nanoai.feature.chat.domain.LocalInferenceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Repository implementation that inspects installed on-device models and runtime readiness. */
@Singleton
class DefaultLocalInferenceRepository
@Inject
constructor(
  private val modelCatalogRepository: ModelCatalogRepository,
  private val localRuntimeGateway: LocalRuntimeGateway,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LocalInferenceRepository {

  override fun observeInstalledLocalModels(): Flow<List<ModelPackage>> =
    modelCatalogRepository.observeInstalledModels().map { packages -> packages.localCandidates() }

  override suspend fun getInstalledLocalModels(): List<ModelPackage> =
    withContext(ioDispatcher) { modelCatalogRepository.getInstalledModels().localCandidates() }

  override suspend fun isModelReady(modelId: String): Boolean =
    withContext(ioDispatcher) { localRuntimeGateway.isModelReady(modelId) }

  private fun List<ModelPackage>.localCandidates(): List<ModelPackage> = filter { model ->
    model.providerType != ProviderType.CLOUD_API && model.installState == InstallState.INSTALLED
  }
}
