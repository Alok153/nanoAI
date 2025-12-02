package com.vjaykrsna.nanoai.core.common

/** Enumeration of supported runtime types for model execution. */
enum class RuntimeType(val value: String) {
  MEDIA_PIPE("MEDIA_PIPE"),
  TFLITE("TFLITE"),
  MLC_LLM("MLC_LLM"),
  ONNX_RUNTIME("ONNX_RUNTIME"),
}
