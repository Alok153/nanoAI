package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.DeliveryType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock

/**
 * Converts compatible Hugging Face models to ModelPackage for download.
 *
 * This enables the existing download infrastructure to handle HF models by creating the appropriate
 * manifest URLs and metadata.
 */
@Singleton
class HuggingFaceToModelPackageConverter
@Inject
constructor(
  private val compatibilityChecker: HuggingFaceModelCompatibilityChecker,
  private val clock: Clock = Clock.System,
) {

  /**
   * Converts a Hugging Face model to a ModelPackage if it's compatible with local runtimes. Returns
   * null if the model is not compatible.
   */
  fun convertIfCompatible(hfModel: HuggingFaceModelSummary): ModelPackage? {
    val providerType = compatibilityChecker.checkCompatibility(hfModel) ?: return null

    return ModelPackage(
      modelId = generateModelId(hfModel),
      displayName = hfModel.displayName,
      version = hfModel.lastModified?.toString() ?: "latest",
      providerType = providerType,
      deliveryType = DeliveryType.CLOUD_FALLBACK, // HF models are fetched from cloud
      minAppVersion = 1, // Assume compatible with current version
      sizeBytes = hfModel.totalSizeBytes ?: 0L,
      capabilities = determineCapabilities(hfModel),
      installState = com.vjaykrsna.nanoai.feature.library.model.InstallState.NOT_INSTALLED,
      manifestUrl = buildManifestUrl(hfModel, providerType),
      createdAt = hfModel.createdAt ?: clock.now(),
      updatedAt = hfModel.lastModified ?: clock.now(),
      author = hfModel.author,
      license = hfModel.license,
      languages = hfModel.languages,
      architectures = hfModel.architectures,
      modelType = hfModel.modelType,
      summary = hfModel.summary,
      description = hfModel.description,
    )
  }

  private fun generateModelId(hfModel: HuggingFaceModelSummary): String {
    // Use the HF model ID but ensure it doesn't conflict with curated models
    return "hf-${hfModel.modelId.replace("/", "-")}"
  }

  private fun determineCapabilities(hfModel: HuggingFaceModelSummary): Set<String> {
    val capabilities = mutableSetOf<String>()

    // Map pipeline tags to capabilities
    val pipelineCapabilities =
      mapOf(
        "text-generation" to "text-generation",
        "text2text-generation" to "text2text-generation",
        "image-text-to-text" to "multimodal",
        "text-to-image" to "image-generation",
        "automatic-speech-recognition" to "speech-recognition",
        "text-to-speech" to "text-to-speech",
        "translation" to "translation",
        "summarization" to "summarization",
        "question-answering" to "question-answering",
      )

    hfModel.pipelineTag?.let { tag ->
      pipelineCapabilities[tag.lowercase()]?.let { capabilities.add(it) }
    }

    // Map model tags to capabilities
    val tagCapabilities =
      mapOf(
        "multimodal" to "multimodal",
        "vision" to "vision",
        "audio" to "audio",
        "image" to "image",
      )

    hfModel.tags.forEach { tag -> tagCapabilities[tag.lowercase()]?.let { capabilities.add(it) } }

    // Default to text-generation if no specific capabilities detected
    return capabilities.ifEmpty { setOf("text-generation") }
  }

  private fun buildManifestUrl(
    hfModel: HuggingFaceModelSummary,
    providerType: ProviderType,
  ): String {
    // Construct hf:// URL that the existing infrastructure can handle
    // Format: hf://repository?artifact=artifactPath&revision=revision
    val repository = hfModel.modelId

    // Determine the artifact path based on provider and model characteristics
    val artifactPath = determineArtifactPath(providerType)

    // Use lastModified as revision, or default to "main"
    val revision = hfModel.lastModified?.toString() ?: "main"

    return buildString {
      append("hf://")
      append(repository)
      append("?artifact=")
      append(artifactPath)
      append("&revision=")
      append(revision)
    }
  }

  private fun determineArtifactPath(providerType: ProviderType): String =
    when (providerType) {
      ProviderType.MLC_LLM -> "model.safetensors"
      ProviderType.MEDIA_PIPE -> "model.bin"
      else -> "model.bin"
    }
}
