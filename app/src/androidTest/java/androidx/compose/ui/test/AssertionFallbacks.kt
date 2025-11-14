package androidx.compose.ui.test

/**
 * Temporary fallback implementations for missing Compose test assertion helpers on older
 * Compose BOMs. Remove when upgrading to a version that provides these helpers.
 */
@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.assertExists(message: String? = null): SemanticsNodeInteraction {
  fetchSemanticsNode(message ?: "Failed to assert SemanticsNode exists.")
  return this
}

@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.assertDoesNotExist(message: String? = null): SemanticsNodeInteraction {
  try {
    fetchSemanticsNode(message ?: "SemanticsNode still present.")
  } catch (_: AssertionError) {
    return this
  }
  throw AssertionError(message ?: "Expected SemanticsNode to be absent.")
}
