package com.vjaykrsna.nanoai.core.domain.uiux.navigation

import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId

internal fun ModeId.toRoute(): String = Screen.fromModeId(this).route

internal fun String.toModeIdOrDefault(): ModeId = Screen.fromRoute(this)?.modeId ?: ModeId.HOME

fun String.toModeIdOrNull(): ModeId? = Screen.fromRoute(this)?.modeId
