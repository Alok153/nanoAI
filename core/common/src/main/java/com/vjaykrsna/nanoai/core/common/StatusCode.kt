package com.vjaykrsna.nanoai.core.common

/** Enumeration of status codes used in model verification and other operations. */
enum class StatusCode(val value: String) {
  RETRY("RETRY"),
  INTEGRITY_FAILURE("INTEGRITY_FAILURE"),
}
