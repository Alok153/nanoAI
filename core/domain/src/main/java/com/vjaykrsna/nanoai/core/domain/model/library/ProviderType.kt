package com.vjaykrsna.nanoai.core.domain.model.library

/** Type of AI model runtime provider. Determines how the model is loaded and executed. */
enum class ProviderType {
  /** Google MediaPipe GenAI with LiteRT runtime. */
  MEDIA_PIPE,

  /** TensorFlow Lite standalone runtime. */
  TFLITE,

  /** MLC LLM (Machine Learning Compilation) runtime. */
  MLC_LLM,

  /** ONNX Runtime for cross-platform inference. */
  ONNX_RUNTIME,

  /** Cloud-based API (not a local model). */
  CLOUD_API,

  /** Leap AI for local model inference. */
  LEAP,
}
