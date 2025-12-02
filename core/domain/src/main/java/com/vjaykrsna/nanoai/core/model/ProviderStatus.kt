package com.vjaykrsna.nanoai.core.model

/** Health/connectivity status of an API provider. */
enum class ProviderStatus {
  /** Provider is operational and responding normally. */
  OK,

  /** API key is invalid or authentication failed. */
  UNAUTHORIZED,

  /** Rate limit exceeded, quota exhausted. */
  RATE_LIMITED,

  /** Provider returned an error or is unreachable. */
  ERROR,

  /** Status has not been checked yet or is unknown. */
  UNKNOWN,
}
