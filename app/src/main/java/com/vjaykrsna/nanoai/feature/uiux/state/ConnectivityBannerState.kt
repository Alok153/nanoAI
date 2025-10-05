package com.vjaykrsna.nanoai.feature.uiux.state

import java.time.Instant

/** State container for the connectivity banner displayed at the top of the shell. */
data class ConnectivityBannerState(
  val status: ConnectivityStatus,
  val lastDismissedAt: Instant? = null,
  val queuedActionCount: Int = 0,
  val cta: CommandAction? = null,
) {
  /** True when the banner should be rendered. */
  val isVisible: Boolean
    get() = status != ConnectivityStatus.ONLINE || queuedActionCount > 0

  /** Primary headline copy depending on connectivity. */
  val headline: String
    get() =
      when (status) {
        ConnectivityStatus.ONLINE -> if (queuedActionCount > 0) "Syncing" else "Back online"
        ConnectivityStatus.OFFLINE -> "Working offline"
        ConnectivityStatus.LIMITED -> "Connectivity limited"
      }

  /** Supporting message describing queued actions. */
  val supportingText: String
    get() =
      when (status) {
        ConnectivityStatus.ONLINE ->
          if (queuedActionCount > 0) "Resuming ${queuedActionCount} queued action(s)"
          else "All systems are back online"
        ConnectivityStatus.OFFLINE ->
          if (queuedActionCount > 0) "${queuedActionCount} action(s) will run when online"
          else "Changes are saved locally"
        ConnectivityStatus.LIMITED ->
          "Some requests may be delayed"
      }

  /** Recommended CTA label derived either from provided [cta] or default heuristics. */
  val ctaLabel: String?
    get() = cta?.title ?: defaultCtaLabel()

  private fun defaultCtaLabel(): String? =
    when (status) {
      ConnectivityStatus.OFFLINE ->
        if (queuedActionCount > 0) "View queue ($queuedActionCount)" else "View queue"
      ConnectivityStatus.LIMITED -> "Retry now"
      ConnectivityStatus.ONLINE -> null
    }
}
