package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.runtime.Immutable

@Immutable
internal object ModelLibraryUiConstants {
  const val PERCENTAGE_MULTIPLIER = 100
  const val MAX_CAPABILITY_CHIPS = 4
  const val BYTES_PER_KIB = 1024.0
  const val BYTES_PER_MIB = BYTES_PER_KIB * 1024.0
  const val BYTES_PER_GIB = BYTES_PER_MIB * 1024.0
  const val THOUSAND = 1000.0
  const val TEN = 10.0
  const val ONE = 1.0
  const val COUNT_FORMAT_THRESHOLD_LONG = THOUSAND
  const val COUNT_DECIMAL_THRESHOLD = TEN
  const val COUNT_INTEGER_CHECK = ONE
  const val SEARCH_FIELD_TAG = "model_library_search_field"
  const val FILTER_TOGGLE_TAG = "model_library_filter_toggle"
  const val FILTER_PANEL_TAG = "model_library_filter_panel"
  const val LOADING_INDICATOR_TAG = "model_library_loading"
  const val DOWNLOAD_QUEUE_TAG = "model_library_download_queue"
  const val DOWNLOAD_QUEUE_HEADER_TAG = "model_library_download_queue_header"
  const val SECTION_ATTENTION_TAG = "model_library_section_attention"
  const val SECTION_INSTALLED_TAG = "model_library_section_installed"
  const val SECTION_AVAILABLE_TAG = "model_library_section_available"
  const val LIST_TAG = "model_library_list"
}
