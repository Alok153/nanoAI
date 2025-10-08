package com.vjaykrsna.nanoai.feature.uiux.state

sealed class NanoError {
  data class Network(val banner: ConnectivityBannerState) : NanoError()

  data class Inline(
    val title: String,
    val description: String? = null,
    val actionLabel: String? = null,
  ) : NanoError()

  data class Snackbar(val message: String) : NanoError()
}
