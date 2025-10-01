package com.vjaykrsna.nanoai.core.model

/**
 * Type of API provider/protocol.
 */
enum class APIType {
    /**
     * OpenAI-compatible API (supports /v1/completions, /v1/models endpoints).
     */
    OPENAI_COMPATIBLE,

    /**
     * Google Gemini API (custom protocol).
     */
    GEMINI,

    /**
     * Custom API with proprietary protocol.
     */
    CUSTOM,
}
