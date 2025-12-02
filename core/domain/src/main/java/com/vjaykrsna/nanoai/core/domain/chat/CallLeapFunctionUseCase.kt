package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import javax.inject.Inject

/** A use case that calls a function on a Leap model. */
class CallLeapFunctionUseCase @Inject constructor() {
  /**
   * Calls a function on a Leap model.
   *
   * @param functionName The name of the function to call.
   * @param args The arguments to pass to the function.
   * @return A [NanoAIResult] describing success or telemetry-rich failure state.
   */
  suspend operator fun invoke(functionName: String, args: Map<String, Any>): NanoAIResult<Any> {
    // TODO: Implement function calling with the Leap SDK.
    return NanoAIResult.recoverable(
      message = "Function calling is not yet supported",
      telemetryId = "LEAP_FUNCTION_UNSUPPORTED",
      context = mapOf("functionName" to functionName, "argCount" to args.size.toString()),
    )
  }
}
