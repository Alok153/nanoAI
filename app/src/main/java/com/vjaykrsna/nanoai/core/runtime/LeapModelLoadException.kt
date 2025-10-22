package com.vjaykrsna.nanoai.core.runtime

/** An exception that is thrown when a Leap model fails to load. */
class LeapModelLoadException(message: String, cause: Throwable) : Exception(message, cause)
