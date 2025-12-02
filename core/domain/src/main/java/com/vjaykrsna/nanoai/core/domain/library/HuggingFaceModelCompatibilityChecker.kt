package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import javax.inject.Inject
import javax.inject.Singleton

/** Determines if a Hugging Face model is compatible with local runtimes. */
@Singleton
class HuggingFaceModelCompatibilityChecker @Inject constructor() {

  /**
   * Checks if a Hugging Face model can be downloaded and run locally. Returns the appropriate
   * ProviderType if compatible, null otherwise.
   */
  fun checkCompatibility(model: HuggingFaceModelSummary): ProviderType? {
    // Skip incompatible models
    if (model.isDisabled || model.hasGatedAccess || model.isPrivate) {
      return null
    }

    return when {
      isCompatibleWithMLCLLM(model) -> ProviderType.MLC_LLM
      isCompatibleWithMediaPipe(model) -> ProviderType.MEDIA_PIPE
      else -> null
    }
  }

  private fun isCompatibleWithMLCLLM(model: HuggingFaceModelSummary): Boolean {
    // MLC LLM supports transformers-based models
    val compatibleLibraries = setOf("transformers", "diffusers")
    val textGenerationTasks =
      setOf("text-generation", "text2text-generation", "causal-lm", "seq2seq-lm")
    val compatibleArchitectures =
      setOf(
        "transformer",
        "llama",
        "gpt",
        "bert",
        "t5",
        "bart",
        "roberta",
        "electra",
        "albert",
        "distilbert",
        "gpt2",
        "gpt_neo",
        "gpt_neox",
        "opt",
        "bloom",
        "galactica",
        "mt5",
      )

    return model.libraryName?.lowercase() in compatibleLibraries ||
      model.pipelineTag?.lowercase() in textGenerationTasks ||
      model.architectures.any { arch ->
        compatibleArchitectures.any { compatible -> arch.contains(compatible, ignoreCase = true) }
      }
  }

  private fun isCompatibleWithMediaPipe(model: HuggingFaceModelSummary): Boolean {
    // MediaPipe has more restrictive compatibility
    // Currently limited to models that can be converted to LiteRT format
    // This is more conservative - only enable for known working models

    val compatibleModelTypes =
      setOf(
        "gemma" // Google's Gemma models
      )

    if (model.modelType?.lowercase() in compatibleModelTypes) {
      return true
    }

    // For now, be conservative with MediaPipe compatibility
    // Most HF models need conversion to LiteRT format first
    return false
  }
}
