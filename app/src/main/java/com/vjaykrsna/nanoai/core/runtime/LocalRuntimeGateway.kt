package com.vjaykrsna.nanoai.core.runtime

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Gateway that routes local inference calls through the IO dispatcher. */
interface LocalRuntimeGateway {
  suspend fun isModelReady(modelId: String): Boolean

  suspend fun hasReadyModel(models: List<ModelPackage>): Boolean

  suspend fun generate(request: LocalGenerationRequest): NanoAIResult<LocalGenerationResult>
}

@Singleton
class DefaultLocalRuntimeGateway
@Inject
constructor(
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val runtime: LocalModelRuntime,
) : LocalRuntimeGateway {

  override suspend fun isModelReady(modelId: String): Boolean =
    withContext(ioDispatcher) { runtime.isModelReady(modelId) }

  override suspend fun hasReadyModel(models: List<ModelPackage>): Boolean =
    withContext(ioDispatcher) { runtime.hasReadyModel(models) }

  override suspend fun generate(
    request: LocalGenerationRequest
  ): NanoAIResult<LocalGenerationResult> = withContext(ioDispatcher) { runtime.generate(request) }
}
