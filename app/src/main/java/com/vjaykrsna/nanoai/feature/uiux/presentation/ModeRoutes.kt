package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.ui.navigation.Screen

internal fun ModeId.toRoute(): String = Screen.fromModeId(this).route

internal fun String.toModeIdOrDefault(): ModeId = Screen.fromRoute(this)?.modeId ?: ModeId.HOME

internal fun String.toModeIdOrNull(): ModeId? = Screen.fromRoute(this)?.modeId
