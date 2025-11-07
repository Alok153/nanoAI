package com.vjaykrsna.nanoai.core.common

/** Extension functions to make NanoAIResult compatible with Result-style usage. */
inline fun <T, R> NanoAIResult<T>.fold(
  onSuccess: (value: T) -> R,
  onFailure: (error: NanoAIResult.RecoverableError) -> R,
): R =
  when (this) {
    is NanoAIResult.Success -> onSuccess(value)
    is NanoAIResult.RecoverableError -> onFailure(this)
    is NanoAIResult.FatalError ->
      onFailure(
        NanoAIResult.recoverable(
          message = message,
          telemetryId = telemetryId,
          cause = cause,
          context = context,
        )
      )
  }

inline fun <T> NanoAIResult<T>.onSuccess(action: (value: T) -> Unit): NanoAIResult<T> {
  if (this is NanoAIResult.Success) {
    action(value)
  }
  return this
}

inline fun <T> NanoAIResult<T>.onFailure(
  action: (error: NanoAIResult.RecoverableError) -> Unit
): NanoAIResult<T> {
  if (this is NanoAIResult.RecoverableError) {
    action(this)
  } else if (this is NanoAIResult.FatalError) {
    action(
      NanoAIResult.recoverable(
        message = message,
        telemetryId = telemetryId,
        cause = cause,
        context = context,
      )
    )
  }
  return this
}
