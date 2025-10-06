package com.vjaykrsna.nanoai.feature.uiux.state

internal fun ModeId.toRoute(): String = name.lowercase()

internal fun String.toModeIdOrDefault(): ModeId =
  ModeId.entries.firstOrNull { entry -> entry.name.equals(this, ignoreCase = true) } ?: ModeId.HOME

internal fun String.toModeIdOrNull(): ModeId? =
  ModeId.entries.firstOrNull { entry -> entry.name.equals(this, ignoreCase = true) }
