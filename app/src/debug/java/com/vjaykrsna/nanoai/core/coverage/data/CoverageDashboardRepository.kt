package com.vjaykrsna.nanoai.core.coverage.data

import android.content.Context
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Repository interface for coverage dashboard data. */
interface CoverageDashboardRepository {
  @OneShot("Load coverage dashboard snapshot")
  suspend fun loadSnapshot(): NanoAIResult<CoverageDashboardPayload>
}

/** Reads coverage dashboard snapshots from bundled assets to power the offline presenter. */
class CoverageDashboardRepositoryImpl
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val json: Json = Json { ignoreUnknownKeys = true },
) : CoverageDashboardRepository {

  @OneShot("Load coverage dashboard snapshot")
  override suspend fun loadSnapshot(): NanoAIResult<CoverageDashboardPayload> =
    withContext(Dispatchers.IO) {
      try {
        val payload =
          context.assets.open(SNAPSHOT_ASSET_PATH).bufferedReader().use { reader ->
            reader.readText()
          }
        val decoded = json.decodeFromString(CoverageDashboardPayload.serializer(), payload)
        NanoAIResult.success(decoded)
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (serialization: SerializationException) {
        NanoAIResult.recoverable(
          message = "Coverage dashboard payload invalid",
          cause = serialization,
          context = mapOf("path" to SNAPSHOT_ASSET_PATH),
        )
      } catch (io: IOException) {
        NanoAIResult.recoverable(
          message = "Unable to read coverage dashboard snapshot",
          cause = io,
          context = mapOf("path" to SNAPSHOT_ASSET_PATH),
        )
      }
    }

  companion object {
    private const val SNAPSHOT_ASSET_PATH = "coverage/dashboard.json"
  }
}

@Serializable
data class CoverageDashboardPayload(
  val buildId: String,
  val generatedAt: String,
  val layers: List<LayerPayload>,
  val trend: Map<String, Double> = emptyMap(),
  val risks: List<RiskPayload> = emptyList(),
)

@Serializable
data class LayerPayload(val layer: String, val coverage: Double, val threshold: Double)

@Serializable
data class RiskPayload(
  val riskId: String,
  val title: String,
  val severity: String,
  val status: String,
)
