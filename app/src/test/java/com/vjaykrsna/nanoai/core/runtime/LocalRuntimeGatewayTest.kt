package com.vjaykrsna.nanoai.core.runtime

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalRuntimeGatewayTest {
  private val calls = AtomicInteger(0)
  private val dispatcher = StandardTestDispatcher()
  private val runtime = FakeRuntime(calls)
  private val gateway = DefaultLocalRuntimeGateway(dispatcher, runtime)

  @Test
  fun isModelReady_delegatesToRuntime() =
    runTest(dispatcher) {
      assertThat(gateway.isModelReady("model-1")).isTrue()
      assertThat(calls.get()).isEqualTo(1)
    }

  @Test
  fun hasReadyModel_delegatesToRuntime() =
    runTest(dispatcher) {
      val models = listOf(stubModel())

      assertThat(gateway.hasReadyModel(models)).isTrue()
      assertThat(calls.get()).isEqualTo(1)
    }

  @Test
  fun generate_returnsRuntimeResult() =
    runTest(dispatcher) {
      val request = LocalGenerationRequest(modelId = "m1", prompt = "hi")

      val result = gateway.generate(request)

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
      assertThat(calls.get()).isEqualTo(1)
    }

  private class FakeRuntime(private val calls: AtomicInteger) : LocalModelRuntime {
    override suspend fun isModelReady(modelId: String): Boolean {
      calls.incrementAndGet()
      return true
    }

    override suspend fun hasReadyModel(models: List<ModelPackage>): Boolean {
      calls.incrementAndGet()
      return true
    }

    override suspend fun generate(
      request: LocalGenerationRequest
    ): NanoAIResult<LocalGenerationResult> {
      calls.incrementAndGet()
      return NanoAIResult.success(
        LocalGenerationResult(
          text = "ok",
          latencyMs = 1,
          metadata = mapOf("provider" to ProviderType.MEDIA_PIPE.name),
        )
      )
    }
  }

  private fun stubModel(): ModelPackage =
    ModelPackage(
      modelId = "m1",
      displayName = "Model One",
      version = "1.0",
      providerType = ProviderType.MEDIA_PIPE,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1_000,
      capabilities = emptySet(),
      installState = InstallState.INSTALLED,
      manifestUrl = "https://example.com/manifest.json",
      createdAt = Instant.fromEpochMilliseconds(0),
      updatedAt = Instant.fromEpochMilliseconds(0),
    )
}
