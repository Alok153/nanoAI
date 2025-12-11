package com.vjaykrsna.nanoai.core.model

/**
 * Feature-facing alias for the shared NanoAI result contract.
 *
 * Keeps feature layers decoupled from the internal common module package while reusing the same
 * telemetry-aware result semantics.
 */
typealias NanoAIResult<T> = com.vjaykrsna.nanoai.core.common.NanoAIResult<T>

typealias NanoAISuccess<T> = com.vjaykrsna.nanoai.core.common.NanoAIResult.Success<T>

typealias NanoAIRecoverableError = com.vjaykrsna.nanoai.core.common.NanoAIResult.RecoverableError

typealias NanoAIFatalError = com.vjaykrsna.nanoai.core.common.NanoAIResult.FatalError

@Suppress("FunctionName")
fun <T> NanoAIResultSuccess(value: T): NanoAIResult<T> =
  com.vjaykrsna.nanoai.core.common.NanoAIResult.success(value)

@Suppress("FunctionName")
fun NanoAIResultSuccess(): NanoAIResult<Unit> =
  com.vjaykrsna.nanoai.core.common.NanoAIResult.success(Unit)
