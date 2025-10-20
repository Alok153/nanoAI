package com.vjaykrsna.nanoai.coverage.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Reads coverage dashboard snapshots from bundled assets to power the offline presenter. */
class CoverageDashboardRepository
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val json: Json = Json { ignoreUnknownKeys = true },
) {

  suspend fun loadSnapshot(): CoverageDashboardPayload =
    withContext(Dispatchers.IO) {
      val payload =
        context.assets.open(SNAPSHOT_ASSET_PATH).bufferedReader().use { reader ->
          reader.readText()
        }
      try {
        json.decodeFromString(CoverageDashboardPayload.serializer(), payload)
      } catch (error: SerializationException) {
        throw IllegalStateException("Failed to parse coverage dashboard payload", error)
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
