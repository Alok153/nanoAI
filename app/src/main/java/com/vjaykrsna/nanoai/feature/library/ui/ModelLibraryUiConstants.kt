package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.runtime.Immutable

@Immutable
internal object ModelLibraryUiConstants {
  const val PERCENTAGE_MULTIPLIER = 100
  const val MAX_CAPABILITY_CHIPS = 4
  const val BYTES_PER_KIB = 1024.0
  const val BYTES_PER_MIB = BYTES_PER_KIB * 1024.0
  const val BYTES_PER_GIB = BYTES_PER_MIB * 1024.0
  const val SEARCH_FIELD_TAG = "model_library_search_field"
  const val LOADING_INDICATOR_TAG = "model_library_loading"
  const val SECTION_ATTENTION_TAG = "model_library_section_attention"
  const val SECTION_INSTALLED_TAG = "model_library_section_installed"
  const val SECTION_AVAILABLE_TAG = "model_library_section_available"
  const val LIST_TAG = "model_library_list"
}
